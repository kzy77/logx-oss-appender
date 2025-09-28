package org.logx.core;

import org.logx.storage.StorageService;

import java.util.concurrent.TimeUnit;

/**
 * 异步处理引擎接口
 * <p>
 * 定义了异步日志处理引擎的核心API，负责接收日志数据、 启动和停止处理流程，并提供优雅停机支持。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public interface AsyncEngine {

    /**
     * 启动异步引擎
     */
    void start();

    /**
     * 停止异步引擎
     *
     * @param timeout
     *            停机超时时间
     * @param timeUnit
     *            超时时间单位
     */
    void stop(long timeout, TimeUnit timeUnit);

    /**
     * 将日志数据放入处理队列
     *
     * @param data
     *            日志数据
     */
    void put(byte[] data);

    /**
     * 创建并返回一个AsyncEngine的实例
     *
     * @param storage
     *            存储服务的实现
     *
     * @return AsyncEngine的实例
     */
    static AsyncEngine create(StorageService storage) {
        // 返回一个模拟实现，以便在核心引擎完成前进行集成
        return new MockAsyncEngine(storage);
    }
}

/**
 * AsyncEngine的模拟实现 用于在核心引擎开发完成前，为上层适配器提供集成点
 */
class MockAsyncEngine implements AsyncEngine {

    private final StorageService s3Storage;

    public MockAsyncEngine(StorageService s3Storage) {
        this.s3Storage = s3Storage;
    }

    @Override
    public void start() {
        // 模拟启动
        System.out.println("MockAsyncEngine started with backend: " + s3Storage.getOssType());
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        // 模拟停止
        System.out.println("MockAsyncEngine stopped.");
    }

    @Override
    public void put(byte[] data) {
        // 模拟处理
        // 在实际实现中，这里会将数据放入Disruptor队列
    }
}
