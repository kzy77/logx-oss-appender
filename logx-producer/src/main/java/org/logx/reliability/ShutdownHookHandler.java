package org.logx.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JVM关闭钩子处理器
 * <p>
 * 确保应用关闭时能够优雅处理剩余的队列数据，避免数据丢失。 提供30秒超时保护，避免关闭过程无限等待。
 * <p>
 * 主要特性：
 * <ul>
 * <li>自动注册JVM shutdown hook</li>
 * <li>协调多个组件的优雅关闭</li>
 * <li>30秒超时保护机制</li>
 * <li>关闭状态监控和报告</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class ShutdownHookHandler {

    private static final long DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final long shutdownTimeoutSeconds;
    private final List<ShutdownCallback> callbacks;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final AtomicBoolean registered = new AtomicBoolean(false);

    private Thread shutdownHook;

    /**
     * 关闭回调接口
     */
    public interface ShutdownCallback {
        /**
         * 执行关闭操作
         *
         * @param timeoutSeconds
         *            剩余超时时间（秒）
         *
         * @return 是否成功关闭
         */
        boolean shutdown(long timeoutSeconds);

        /**
         * 获取组件名称（用于日志）
         */
        String getComponentName();
    }

    /**
     * 构造关闭钩子处理器
     *
     * @param shutdownTimeoutSeconds
     *            关闭超时时间（秒）
     */
    public ShutdownHookHandler(long shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
        this.callbacks = new ArrayList<>();
    }

    /**
     * 使用默认超时时间构造
     */
    public ShutdownHookHandler() {
        this(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
    }

    /**
     * 注册关闭回调
     *
     * @param callback
     *            关闭回调
     */
    public synchronized void registerCallback(ShutdownCallback callback) {
        if (shutdownInProgress.get()) {
            throw new IllegalStateException("Cannot register callback during shutdown");
        }
        callbacks.add(callback);
    }

    /**
     * 注册JVM关闭钩子
     */
    public synchronized void registerShutdownHook() {
        if (registered.get()) {
            return; // 已经注册过了
        }

        shutdownHook = new Thread(this::executeShutdown, "oss-appender-shutdown-hook");

        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            registered.set(true);
            System.out.println("OSS Appender shutdown hook registered successfully");
        } catch (Exception e) {
            System.err.println("Failed to register shutdown hook: " + e.getMessage());
        }
    }

    /**
     * 手动执行关闭（用于测试或主动关闭）
     */
    public void executeShutdown() {
        if (!shutdownInProgress.compareAndSet(false, true)) {
            return; // 已经在关闭中
        }

        System.out.println("OSS Appender shutdown process started, timeout: " + shutdownTimeoutSeconds + "s");
        long startTime = System.currentTimeMillis();

        try {
            // 按注册顺序逐个关闭组件
            for (int i = 0; i < callbacks.size(); i++) {
                ShutdownCallback callback = callbacks.get(i);
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long remaining = Math.max(0, shutdownTimeoutSeconds - elapsed);

                if (remaining <= 0) {
                    System.err.println("Shutdown timeout reached, forcing exit");
                    break;
                }

                System.out.println("Shutting down component: " + callback.getComponentName() + ", remaining timeout: "
                        + remaining + "s");

                try {
                    boolean success = callback.shutdown(remaining);
                    if (success) {
                        System.out.println("Component " + callback.getComponentName() + " shutdown successfully");
                    } else {
                        System.err.println("Component " + callback.getComponentName() + " shutdown failed");
                    }
                } catch (Exception e) {
                    System.err.println(
                            "Error shutting down component " + callback.getComponentName() + ": " + e.getMessage());
                }
            }

            long totalTime = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("OSS Appender shutdown process completed in " + totalTime + "s");

        } catch (RuntimeException e) {
            System.err.println("Runtime error during shutdown process: " + e.getMessage());
        } catch (Error e) {
            System.err.println("Error during shutdown process: " + e.getMessage());
        }
    }

    /**
     * 移除JVM关闭钩子（用于测试）
     */
    public synchronized void unregisterShutdownHook() {
        if (!registered.get() || shutdownHook == null) {
            return;
        }

        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            registered.set(false);
            System.out.println("OSS Appender shutdown hook unregistered");
        } catch (IllegalStateException e) {
            // 可能已经在执行中，这是正常情况
            System.out.println("Shutdown hook is already running, cannot unregister");
        } catch (Exception e) {
            // 记录其他异常但不抛出，避免影响业务
            System.err.println("Failed to unregister shutdown hook: " + e.getMessage());
        }
    }

    /**
     * 检查是否正在关闭
     */
    public boolean isShutdownInProgress() {
        return shutdownInProgress.get();
    }

    /**
     * 检查是否已注册关闭钩子
     */
    public boolean isRegistered() {
        return registered.get();
    }

    /**
     * 获取注册的回调数量
     */
    public int getCallbackCount() {
        return callbacks.size();
    }

    /**
     * 辅助方法：等待指定时间或直到条件满足
     */
    public static boolean waitForCondition(BooleanSupplier condition, long timeoutSeconds) {
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000;

        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return true;
            }

            try {
                Thread.sleep(100); // 100ms检查间隔
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * 函数式接口，用于条件等待
     */
    @FunctionalInterface
    public interface BooleanSupplier {
        boolean getAsBoolean();
    }

    /**
     * 创建基于CountDownLatch的关闭回调
     */
    public static ShutdownCallback createLatchCallback(String componentName, CountDownLatch latch,
            long maxWaitSeconds) {
        return new ShutdownCallback() {
            @Override
            public boolean shutdown(long timeoutSeconds) {
                try {
                    long waitTime = Math.min(timeoutSeconds, maxWaitSeconds);
                    return latch.await(waitTime, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            @Override
            public String getComponentName() {
                return componentName;
            }
        };
    }

    /**
     * 创建基于AutoCloseable的关闭回调
     */
    public static ShutdownCallback createCloseableCallback(String componentName, AutoCloseable closeable) {
        return new ShutdownCallback() {
            @Override
            public boolean shutdown(long timeoutSeconds) {
                try {
                    closeable.close();
                    return true;
                } catch (Exception e) {
                    System.err.println("Failed to close " + componentName + ": " + e.getMessage());
                    return false;
                }
            }

            @Override
            public String getComponentName() {
                return componentName;
            }
        };
    }
}
