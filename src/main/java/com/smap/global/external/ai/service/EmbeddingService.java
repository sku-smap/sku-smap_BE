package com.smap.global.external.ai.service;

import com.smap.global.common.exception.GeneralException;
import com.smap.global.external.ai.client.EmbeddingClient;
import com.smap.global.external.ai.dto.request.EmbeddingRequestDTO;
import com.smap.global.external.ai.dto.response.EmbeddingDataDTO;
import com.smap.global.external.ai.dto.response.EmbeddingResponseDTO;
import com.smap.global.external.ai.exception.AiErrorCode;
import com.smap.global.external.ai.properties.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final AiProperties aiProperties;

    public List<float[]> embed(List<String> texts) {
        EmbeddingRequestDTO request = EmbeddingRequestDTO.builder()
                .model(aiProperties.getEmbeddingModel())
                .input(texts)
                .dimensions(aiProperties.getEmbeddingDimensions())
                .build();

        EmbeddingResponseDTO response = embeddingClient.embeddings(request);
        if (response == null || response.getData() == null || response.getData().size() != texts.size()) {
            throw new GeneralException(AiErrorCode.INVALID_RESPONSE);
        }
        float[][] vectors = new float[texts.size()][];
        for (EmbeddingDataDTO data : response.getData()) {
            List<Float> values = data.getEmbedding();
            if (values == null || values.size() != aiProperties.getEmbeddingDimensions()
                    || data.getIndex() < 0 || data.getIndex() >= texts.size()) {
                throw new GeneralException(AiErrorCode.INVALID_RESPONSE);
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i);
            }
            vectors[data.getIndex()] = vector;
        }
        for (float[] vector : vectors) {
            if (vector == null) {
                throw new GeneralException(AiErrorCode.INVALID_RESPONSE);
            }
        }
        return List.of(vectors);
    }

    public float[] embed(String text) {
        return embed(List.of(text)).get(0);
    }
}
