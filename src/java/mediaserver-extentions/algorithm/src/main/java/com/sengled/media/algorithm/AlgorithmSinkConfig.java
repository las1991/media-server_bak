package com.sengled.media.algorithm;

import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import com.sengled.media.Version;
import com.sengled.media.algorithm.config.Actions;
import com.sengled.media.algorithm.config.Actions.AlgorithmType;
import com.sengled.media.algorithm.config.AlgorithmConfig;
import com.sengled.media.device.MediaDeviceProfile;
import com.sengled.media.device.ProductType;
import com.sengled.media.plugin.config.storage.StorageConfig;
import com.sengled.utils.VersionUtils;

public class AlgorithmSinkConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmSinkConfig.class);
    private final static Pattern VERSION_PATTERN = Pattern.compile("^[1-9]{1,}\\.[0-9]\\.[0-9]{1,3}$");

    private final MediaDeviceProfile profile;
    private final AlgorithmConfig algorithmConfig;
    private final StorageConfig storageConfig;
    private final int fps;


    public AlgorithmSinkConfig(MediaDeviceProfile profile, AlgorithmConfig algorithmConfig,
                               StorageConfig storageConfig) {
        super();
        this.profile = profile;
        this.algorithmConfig = algorithmConfig;
        this.storageConfig = storageConfig;
        //配合修复bug redmine #5397,#5318

        final String version = profile.getVersion();
        if (VersionUtils.compare(version, "2.2.152") >= 0 
                && VersionUtils.compare(version, "4.0.0") < 0) {
            fps = 20;
        } else {
            fps = 25;
        }
        
        LOGGER.info("[{}], fps = {}, version = {}", profile.getToken(), fps, version);
    }

    public boolean isModified(AlgorithmSinkConfig config) {
        return !serial(false).equals(config.serial(false));
    }

    /**
     * SNAP2, SNAP3 有 PIR 元件
     *
     * @return
     */
    public boolean isPirUsed() {
        return ProductType.SNAP2.getVal() == profile.getProductType()
                || ProductType.SNAP3.getVal() == profile.getProductType();
    }

    public int getDetectionInternal() {
        return storageConfig.isEnable() && storageConfig.getFileExpires() > 24 ? 0 : 2500;
    }

    public boolean isAlgorithmEnabled() {
        Actions actions = algorithmConfig.getActions();
        List<AlgorithmType> algorithms = actions.getAlgorithms();
        return actions.isEnable() && (algorithms.contains(AlgorithmType.MOTION) || algorithms.contains(AlgorithmType.PERSION));
    }

    public int getFps() {
        return fps;
    }

    public AlgorithmConfig getAlgorithmConfig() {
        return algorithmConfig;
    }

    public MediaDeviceProfile getProfile() {
        return profile;
    }

    public StorageConfig getStorageConfig() {
        return storageConfig;
    }

    @Override
    public String toString() {
        return serial(true);
    }

    private String serial(boolean includeProfile) {
        JSONObject obj = new JSONObject(true);
        obj.put("storage", storageConfig);
        obj.put("algorithm", algorithmConfig);
        if (includeProfile) {
            obj.put("profile", profile);
        } else if (null != profile) {
            obj.put("profileVersion", profile.getVersion());
        }

        return obj.toJSONString();
    }


}
