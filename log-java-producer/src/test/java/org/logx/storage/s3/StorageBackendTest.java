package org.logx.storage.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.logx.storage.StorageBackend;

import static org.assertj.core.api.Assertions.*;

/**
 * StorageBackend枚举测试
 * <p>
 * 验证存储后端枚举的检测逻辑、属性和方法的正确性。
 */
class StorageBackendTest {

    @Test
    void testAllBackendTypes() {
        // 验证所有后端类型的基本属性
        assertThat(StorageBackend.ALIYUN_OSS.getDisplayName()).isEqualTo("Aliyun OSS");
        assertThat(StorageBackend.ALIYUN_OSS.getUrlPrefix()).isEqualTo("oss");
        assertThat(StorageBackend.ALIYUN_OSS.isPathStyleDefault()).isTrue();

        assertThat(StorageBackend.AWS_S3.getDisplayName()).isEqualTo("AWS S3");
        assertThat(StorageBackend.AWS_S3.getUrlPrefix()).isEqualTo("s3");
        assertThat(StorageBackend.AWS_S3.isPathStyleDefault()).isFalse();

        assertThat(StorageBackend.MINIO.getDisplayName()).isEqualTo("MinIO");
        assertThat(StorageBackend.MINIO.getUrlPrefix()).isEqualTo("minio");
        assertThat(StorageBackend.MINIO.isPathStyleDefault()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({ "'https://oss-cn-hangzhou.aliyuncs.com', ALIYUN_OSS",
            "'https://bucket.oss-cn-beijing.aliyuncs.com', ALIYUN_OSS", "'https://s3.amazonaws.com', AWS_S3",
            "'https://bucket.s3.us-west-2.amazonaws.com', AWS_S3",
            "'https://cos.ap-guangzhou.myqcloud.com', TENCENT_COS",
            "'https://obs.cn-north-1.myhuaweicloud.com', HUAWEI_OBS", "'http://localhost:9000', MINIO",
            "'https://play.minio.io:9000', MINIO", "'https://custom-example.com', GENERIC_S3" })
    void testDetectFromEndpoint(String endpoint, StorageBackend expected) {
        // When
        StorageBackend detected = StorageBackend.detectFromEndpoint(endpoint);

        // Then
        assertThat(detected).isEqualTo(expected);
    }

    @Test
    void testDetectFromEndpointWithNull() {
        // When
        StorageBackend detected = StorageBackend.detectFromEndpoint(null);

        // Then
        assertThat(detected).isEqualTo(StorageBackend.GENERIC_S3);
    }

    @Test
    void testDetectFromEndpointWithEmpty() {
        // When
        StorageBackend detected = StorageBackend.detectFromEndpoint("");

        // Then
        assertThat(detected).isEqualTo(StorageBackend.GENERIC_S3);
    }

    @ParameterizedTest
    @CsvSource({ "'https://oss-cn-hangzhou.aliyuncs.com', 'cn-hangzhou', ALIYUN_OSS",
            "'https://bucket.s3.amazonaws.com', 'us-west-2', AWS_S3",
            "'https://custom-oss.example.com', 'cn-beijing', ALIYUN_OSS",
            "'https://custom-test.example.com', 'us-east-1', AWS_S3",
            "'https://unknown.example.com', 'unknown-region', GENERIC_S3" })
    void testDetectFromConfig(String endpoint, String region, StorageBackend expected) {
        // When
        StorageBackend detected = StorageBackend.detectFromConfig(endpoint, region);

        // Then
        assertThat(detected).isEqualTo(expected);
    }

    @Test
    void testCloudProviderCategories() {
        // 国内云服务商
        assertThat(StorageBackend.ALIYUN_OSS.isDomesticCloud()).isTrue();
        assertThat(StorageBackend.TENCENT_COS.isDomesticCloud()).isTrue();
        assertThat(StorageBackend.HUAWEI_OBS.isDomesticCloud()).isTrue();

        // 国际云服务商
        assertThat(StorageBackend.AWS_S3.isInternationalCloud()).isTrue();

        // 开源/私有化部署
        assertThat(StorageBackend.MINIO.isOpenSource()).isTrue();
        assertThat(StorageBackend.GENERIC_S3.isOpenSource()).isTrue();

        // 互斥性验证
        assertThat(StorageBackend.ALIYUN_OSS.isInternationalCloud()).isFalse();
        assertThat(StorageBackend.AWS_S3.isDomesticCloud()).isFalse();
        assertThat(StorageBackend.AWS_S3.isOpenSource()).isFalse();
    }

    @Test
    void testToString() {
        // When & Then
        assertThat(StorageBackend.ALIYUN_OSS.toString()).contains("Aliyun OSS");
        assertThat(StorageBackend.ALIYUN_OSS.toString()).contains("ALIYUN_OSS");
        assertThat(StorageBackend.AWS_S3.toString()).contains("AWS S3");
        assertThat(StorageBackend.AWS_S3.toString()).contains("AWS_S3");
    }
}