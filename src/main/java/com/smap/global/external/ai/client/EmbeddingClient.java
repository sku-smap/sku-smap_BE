package com.smap.global.external.ai.client;

import com.smap.global.external.ai.config.AiConfig;
import com.smap.global.external.ai.dto.request.EmbeddingRequestDTO;
import com.smap.global.external.ai.dto.response.EmbeddingResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "openai",
        contextId = "openaiEmbedding",
        url = "${ai.openai.base-url}",
        configuration = AiConfig.class
)
public interface EmbeddingClient {

    @PostMapping("/v1/embeddings")
    EmbeddingResponseDTO embeddings(@RequestBody EmbeddingRequestDTO request);
}
