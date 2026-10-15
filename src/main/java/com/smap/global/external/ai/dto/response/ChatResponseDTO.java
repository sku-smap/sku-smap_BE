package com.smap.global.external.ai.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ChatResponseDTO {

    private String id;
    private String model;
    private List<ChatChoiceDTO> choices;
}
