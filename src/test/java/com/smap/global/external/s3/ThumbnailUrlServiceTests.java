package com.smap.global.external.s3;

import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ThumbnailUrlServiceTests {
    private S3Presigner presigner;
    private S3Client s3;
    private PhotoStorageProperties settings;
    private ThumbnailUrlService service;
    private ListObjectsV2Request lastRequest;

    @BeforeEach
    void setUp() {
        presigner = S3Presigner.builder().region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-access", "test-secret")))
                .build();
        s3 = mock(S3Client.class);
        settings = new PhotoStorageProperties();
        service = new ThumbnailUrlService(s3, new PhotoUrlService(presigner, settings), settings);
    }

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @SuppressWarnings("unchecked")
    private void listReturns(String... keys) {
        when(s3.listObjectsV2Paginator(any(Consumer.class))).thenAnswer(call -> {
            var builder = ListObjectsV2Request.builder();
            ((Consumer<ListObjectsV2Request.Builder>) call.getArgument(0)).accept(builder);
            lastRequest = builder.build();
            return new ListObjectsV2Iterable(s3, lastRequest);
        });
        var objects = java.util.Arrays.stream(keys).map(key -> S3Object.builder().key(key).build()).toList();
        when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(objects).isTruncated(false).build());
    }

    @Test
    void signsOnlyListedThumbnails() {
        listReturns("thumbs/", "thumbs/청운관_1층_중앙.jpg", "thumbs/old/청운관_L층_뒷길끝.jpg");
        service.refresh();
        assertEquals("thumbs/", lastRequest.prefix());
        assertEquals(settings.getBucket(), lastRequest.bucket());
        var url = URI.create(service.createUrl("청운관_1층_중앙.jpg"));
        assertEquals("/thumbs/청운관_1층_중앙.jpg", url.getPath());
        assertTrue(url.getRawQuery().contains("X-Amz-Expires=3600"));
        assertNull(service.createUrl("청운관_L층_뒷길끝.jpg"));
        assertNull(service.createUrl("없는사진.jpg"));
        assertNull(service.createUrl(null));
    }

    @Test
    void usesPhotoPrefixAndFolderSetting() {
        settings.setPrefix("/panoramas/");
        settings.setThumbnailFolder("small/");
        listReturns("panoramas/small/사진 (1).jpg");
        service.refresh();
        assertEquals("panoramas/small/", lastRequest.prefix());
        assertEquals("/panoramas/small/사진 (1).jpg", URI.create(service.createUrl("사진 (1).jpg")).getPath());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listingFailureKeepsPreviousList() {
        listReturns("thumbs/a.jpg");
        service.refresh();
        assertNotNull(service.createUrl("a.jpg"));
        doThrow(SdkClientException.create("private detail")).when(s3).listObjectsV2Paginator(any(Consumer.class));
        assertDoesNotThrow(service::refresh);
        assertNotNull(service.createUrl("a.jpg"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void firstListingFailureReturnsNull() {
        doThrow(SdkClientException.create("private detail")).when(s3).listObjectsV2Paginator(any(Consumer.class));
        assertDoesNotThrow(service::refresh);
        assertNull(service.createUrl("a.jpg"));
    }

    @Test
    void disabledSettingsSkipS3AndReturnNull() {
        listReturns("thumbs/a.jpg");
        service.refresh();
        settings.setThumbnailEnabled(false);
        service.refresh();
        assertNull(service.createUrl("a.jpg"));
        settings.setThumbnailEnabled(true);
        settings.setEnabled(false);
        service.refresh();
        assertNull(service.createUrl("a.jpg"));
        verify(s3, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }
}
