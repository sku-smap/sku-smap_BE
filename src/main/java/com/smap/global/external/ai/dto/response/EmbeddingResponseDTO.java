package com.smap.global.external.ai.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class EmbeddingResponseDTO {

    private String model;
    private List<EmbeddingDataDTO> data;
}
