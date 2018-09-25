package com.sengled.media.storage.services;

import com.alibaba.fastjson.JSONArray;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Tag;
import com.sengled.media.storage.RecordCounter;
import com.sengled.media.storage.s3.AmazonS3Template;
import com.sengled.media.storage.s3.StorageProperties;
import com.sengled.media.storage.services.exception.AwsTransferException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class AsyncStorageHandler implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncStorageHandler.class);
    public static String FILE_SEPARATOR = System.getProperty("file.separator");
    
    public static final String MERGE_UPLOAD = "merge_upload";

    public static final String FLVFILE_DIR = "flvfile";


    public static final String ERROR_DIR = "error";

    private volatile int nonUploadedFilesCount;

    @Autowired
    private AmazonS3Template s3Template;

    @Autowired
    RecordCounter recordCounter;

    @Autowired
    StorageProperties storageProperties;
    
    @Autowired
    SyncDynamodb syncDynamodb;
    
    @Value("${aws_video_bucket}")
    private String awsVideoBucket;

    @Value("${puts3.thread.num}")
    private int puts3ThreadNum;

    @Value("${mergeFile.maxNum}")
    private int mergeFileMaxNum;

    @Value("${mergeFile.minNum}")
    private int mergeFileMinNum;

    @Value("${mergeFile.mem.maxFlvFileNum:300}")
    private int memFlvFileMaxNum;

    @Value("${mergeFile.root.directory}")
    private String mergeFileRootDirectory;

    @Value("${mergeFile.safe.directory}")
    private String mergeFileSafeDirectory;


    private ExecutorService putPool;
    private File errorDir;
    private Set<FlvFileGroup> uploadErrorFiles = new HashSet<>();
    private Object uploadErrorFilesLock = new Object();
    private Object mergeLock = new Object();
    private Timer time = new Timer();
    
    private String flvFileDir;
    private String mergeUploadDir;
    private String mergeUploadSafeDir;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            init();
        } catch (Exception e) {
            LOGGER.error("init error.{}", e);
            System.exit(1);
        }
    }

    private void init() throws Exception {
        LOGGER.info("init...");
        FileUtils.forceMkdir(new File(mergeFileRootDirectory));

        flvFileDir = mergeFileRootDirectory + FILE_SEPARATOR + FLVFILE_DIR;
        FileUtils.deleteQuietly(new File(flvFileDir));
        FileUtils.forceMkdir(new File(flvFileDir));

        mergeUploadDir = mergeFileRootDirectory + FILE_SEPARATOR + MERGE_UPLOAD;
        FileUtils.forceMkdir(new File(mergeUploadDir));

        mergeUploadSafeDir = mergeFileSafeDirectory + FILE_SEPARATOR + MERGE_UPLOAD;
        FileUtils.forceMkdir(new File(mergeUploadSafeDir));

        errorDir = new File(mergeFileSafeDirectory + FILE_SEPARATOR + ERROR_DIR);
        FileUtils.deleteQuietly(errorDir);
        FileUtils.forceMkdir(errorDir);

        putPool = Executors.newFixedThreadPool(puts3ThreadNum);

        allUpload(mergeUploadDir);
        allUpload(mergeUploadSafeDir);

        LOGGER.info("FLV_FILE_TMP:{}", flvFileDir);
        LOGGER.info("FLV_UPLOAD_DIR:{}", mergeUploadDir);
        LOGGER.info("FLV_UPLOAD_SAFE_DIR:{}", mergeUploadSafeDir);

        notUploadFileCountSchedule(time, 10000, 10000);
        mergeAndUploadSchedule(time, 30000, 5000, flvFileDir, mergeUploadDir, mergeUploadSafeDir);
    }

    private void mergeAndUploadSchedule(Timer time, final long delay, final long period,
            String flvFileDir, String mergeUploadDir, String mergeSafeDir) {
        // merge and upload
        time.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    LOGGER.info("start merge...");
                    mergeAndUpload(flvFileDir, mergeUploadDir,mergeSafeDir);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }, delay, period);
    }

    private void notUploadFileCountSchedule(Timer time, final long delay, final long period) {

        time.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    String mergeUploadDir =   mergeFileRootDirectory + FILE_SEPARATOR + MERGE_UPLOAD;
                    Collection<File> files =   FileUtils.listFiles(new File(mergeUploadDir), null, false);

                    String mergeUploadSafeDir =   mergeFileSafeDirectory + FILE_SEPARATOR + MERGE_UPLOAD;
                    Collection<File> safeFiles =   FileUtils.listFiles(new File(mergeUploadSafeDir), null, false);

                    nonUploadedFilesCount = files.size() + safeFiles.size();
                    if (!files.isEmpty()) {
                        LOGGER.info("dir {}  nonUploadedFilesCount:{}", mergeUploadDir, files.size());
                    }
                    if (!safeFiles.isEmpty()) {
                        LOGGER.info("dir {}  nonUploadedFilesCount:{}", mergeUploadSafeDir, safeFiles.size());
                    }
                    recordCounter.setAndGetNotUploadFileNum(nonUploadedFilesCount);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }, delay, period);
    }

    private void mergeAndUpload(String flvFileDir, String mergeUploadDir, String mergeSafeDir) {
        LOGGER.debug("flvFileDir:{}, mergeUploadDir:{}, mergeUploadSafeDir:{}, mergeFileMaxNum:{}", flvFileDir,mergeUploadDir, mergeSafeDir,mergeFileMaxNum);
     
        List<FlvFileGroup> files =  mergeSafe(flvFileDir, mergeUploadDir, mergeSafeDir);
        addUnUploadFile(files);
        uploadyAsync(files);
    }
    private void uploadyAsync(List<FlvFileGroup> files) {
        for ( FlvFileGroup flvFileGroup : files ) {
            if (null == flvFileGroup.getInfoFile() || null == flvFileGroup.getDataFile()) {
                LOGGER.warn("infoFile or dataFile is null.");
                continue;
            }
            uploadAsync(flvFileGroup.getInfoFile().getAbsolutePath(),flvFileGroup.getDataFile().getAbsolutePath());
        }
        
    }

    private void addUnUploadFile(List<FlvFileGroup> files) {
        if ( ! CollectionUtils.isEmpty(uploadErrorFiles) ) {
            synchronized (uploadErrorFilesLock) {
                files.addAll(uploadErrorFiles);
                uploadErrorFiles.clear();
            }
        }
    }

    private List<FlvFileGroup> mergeSafe(String flvFileDir, String mergeUploadDir, String mergeUploadSafeDir) {
        List<FlvFileGroup> files = null;
        synchronized(mergeLock){
            try {
                Collection<File> datFiles = FileUtils.listFiles(new File(mergeUploadDir), new String[] {"dat"}, false);
                if (datFiles.size() * mergeFileMaxNum < memFlvFileMaxNum){
                    files = FlvFileMergeUtils.merge(flvFileDir, mergeUploadDir,mergeFileMaxNum, mergeFileMinNum);
                } else {
                	files = FlvFileMergeUtils.merge(flvFileDir, mergeUploadSafeDir,mergeFileMaxNum, mergeFileMinNum);
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(),e);
            }
        }
        return files;
    }

    private List<FlvFileGroup> merge(String flvFileDir, String mergeUploadDir, String string) {
        List<FlvFileGroup> files = null;
        synchronized(mergeLock){
            try {
                files = FlvFileMergeUtils.merge(flvFileDir, mergeUploadDir,mergeFileMaxNum, mergeFileMinNum);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(),e);
            }    
        }
        return files;
    }

    private void allUpload(String mergeUploadDir) {
        Collection<File> infoFiles =
                FileUtils.listFiles(new File(mergeUploadDir), new String[] {"inf"}, false);
        Collection<File> dataFiles =
                FileUtils.listFiles(new File(mergeUploadDir), new String[] {"dat"}, false);
        if (dataFiles.size() != infoFiles.size()) {
            LOGGER.error("dir {} infoFiles and dataFiles unpaired!", mergeUploadDir);
        }
        recordCounter.setAndGetNotUploadFileNum(dataFiles.size() + infoFiles.size());
        List<Future<Void>> futureFuture = new ArrayList<>();
        for (File infoFile : infoFiles) {
            String name = infoFile.getName();
            File dataFile = new File(mergeUploadDir + FILE_SEPARATOR
                    + name.substring(0, name.lastIndexOf(".")) + FlvFileGroup.DATA_FILE_SUFFIX);
            Future<Void> future =
                    uploadAsync(infoFile.getAbsolutePath(), dataFile.getAbsolutePath());
            futureFuture.add(future);
        }
        for (Future<Void> future : futureFuture) {
            try {
                future.get();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private Future<Void> uploadAsync(final String infoFile, final String dataFile) {
        return putPool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                putS3AndDatabase(infoFile, dataFile);
                return null;
            }
        });
    }

    private void putS3AndDatabase(final String infoFilePath, final String dataFilePath) throws Exception {

        File dataFile = new File(dataFilePath);
        File infoFile = new File(infoFilePath);
        LOGGER.debug("dataFile:{} , infoFile:{} ", dataFile.getAbsoluteFile(),infoFile.getAbsoluteFile());
        if( ! dataFile.exists()  ||  ! infoFile.exists() ){
            FileUtils.deleteQuietly(dataFile);
            FileUtils.deleteQuietly(infoFile);
            throw new Exception(" dataFile or infoFile not exists. ");
        }
        
        //载入信息文件
        List<StorageMessage> smgs = loadInfoFile(infoFile);
        if( CollectionUtils.isEmpty(smgs)){
            LOGGER.error("loadInfoFile {} is empty!" , infoFile.getAbsolutePath());
            return;
        }
        
        //上传
        boolean isDeleteFile = false;
        try {
            //Thread.currentThread().sleep(500);
            put(dataFile, smgs);
            recordCounter.markDynamodbSuccess(smgs.size());
            isDeleteFile = true;
        }catch(AwsTransferException e){//上传失败，记录未上传的info 和data文件
        //}catch (RuntimeException e){
            LOGGER.warn(e.getMessage(), e);
            synchronized (uploadErrorFilesLock) {
                LOGGER.info("add not upload file to uploadErrorFiles");
                uploadErrorFiles.add(new FlvFileGroup(dataFile, infoFile));
            }
            isDeleteFile = false;//AWS原因上传失败：不删除
        }catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            recordCounter.markDynamodbFail(smgs.size());
            isDeleteFile = true;
        }
        if( isDeleteFile ){
            FileUtils.deleteQuietly(infoFile);
            FileUtils.deleteQuietly(dataFile);
        }
    }

    private List<StorageMessage> loadInfoFile(File infoFile) {
        List<StorageMessage> smgs = null;
        try {
            String infoString = FileUtils.readFileToString(infoFile);
            smgs = JSONArray.parseArray(infoString, StorageMessage.class);
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            try {
                FileUtils.moveFileToDirectory(infoFile, errorDir, false);
            } catch (IOException e) {
                
            }
        }
        if( smgs == null ){
            return Collections.emptyList();
        }
        return smgs;
    }

    private void put(File dataFile, List<StorageMessage> smgs) throws Exception {

        // s3
        long startTime = System.currentTimeMillis();
        int daysNum = smgs.get(0).getStorageTimeDays();
        PutObjectRequest putObjectRequest =   new PutObjectRequest(awsVideoBucket, dataFile.getName(), dataFile);
        Tag tag = storageProperties.getTag(daysNum + "");
        if (null != tag) {
            putObjectRequest.setTagging(new ObjectTagging(Arrays.asList(tag)));
        }
        PutObjectResult re;
        try {
            re = s3Template.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new AwsTransferException(e.getMessage(),e);
        }
        LOGGER.debug("put s3 ExpirationTime:{} ,cost:{} ms", re.getExpirationTime(), (System.currentTimeMillis() - startTime));
                
        //dynamodb
        syncDynamodb.putDynamodb(smgs);
        LOGGER.debug("put Dynamodb and s3 cost:{} ms",(System.currentTimeMillis() - startTime));
    }

    public int getNonUploadedFilesCount() {
        return nonUploadedFilesCount;
    }
    
    public void mergeSync(){
        merge(flvFileDir, mergeUploadDir, "flv");
        merge(flvFileDir, mergeUploadSafeDir, "flv");
    }
}
