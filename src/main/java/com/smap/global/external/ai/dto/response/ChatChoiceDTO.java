package com.smap.global.external.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smap.global.external.ai.dto.request.ChatMessageDTO;
import lombok.Getter;

@Getter
public class ChatChoiceDTO {

    private int index;
    private ChatMessageDTO message;

    @JsonProperty("finish_reason")
    private String finishReason;
}
