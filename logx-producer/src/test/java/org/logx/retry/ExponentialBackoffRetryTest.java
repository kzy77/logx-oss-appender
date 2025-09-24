package org.logx.retry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.logx.exception.StorageException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * ExponentialBackoffRetry重试策略测试
 * <p>
 * 验证指数退避重试策略的逻辑正确性，包括重试判断、延迟计算等。
 */
class ExponentialBackoffRetryTest {

    @Test
    void testDefaultStrategy() {
        // Given
        ExponentialBackoffRetry strategy = ExponentialBackoffRetry.defaultStrategy();

        // Then
        assertThat(strategy.getMaxRetries()).isEqualTo(3);
        assertThat(strategy.getInitialDelay()).isEqualTo(Duration.ofMillis(200));
        assertThat(strategy.getMultiplier()).isEqualTo(2.0);
        assertThat(strategy.getMaxDelay()).isEqualTo(Duration.ofSeconds(30));
        assertThat(strategy.getJitterFactor()).isEqualTo(0.1);
        assertThat(strategy.getStrategyName()).isEqualTo("ExponentialBackoffRetry");
    }

    @Test
    void testCustomStrategy() {
        // Given
        ExponentialBackoffRetry strategy = new ExponentialBackoffRetry(5, Duration.ofSeconds(1), 3.0,
                Duration.ofMinutes(1), 0.2);

        // Then
        assertThat(strategy.getMaxRetries()).isEqualTo(5);
        assertThat(strategy.getInitialDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(strategy.getMultiplier()).isEqualTo(3.0);
        assertThat(strategy.getMaxDelay()).isEqualTo(Duration.ofMinutes(1));
        assertThat(strategy.getJitterFactor()).isEqualTo(0.2);
    }

    @Test
    void testInvalidParameters() {
        // 负数重试次数
        assertThatThrownBy(
                () -> new ExponentialBackoffRetry(-1, Duration.ofSeconds(1), 2.0, Duration.ofMinutes(1), 0.1))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("maxRetries must be non-negative");

        // 倍数小于等于1
        assertThatThrownBy(() -> new ExponentialBackoffRetry(3, Duration.ofSeconds(1), 1.0, Duration.ofMinutes(1), 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multiplier must be greater than 1.0");

        // 抖动因子超出范围
        assertThatThrownBy(() -> new ExponentialBackoffRetry(3, Duration.ofSeconds(1), 2.0, Duration.ofMinutes(1), 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jitterFactor must be between 0.0 and 1.0");
    }

    @ParameterizedTest
    @EnumSource(value = StorageException.ErrorType.class, names = { "NETWORK_ERROR", "SERVER_ERROR", "UNKNOWN_ERROR" })
    void testShouldRetryForRetryableErrors(StorageException.ErrorType errorType) {
        // Given
        ExponentialBackoffRetry strategy = ExponentialBackoffRetry.defaultStrategy();
        StorageException exception = new StorageException("Test error", errorType);

        // When & Then
        assertThat(strategy.shouldRetry(exception, 1)).isTrue();
        assertThat(strategy.shouldRetry(exception, 2)).isTrue();
        assertThat(strategy.shouldRetry(exception, 3)).isTrue();
        assertThat(strategy.shouldRetry(exception, 4)).isFalse(); // 超过最大重试次数
    }

    @ParameterizedTest
    @EnumSource(value = StorageException.ErrorType.class, names = { "AUTHENTICATION_ERROR", "CLIENT_ERROR",
            "CONFIGURATION_ERROR" })
    void testShouldNotRetryForNonRetryableErrors(StorageException.ErrorType errorType) {
        // Given
        ExponentialBackoffRetry strategy = ExponentialBackoffRetry.defaultStrategy();
        StorageException exception = new StorageException("Test error", errorType);

        // When & Then
        assertThat(strategy.shouldRetry(exception, 1)).isFalse();
        assertThat(strategy.shouldRetry(exception, 2)).isFalse();
    }

    @Test
    void testShouldNotRetryWhenExceptionIsNotRetryable() {
        // Given
        ExponentialBackoffRetry strategy = ExponentialBackoffRetry.defaultStrategy();
        StorageException exception = new StorageException("Non-retryable error",
                StorageException.ErrorType.NETWORK_ERROR, "ERROR_001", null, false // 明确设置为不可重试
        );

        // When & Then
        assertThat(strategy.shouldRetry(exception, 1)).isFalse();
    }

    @Test
    void testCalculateDelayProgression() {
        // Given
        ExponentialBackoffRetry strategy = new ExponentialBackoffRetry(5, Duration.ofMillis(100), 2.0,
                Duration.ofSeconds(10), 0.0 // 无抖动，便于测试
        );

        // When & Then
        assertThat(strategy.calculateDelay(1)).isEqualTo(Duration.ZERO); // 第一次不延迟
        assertThat(strategy.calculateDelay(2)).isEqualTo(Duration.ofMillis(100)); // 100ms
        assertThat(strategy.calculateDelay(3)).isEqualTo(Duration.ofMillis(200)); // 200ms
        assertThat(strategy.calculateDelay(4)).isEqualTo(Duration.ofMillis(400)); // 400ms
        assertThat(strategy.calculateDelay(5)).isEqualTo(Duration.ofMillis(800)); // 800ms
    }

    @Test
    void testCalculateDelayWithMaxLimit() {
        // Given
        ExponentialBackoffRetry strategy = new ExponentialBackoffRetry(10, Duration.ofSeconds(1), 2.0,
                Duration.ofSeconds(5), // 最大延迟5秒
                0.0 // 无抖动
        );

        // When & Then
        assertThat(strategy.calculateDelay(1)).isEqualTo(Duration.ZERO);
        assertThat(strategy.calculateDelay(2)).isEqualTo(Duration.ofSeconds(1));
        assertThat(strategy.calculateDelay(3)).isEqualTo(Duration.ofSeconds(2));
        assertThat(strategy.calculateDelay(4)).isEqualTo(Duration.ofSeconds(4));
        assertThat(strategy.calculateDelay(5)).isEqualTo(Duration.ofSeconds(5)); // 达到最大值
        assertThat(strategy.calculateDelay(10)).isEqualTo(Duration.ofSeconds(5)); // 保持最大值
    }

    @Test
    void testCalculateDelayWithJitter() {
        // Given
        ExponentialBackoffRetry strategy = new ExponentialBackoffRetry(5, Duration.ofMillis(100), 2.0,
                Duration.ofSeconds(10), 0.5 // 50%抖动
        );

        // When
        Duration delay2 = strategy.calculateDelay(2);
        Duration delay3 = strategy.calculateDelay(3);

        // Then
        // 基础延迟是100ms，加上最多50%的抖动，应该在100-150ms之间
        assertThat(delay2.toMillis()).isBetween(100L, 150L);

        // 基础延迟是200ms，加上最多50%的抖动，应该在200-300ms之间
        assertThat(delay3.toMillis()).isBetween(200L, 300L);
    }

    @Test
    void testPresetStrategies() {
        // Fast retry
        ExponentialBackoffRetry fastRetry = ExponentialBackoffRetry.fastRetry();
        assertThat(fastRetry.getMaxRetries()).isEqualTo(3);
        assertThat(fastRetry.getInitialDelay()).isEqualTo(Duration.ofMillis(100));
        assertThat(fastRetry.getMultiplier()).isEqualTo(1.5);

        // Conservative retry
        ExponentialBackoffRetry conservativeRetry = ExponentialBackoffRetry.conservativeRetry();
        assertThat(conservativeRetry.getMaxRetries()).isEqualTo(5);
        assertThat(conservativeRetry.getInitialDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(conservativeRetry.getMultiplier()).isEqualTo(2.5);
    }

    @Test
    void testToString() {
        // Given
        ExponentialBackoffRetry strategy = ExponentialBackoffRetry.defaultStrategy();

        // When
        String toString = strategy.toString();

        // Then
        assertThat(toString).contains("ExponentialBackoffRetry");
        assertThat(toString).contains("maxRetries=3");
        assertThat(toString).contains("initialDelay=PT0.2S");
        assertThat(toString).contains("multiplier=2.0");
    }
}
