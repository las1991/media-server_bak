package com.sengled.media.storage.repository;

import java.util.List;
import com.sengled.media.storage.repository.domain.M3StorageItem;

public interface M3StorageItemRepository {
    void save(M3StorageItem m3StorageItem) throws Exception;
    void batchSave(List<M3StorageItem> m3StorageList) throws Exception;
}
