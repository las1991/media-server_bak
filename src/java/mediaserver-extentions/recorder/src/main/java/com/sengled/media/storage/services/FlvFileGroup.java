package com.sengled.media.storage.services;

import java.io.File;

public class FlvFileGroup {
    public static final String DATA_FILE_SUFFIX = ".dat";
    public static final String INFO_SUFFIX = ".inf";
    public static String FILE_SEPARATOR = System.getProperty("file.separator");

    private File dataFile;
    private File infoFile;

    public FlvFileGroup(String absPath, String prefix) {
        this.dataFile = new File(absPath + FILE_SEPARATOR + prefix + DATA_FILE_SUFFIX);
        this.infoFile = new File(absPath + FILE_SEPARATOR + prefix + INFO_SUFFIX);
    }

    public FlvFileGroup(File dataFile, File infoFile) {
        super();
        this.dataFile = dataFile;
        this.infoFile = infoFile;
    }

    public File getDataFile() {
        return dataFile;
    }

    public void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }

    public File getInfoFile() {
        return infoFile;
    }

    public void setInfoFile(File infoFile) {
        this.infoFile = infoFile;
    }

    @Override
    public boolean equals(Object obj) {
        if( null == obj ){
            return false;
        }
        FlvFileGroup argObj = (FlvFileGroup) obj;
        boolean infoBool = false;
        boolean dataBool = false;

        try {
            infoBool = argObj.getInfoFile().getAbsolutePath().equals(this.getInfoFile().getAbsolutePath());
        } catch (Exception e) {

        }
        try {
            dataBool = argObj.getDataFile().getAbsolutePath().equals(this.getDataFile().getAbsolutePath());
        } catch (Exception e) {

        }
        return infoBool || dataBool;
    }

    @Override
    public int hashCode() {
        String infoFileName="",dataFileName="";
        
        try {
            infoFileName = this.getInfoFile().getAbsolutePath();
        } catch (Exception e) {

        }
        try {
            dataFileName = this.getDataFile().getAbsolutePath();
        } catch (Exception e) {

        }
        return infoFileName.hashCode() + dataFileName.hashCode();
    }
}
