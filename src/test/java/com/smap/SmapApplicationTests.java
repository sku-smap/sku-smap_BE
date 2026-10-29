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
    void routeRequestsAreValidatedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION"));
    }

    @Test
    void swaggerDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/users/me']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/example']").doesNotExist());
    }

    @Test
    void frontendCorsWorksWithoutSecurityConfiguration() throws Exception {
        for (String origin : new String[]{"http://localhost:3000", "http://localhost:5173", "https://sku-smap.vercel.app"}) {
            mockMvc.perform(options("/api/v1/routes")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
        mockMvc.perform(get("/api/v1/nodes").header("Origin", "https://sku-smap.vercel.app"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://sku-smap.vercel.app"));
        mockMvc.perform(options("/api/v1/routes")
                        .header("Origin", "https://sku-smap.vercel.app.attacker.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
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
