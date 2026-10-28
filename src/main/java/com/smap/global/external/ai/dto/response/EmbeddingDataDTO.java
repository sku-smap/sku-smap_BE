package com.smap.global.external.ai.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class EmbeddingDataDTO {

    private int index;
    private List<Float> embedding;
}
