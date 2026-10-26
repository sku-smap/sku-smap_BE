package com.smap.global.external.ai.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.smap.global.common.exception.GeneralException;
import com.smap.global.external.ai.client.AiClient;
import com.smap.global.external.ai.dto.request.ChatMessageDTO;
import com.smap.global.external.ai.dto.response.ChatResponseDTO;
import com.smap.global.external.ai.exception.AiErrorCode;
import com.smap.global.external.ai.properties.AiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceTests {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void responseMessageCanBeDeserializedAndRequestBuilderStillWorks() throws Exception {
        ChatResponseDTO response = mapper.readValue("""
                {"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"answer","refusal":null}}]}
                """, ChatResponseDTO.class);
        assertEquals("assistant", response.getChoices().get(0).getMessage().getRole());
        assertEquals("answer", response.getChoices().get(0).getMessage().getContent());
        ChatMessageDTO request = ChatMessageDTO.builder().role("user").content("question").build();
        assertEquals("question", mapper.readTree(mapper.writeValueAsString(request)).get("content").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"stop", "length", "content_filter", "tool_calls"})
    void nonemptyTextIsReturnedUnchanged(String finishReason) throws Exception {
        AiService service = service("""
                {"choices":[{"finish_reason":"%s","message":{"role":"assistant","content":"  answer  "}}]}
                """.formatted(finishReason));
        assertEquals("  answer  ", service.chat("system", "question"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"choices\":null}", "{\"choices\":[]}", "{\"choices\":[null]}"})
    void missingResponseOrChoicesIsAnExplicitError(String response) throws Exception {
        GeneralException error = assertThrows(GeneralException.class, () -> service(response).chat("question"));
        assertEquals(AiErrorCode.INVALID_RESPONSE, error.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"choices\":[{}]}",
            "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":null}]}",
            "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":null}}]}",
            "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"\"}}]}",
            "{\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{\"content\":\"  \"}}]}"
    })
    void missingOrBlankContentIsNotASuccess(String response) throws Exception {
        GeneralException error = assertThrows(GeneralException.class, () -> service(response).chat("question"));
        assertEquals(AiErrorCode.EMPTY_RESPONSE, error.getErrorCode());
    }

    @Test
    void filteredEmptyResponseHasADistinctError() throws Exception {
        AiService service = service("""
                {"choices":[{"finish_reason":"content_filter","message":{"role":"assistant","content":null}}]}
                """);
        GeneralException error = assertThrows(GeneralException.class, () -> service.chat("question"));
        assertEquals(AiErrorCode.CONTENT_FILTERED, error.getErrorCode());
    }

    @Test
    void requestStillUsesConfiguredModelAndPrompts() throws Exception {
        ChatResponseDTO response = mapper.readValue("""
                {"choices":[{"message":{"content":"answer"},"finish_reason":"stop"}]}
                """, ChatResponseDTO.class);
        AiProperties properties = new AiProperties();
        properties.setModel("test-model");
        properties.setMaxTokens(100);
        AiClient client = request -> {
            assertEquals("test-model", request.getModel());
            assertEquals(100, request.getMaxTokens());
            assertEquals("system", request.getMessages().get(0).getRole());
            assertEquals("system prompt", request.getMessages().get(0).getContent());
            assertEquals("question", request.getMessages().get(1).getContent());
            return response;
        };
        assertEquals("answer", new AiService(client, properties).chat("system prompt", "question"));
    }

    private AiService service(String json) throws Exception {
        ChatResponseDTO response = mapper.readValue(json, ChatResponseDTO.class);
        return new AiService(request -> response, new AiProperties());
    }
}
