package com.smap.global.external.stt.client;

import com.smap.global.external.stt.config.SttConfig;
import com.smap.global.external.stt.dto.TranscribeResponseDTO;
import feign.Request;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(
        name = "stt",
        url = "${stt.oracle.base-url}",
        configuration = SttConfig.class
)
public interface SttClient {

    @PostMapping(value = "/v1/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    TranscribeResponseDTO transcribe(@RequestPart("audio") MultipartFile audio,
                                     @RequestPart("language") String language,
                                     @RequestPart("prompt") String prompt,
                                     Request.Options options);

    @GetMapping("/health")
    Map<String, String> health();
}
