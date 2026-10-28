package com.smap.global.external.ai.client;

import com.smap.global.external.ai.config.AiConfig;
import com.smap.global.external.ai.dto.response.TranscriptionResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "openai",
        contextId = "openaiTranscription",
        url = "${ai.openai.base-url}",
        configuration = AiConfig.class
)
public interface TranscriptionClient {

    @PostMapping(value = "/v1/audio/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    TranscriptionResponseDTO transcribe(@RequestPart("file") MultipartFile file,
                                        @RequestPart("model") String model,
                                        @RequestPart("language") String language,
                                        @RequestPart("prompt") String prompt);
}
