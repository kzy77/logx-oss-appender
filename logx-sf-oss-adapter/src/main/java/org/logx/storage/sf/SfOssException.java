package org.logx.storage.sf;

import org.logx.storage.ProtocolType;
/**
 * SF OSS异常
 * <p>
 * 用于表示SF OSS服务相关的异常。
 */
public class SfOssException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    /**
     * 构造SF OSS异常
     *
     * @param message 异常消息
     */
    public SfOssException(String message) {
        super(message);
        this.statusCode = 0;
        this.errorCode = null;
    }

    /**
     * 构造SF OSS异常
     *
     * @param message 异常消息
     * @param cause 异常原因
     */
    public SfOssException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.errorCode = null;
    }

    /**
     * 构造SF OSS异常
     *
     * @param message 异常消息
     * @param statusCode HTTP状态码
     * @param errorCode 错误码
     */
    public SfOssException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    /**
     * 构造SF OSS异常
     *
     * @param message 异常消息
     * @param cause 异常原因
     * @param statusCode HTTP状态码
     * @param errorCode 错误码
     */
    public SfOssException(String message, Throwable cause, int statusCode, String errorCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    /**
     * 获取HTTP状态码
     *
     * @return HTTP状态码
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
}