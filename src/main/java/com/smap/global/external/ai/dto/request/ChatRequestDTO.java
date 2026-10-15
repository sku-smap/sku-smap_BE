package com.smap.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRequestDTO {

    private String model;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private List<ChatMessageDTO> messages;
}
