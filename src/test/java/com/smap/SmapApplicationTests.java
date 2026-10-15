package com.smap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smap.global.external.ai.dto.response.ChatResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SmapApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

    @Test
    void applicationObjectMapperReadsAiMessageWithoutJacksonized() throws Exception {
        ChatResponseDTO response = objectMapper.readValue("""
                {"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"answer","refusal":null}}]}
                """, ChatResponseDTO.class);
        assertEquals("answer", response.getChoices().get(0).getMessage().getContent());
    }

    @Test
    void exampleCanBeCreatedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"public example\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("public example"));
    }

    @Test
    void swaggerDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/users/me']").doesNotExist());
    }

    @Test
    void frontendCorsWorksWithoutSecurityConfiguration() throws Exception {
        mockMvc.perform(options("/api/v1/example")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void rootAndMissingResourcesReturn404InsteadOf500() throws Exception {
        for (String path : new String[]{"/", "/not-existing-resource"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMON_404_NOT_FOUND"));
        }
    }

}
