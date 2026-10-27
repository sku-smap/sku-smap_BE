package com.smap.global.external.s3;

import com.smap.global.common.exception.GeneralException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 실제 AWS에 접근하지 않고 SDK 서명과 한글 키 인코딩을 검증한다. */
class PhotoUrlServiceTests {
    /** .env에서 바인딩한 명시적 키로 서명하고 불완전한 키 설정은 거부한다. */
    @Test
    void configUsesExplicitCredentials() {
        var settings = new PhotoStorageProperties();
        settings.setAccessKeyId("configured-test-access");
        settings.setSecretAccessKey("configured-test-secret");
        try (var presigner = new PhotoStorageConfig().photoPresigner(settings)) {
            String query = URI.create(new PhotoUrlService(presigner, settings).createUrl("사진.jpg")).getRawQuery();
            assertTrue(query.contains("X-Amz-Credential=configured-test-access%2F"));
        }
        settings.setSecretAccessKey("");
        assertThrows(IllegalStateException.class, () -> new PhotoStorageConfig().photoPresigner(settings));
    }

    @Test
    void signsKoreanFilenameAtBucketRoot() {
        var settings = new PhotoStorageProperties();
        try (var presigner = S3Presigner.builder().region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-access", "test-secret")))
                .build()) {
            var service = new PhotoUrlService(presigner, settings);
            var url = URI.create(service.createUrl("골프장_3층_미소식당쪽.jpg"));
            assertEquals("smap-photos-450974103987-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com", url.getHost());
            assertEquals("/골프장_3층_미소식당쪽.jpg", url.getPath());
            assertTrue(url.getRawQuery().contains("X-Amz-Signature="));
            assertTrue(url.getRawQuery().contains("X-Amz-Expires=3600"));
            settings.setPrefix("/panoramas/");
            assertEquals("/panoramas/사진 (1).jpg", URI.create(service.createUrl("사진 (1).jpg")).getPath());
        }
    }

    @Test
    void disablingPhotosDoesNotResolveCredentials() {
        var settings = new PhotoStorageProperties();
        settings.setEnabled(false);
        var presigner = mock(S3Presigner.class);
        assertNull(new PhotoUrlService(presigner, settings).createUrl("사진.jpg"));
        verifyNoInteractions(presigner);
    }

    @Test
    void missingCredentialsUsesCommonErrorWithoutLeakingDetails() {
        var presigner = mock(S3Presigner.class);
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkClientException.create("private credential detail"));
        var error = assertThrows(GeneralException.class,
                () -> new PhotoUrlService(presigner, new PhotoStorageProperties()).createUrl("사진.jpg"));
        assertEquals(PhotoStorageErrorCode.SIGNING_FAILED, error.getErrorCode());
        assertFalse(error.getMessage().contains("private credential detail"));
    }
}
