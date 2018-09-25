package com.sengled.media.storage.repository;

import java.util.List;
import com.sengled.media.storage.repository.domain.M3Storage;

public interface M3StorageRepository {
    void save(M3Storage m3Storage) throws Exception;
    void batchSave(List<M3Storage> m3StorageList) throws Exception;

    void update(M3Storage m3Storage) throws Exception;
    void batchUpdate(List<M3Storage> m3StorageList) throws Exception;
}
