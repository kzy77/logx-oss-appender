package org.logx.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ServiceLoader;

public class StorageServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(StorageServiceFactory.class);

    public static StorageService createStorageService(StorageConfig config) {
        String ossType = config.getOssType();

        ProtocolType protocol;
        try {
            StorageOssType ossTypeEnum = StorageOssType.valueOf(ossType);
            protocol = ossTypeEnum.getProtocolType();
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown OSS type: {}, trying to parse as protocol type", ossType);
            try {
                protocol = ProtocolType.fromValue(ossType);
            } catch (IllegalArgumentException e2) {
                throw new IllegalStateException("Invalid OSS type or protocol: " + ossType, e2);
            }
        }

        ServiceLoader<StorageService> loader = ServiceLoader.load(StorageService.class);
        Iterator<StorageService> iterator = loader.iterator();

        while (iterator.hasNext()) {
            try {
                StorageService service = iterator.next();
                if (service.supportsProtocol(protocol)) {
                    try {
                        Method initializeMethod = service.getClass().getMethod("initialize", StorageConfig.class);
                        initializeMethod.invoke(service, config);
                    } catch (Exception e) {
                        // Ignore if initialize method is not found or fails
                    }
                    return service;
                }
            } catch (Exception e) {
                logger.error("Failed to load storage service: {}", e.getMessage());
            }
        }

        throw new IllegalStateException("No storage service found for protocol type: " + protocol +
                ". Please ensure the appropriate adapter module is in the classpath.");
    }
}
