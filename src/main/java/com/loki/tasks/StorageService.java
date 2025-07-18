package com.loki.tasks;

import com.google.cloud.storage.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

/**
 * Simple helper to store and read files in Google Cloud Storage.
 */
@Slf4j
@Service
public class StorageService {

    private final Storage storage = StorageOptions.getDefaultInstance().getService();
    private final String bucket;

    public StorageService(@Value("${gcp.storage.bucket}") String bucket) {
        this.bucket = bucket;
    }

    /** 
     * Se ejecuta después de que Spring construye el bean.
     * Si no hay credenciales válidas, escribe un log de error y continúa.
     */
    @PostConstruct
    private void ensureBucketExists() {
        try {
            Bucket b = storage.get(bucket);
            if (b == null) {
                storage.create(BucketInfo.of(bucket));
                log.info("Bucket '{}' creado automáticamente", bucket);
            } else {
                log.debug("Bucket '{}' ya existe", bucket);
            }
        } catch (StorageException e) {
            log.error("No se pudo acceder/crear el bucket '{}'. "
                    + "¿Tienes configuradas las credenciales de GCP? "
                    + "La aplicación seguirá levantando, pero las operaciones de GCS fallarán.", bucket, e);
        }
    }

    public String save(byte[] data, String prefix, String suffix) throws IOException {
        String objectName = prefix + UUID.randomUUID() + suffix;
        BlobId blobId = BlobId.of(bucket, objectName);
        BlobInfo info = BlobInfo.newBuilder(blobId).build();
        storage.create(info, data);
        return objectName;
    }

    public byte[] read(String objectName) throws IOException {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return storage.readAllBytes(bucket, objectName);
            } catch (StorageException e) {
                if (attempt == 3) {
                    throw e;
                }
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to retry", ie);
                }
            }
        }
        throw new IllegalStateException("Unreachable");
    }
}
