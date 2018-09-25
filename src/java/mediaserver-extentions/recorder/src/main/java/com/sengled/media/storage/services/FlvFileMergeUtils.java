package com.sengled.media.storage.services;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONArray;
import com.google.common.io.Files;
import com.sengled.media.clock.SystemClock;

public class FlvFileMergeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlvFileMergeUtils.class);
    
    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;
    
    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;
    
    /**
     * The file copy buffer size (30 MB)
     */
    private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;
    
    /**
     * 合并指定目录中文件到目标目录
     * 
     * @param srcPath
     * @param dstPath
     * @param mergeFileMaxNum
     * @param mergeFileMinNum
     * @param fileSuffix
     * @return
     * @throws Exception
     */
    public static List<FlvFileGroup> merge(String srcPath, String dstPath, int mergeFileMaxNum,
            int mergeFileMinNum) throws Exception {
        List<FlvFileGroup> outList = new ArrayList<FlvFileGroup>();
        File srcdirectory = new File(srcPath);
        FileFilter filter = DirectoryFileFilter.DIRECTORY;
        File[] srcDirs = srcdirectory.listFiles(filter);

        for (File dir : srcDirs) {// 逐个目录合并
            Collection<File> files = FileUtils.listFiles(dir, new String[] {FileNameMetadata.FILE_SUFFIX}, false);
            int fileNum = files.size();
            if ( fileNum == 0  || fileNum < mergeFileMinNum) {
                continue;
            }
            
            if (fileNum < mergeFileMaxNum) {
                mergeToList(files, dstPath, outList);
                LOGGER.info("mergeToList dstPath:{},files:{},outList:{}", dstPath, files.size(),outList.size());
            } else {
                int count = 0;
                List<File> flvFilesSubgroup = new ArrayList<>();
                for (File file : files) {
                    flvFilesSubgroup.add(file);
                    count++;
                    if (count == mergeFileMaxNum) {
                        count = 0;
                        try {
                            mergeToList(flvFilesSubgroup, dstPath, outList);
                            LOGGER.info("mergeToList dstPath:{},flvFilesSubgroup:{},outList:{}",
                                    dstPath, flvFilesSubgroup.size(), outList.size());
                        } catch (Exception e) {
                            LOGGER.error("mergeToList exception.");
                            LOGGER.error(e.getMessage(), e);
                        } finally {
                            flvFilesSubgroup.clear();
                        }

                    }
                }
            }
        }
        return outList;
    }

    private static void mergeToList(Collection<File> srcFiles, String dstPath, List<FlvFileGroup> outList) {
    	String fileNamePrefix = UUID.randomUUID().toString().toUpperCase();
        File dataFile = new File(dstPath, fileNamePrefix + FlvFileGroup.DATA_FILE_SUFFIX);
        File infoFile = new File(dstPath, fileNamePrefix + FlvFileGroup.INFO_SUFFIX);
        FlvFileGroup ffg = new FlvFileGroup(dataFile, infoFile);
        boolean success = false;
        try {
        	// 创建目录
        	FileUtils.forceMkdir(dataFile.getParentFile());

        	// 合并 flv 文件
        	final long startAt = SystemClock.currentTimeMillis();
	        List<FlvFileInfo> metadatas = mergeFlvFiles(srcFiles, dataFile);
	        if (LOGGER.isDebugEnabled()) {
	        	LOGGER.debug("merge {} file cost {}ms", srcFiles.size(), SystemClock.currentTimeMillis() - startAt, dataFile.length());
	        }
	        
	        // 把文件的 metadata 写入到磁盘
	        String infoString = JSONArray.toJSONString(metadatas);
	        Files.write(infoString, infoFile, Charset.defaultCharset());
	        
	        // 合成后，把 flv 删除
	        for (File flvFile : srcFiles) {
	            FileUtils.deleteQuietly(flvFile);
	        }
	        
	        // 返回
	        outList.add(ffg);

	        success = true;
        } catch (Exception e) {
			LOGGER.error("Fail merge flv files for {}", e.getMessage(), e);
		} finally {
        	if (!success) {
        		FileUtils.deleteQuietly(infoFile);
        		FileUtils.deleteQuietly(dataFile);
        	}
		}
	}

	public static List<FlvFileInfo> mergeFlvFiles(Collection<File> srcFiles, File dstFile) throws IOException {
    	List<FlvFileInfo> fileNameInfo = new ArrayList<>();

        dstFile.createNewFile();
        FileChannel output = FileChannel.open(dstFile.toPath(), StandardOpenOption.APPEND);
        long offset = dstFile.length();
        try {
            
            for (File flvFile : srcFiles) {
                FileNameMetadata metadata = FileNameMetadata.parse(flvFile.getName());
                if (null == metadata || null == metadata.getUserId()) {
                    FileUtils.deleteQuietly(flvFile);
                    LOGGER.error("unsupported File " + flvFile.getAbsolutePath());
                    continue;
                }
                
                String token = metadata.getToken();
                long userId = metadata.getUserId();
                long startTime = metadata.getStartTime();
                long endTime = metadata.getEndTime();
                int liveHours = metadata.getStorageTTLHours();
                String city = metadata.getTimeZoneCity();
                long videoIndex = metadata.getVideoIndex();
                
                FileChannel input = FileChannel.open(flvFile.toPath(), StandardOpenOption.READ);
                try{
                    
                	long times = 0;
                    long size = input.size();
                    long pos = 0;
                    long count = 0;
                    while (pos < size && (times ++) < 1024) {
                        count = size - pos > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : size - pos;
                        pos += output.transferFrom(input, pos + offset, count);
                    }
                    fileNameInfo.add(new FlvFileInfo(token, userId,startTime, endTime, dstFile.getName(), liveHours, city, videoIndex, offset, size));
                    
                    LOGGER.debug("offset {}, pos {}, size {}, add {}", offset, pos, size, flvFile);
                    offset += pos;
                } finally{
                    IOUtils.closeQuietly(input);    
                }
            }

            output.close();
            return fileNameInfo;
        } finally {
        	IOUtils.closeQuietly(output);
		}
    }
}
