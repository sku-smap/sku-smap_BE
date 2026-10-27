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

/** 명시적으로 활성화할 때만 실제 S3 사진 앞 2바이트를 조회한다. URL과 키는 출력하지 않는다. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SMAP_S3_TEST", matches = "true")
class PhotoS3IntegrationTests {
    @Autowired PhotoUrlService photos;

    @Test
    void configuredCredentialsCanReadPrivatePhoto() throws Exception {
        var request = HttpRequest.newBuilder(URI.create(photos.createUrl("골프장_3층_미소식당쪽.jpg")))
                .timeout(Duration.ofSeconds(20)).header("Range", "bytes=0-1").GET().build();
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                assertEquals(206, response.statusCode(), "비공개 S3 사진 부분 조회 실패");
                assertArrayEquals(new byte[] {(byte) 0xff, (byte) 0xd8}, body.readNBytes(2), "JPEG 파일 확인 실패");
            }
        } catch (java.io.IOException exception) {
            fail("S3 연결 오류: 네트워크 설정을 확인해 주세요.");
        }
    }
}
