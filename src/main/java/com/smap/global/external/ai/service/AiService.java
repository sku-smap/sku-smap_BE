package com.smap.global.external.ai.service;

import com.smap.global.external.ai.client.AiClient;
import com.smap.global.external.ai.dto.request.ChatMessageDTO;
import com.smap.global.external.ai.dto.request.ChatRequestDTO;
import com.smap.global.external.ai.dto.response.ChatResponseDTO;
import com.smap.global.external.ai.dto.response.ChatChoiceDTO;
import com.smap.global.external.ai.exception.AiErrorCode;
import com.smap.global.common.exception.GeneralException;
import com.smap.global.external.ai.properties.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiClient aiClient;
    private final AiProperties aiProperties;

    public String chat(String systemPrompt, String userMessage) {
        ChatRequestDTO request = ChatRequestDTO.builder()
                .model(aiProperties.getModel())
                .maxTokens(aiProperties.getMaxTokens())
                .messages(List.of(
                        ChatMessageDTO.builder().role("system").content(systemPrompt).build(),
                        ChatMessageDTO.builder().role("user").content(userMessage).build()
                ))
                .build();

        ChatResponseDTO response = aiClient.chat(request);
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0) == null) {
            throw new GeneralException(AiErrorCode.INVALID_RESPONSE);
        }
        ChatChoiceDTO choice = response.getChoices().get(0);
        ChatMessageDTO message = choice.getMessage();
        if (message == null || message.getContent() == null || message.getContent().isBlank()) {
            if ("content_filter".equals(choice.getFinishReason())) {
                throw new GeneralException(AiErrorCode.CONTENT_FILTERED);
            }
            throw new GeneralException(AiErrorCode.EMPTY_RESPONSE);
        }
        return message.getContent();
    }

    public String chat(String userMessage) {
        return chat("You are a helpful assistant.", userMessage);
    }
}
