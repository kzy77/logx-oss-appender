package org.logx.config.properties;

public class LogxOssProperties {

    private Storage storage = new Storage();
    private Batch batch = new Batch();
    private Retry retry = new Retry();
    private Queue queue = new Queue();

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public Queue getQueue() {
        return queue;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public static class Storage {
        private String endpoint;
        private String region;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucket;
        private String keyPrefix = "logx/";
        private String ossType = "SF_S3";
        private boolean pathStyleAccess;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getOssType() {
            return ossType;
        }

        public void setOssType(String ossType) {
            this.ossType = ossType;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }
    }

    public static class Batch {
        private int count = 8192;
        private int bytes = 10 * 1024 * 1024;
        private long maxAgeMs = 60000L;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getBytes() {
            return bytes;
        }

        public void setBytes(int bytes) {
            this.bytes = bytes;
        }

        public long getMaxAgeMs() {
            return maxAgeMs;
        }

        public void setMaxAgeMs(long maxAgeMs) {
            this.maxAgeMs = maxAgeMs;
        }
    }

    public static class Retry {
        private int maxRetries = 3;
        private long baseBackoffMs = 200L;
        private long maxBackoffMs = 10000L;

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getBaseBackoffMs() {
            return baseBackoffMs;
        }

        public void setBaseBackoffMs(long baseBackoffMs) {
            this.baseBackoffMs = baseBackoffMs;
        }

        public long getMaxBackoffMs() {
            return maxBackoffMs;
        }

        public void setMaxBackoffMs(long maxBackoffMs) {
            this.maxBackoffMs = maxBackoffMs;
        }
    }

    public static class Queue {
        private int capacity = 524288;
        private boolean dropWhenFull = false;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public boolean isDropWhenFull() {
            return dropWhenFull;
        }

        public void setDropWhenFull(boolean dropWhenFull) {
            this.dropWhenFull = dropWhenFull;
        }
    }
}
