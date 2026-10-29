package com.smap.global.external.s3;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"storage.photos.enabled=true", "storage.photos.thumbnail-enabled=true"})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SMAP_S3_TEST", matches = "true")
class ThumbnailS3IntegrationTests {
    @Autowired ThumbnailUrlService thumbnails;

    @Test
    void listedThumbnailCanBeDownloaded() throws Exception {
        thumbnails.refresh();
        String url = thumbnails.createUrl("청운관_1층_중앙.jpg");
        assertNotNull(url, "S3 썸네일 목록 조회 실패");
        assertNull(thumbnails.createUrl("없는_사진.jpg"));
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode(), "썸네일 다운로드 실패");
        assertEquals("image/jpeg", response.headers().firstValue("Content-Type").orElse(""));
        assertArrayEquals(new byte[] {(byte) 0xff, (byte) 0xd8}, java.util.Arrays.copyOf(response.body(), 2));
    }
}
