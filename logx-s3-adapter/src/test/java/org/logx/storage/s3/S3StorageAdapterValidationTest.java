package org.logx.storage.s3;

import org.junit.jupiter.api.Test;
import org.logx.storage.StorageConfig;

import static org.junit.jupiter.api.Assertions.*;

public class S3StorageAdapterValidationTest {

    private StorageConfig baseConfig() {
        StorageConfig config = new StorageConfig();
        config.setRegion("us-east-1");
        config.setAccessKeyId("test-access-key");
        config.setAccessKeySecret("test-secret");
        config.setBucket("test-bucket");
        config.setKeyPrefix("logs");
        return config;
    }

    @Test
    void missingAccessKeyIdShouldThrow() {
        StorageConfig config = baseConfig();
        config.setAccessKeyId(null);
        assertThrows(IllegalArgumentException.class, () -> new S3StorageAdapter(config));
    }

    @Test
    void missingAccessKeySecretShouldThrow() {
        StorageConfig config = baseConfig();
        config.setAccessKeySecret("   ");
        assertThrows(IllegalArgumentException.class, () -> new S3StorageAdapter(config));
    }

    @Test
    void missingBucketShouldThrow() {
        StorageConfig config = baseConfig();
        config.setBucket("");
        assertThrows(IllegalArgumentException.class, () -> new S3StorageAdapter(config));
    }

    @Test
    void missingRegionShouldThrow() {
        StorageConfig config = baseConfig();
        config.setRegion(null);
        assertThrows(IllegalArgumentException.class, () -> new S3StorageAdapter(config));
    }

    @Test
    void invalidRegionFallsBackToDefault() {
        StorageConfig config = baseConfig();
        config.setRegion("invalid-region");
        assertDoesNotThrow(() -> {
            try (S3StorageAdapter adapter = new S3StorageAdapter(config)) {
                assertEquals("test-bucket", adapter.getBucketName());
            }
        });
    }
}
