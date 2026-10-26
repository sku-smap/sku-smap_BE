package com.smap.global.external.ai.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageDTO {

    private String role;     // "system" | "user" | "assistant"
    private String content;
}
