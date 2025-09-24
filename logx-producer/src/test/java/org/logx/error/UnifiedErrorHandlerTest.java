package org.logx.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * UnifiedErrorHandler测试类
 */
public class UnifiedErrorHandlerTest {

    private UnifiedErrorHandler errorHandler;

    @BeforeEach
    public void setUp() {
        errorHandler = new UnifiedErrorHandler();
    }

    @Test
    public void testHandleError() {
        ErrorContext context = ErrorContext.builder(ErrorCode.NETWORK_CONNECTION_FAILED, "网络连接失败")
                .type(ErrorType.NETWORK_ERROR).severity(ErrorSeverity.HIGH).build();

        // 测试错误处理 - 应该返回true表示处理成功
        boolean result = errorHandler.handleError(context);
        assertThat(result).isTrue();
    }

    @Test
    public void testHandleWarning() {
        ErrorContext context = ErrorContext.builder(ErrorCode.CONFIG_VALUE_OUT_OF_RANGE, "配置值超出范围")
                .type(ErrorType.CONFIGURATION_ERROR).severity(ErrorSeverity.MEDIUM).build();

        // 测试警告处理 - 不应该抛出异常
        assertThatCode(() -> errorHandler.handleWarning(context)).doesNotThrowAnyException();
    }

    @Test
    public void testHandleFatalError() {
        ErrorContext context = ErrorContext.builder(ErrorCode.SYSTEM_OUT_OF_MEMORY, "系统内存不足")
                .type(ErrorType.SYSTEM_ERROR).severity(ErrorSeverity.CRITICAL).build();

        // 测试致命错误处理 - 不应该抛出异常
        assertThatCode(() -> errorHandler.handleFatalError(context)).doesNotThrowAnyException();
    }
}
