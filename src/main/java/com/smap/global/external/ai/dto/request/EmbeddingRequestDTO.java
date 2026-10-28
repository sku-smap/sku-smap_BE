package com.smap.global.external.ai.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmbeddingRequestDTO {

    private String model;
    private List<String> input;
    private int dimensions;
}
