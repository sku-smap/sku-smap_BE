package com.smap.global.common.exception;

import com.smap.domain.route.exception.RouteErrorCode;
import jakarta.validation.constraints.NotBlank;
import feign.FeignException;
import feign.Request;
import feign.Response;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTests {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ErrorTestController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void unsupportedMethodReturns405AndAllowHeader() throws Exception {
        mvc.perform(delete("/test/body"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("POST")))
                .andExpect(jsonPath("$.code").value("COMMON_405_METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedRequestTypeReturns415AndSupportedTypes() throws Exception {
        mvc.perform(post("/test/body").contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string(HttpHeaders.ACCEPT, containsString("application/json")))
                .andExpect(jsonPath("$.code").value("COMMON_415_UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unsupportedResponseTypeReturns406() throws Exception {
        mvc.perform(get("/test/json").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value("COMMON_406_NOT_ACCEPTABLE"));
    }

    @ParameterizedTest
    @CsvSource({"{broken,COMMON_400_BODY_NOT_READABLE", "{},COMMON_400_VALIDATION"})
    void invalidJsonAndValidationAreSeparated(String body, String code) throws Exception {
        mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void missingBodyReturns400() throws Exception {
        mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_BODY_NOT_READABLE"));
    }

    @Test
    void missingRequiredParameterReturns400() throws Exception {
        mvc.perform(get("/test/parameter"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_MISSING_PARAMETER"));
    }

    @Test
    void invalidParameterTypeReturns400() throws Exception {
        mvc.perform(get("/test/parameter").param("count", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_TYPE_MISMATCH"));
    }

    @ParameterizedTest
    @CsvSource({
            "domain,404,ROUTE_404_NODE",
            "conflict,409,COMMON_409_DATA_CONFLICT",
            "database,503,COMMON_503_DATABASE_UNAVAILABLE",
            "upload,413,COMMON_413_FILE_TOO_LARGE",
            "unexpected,500,COMMON_500",
            "annotated,403,COMMON_403_FORBIDDEN"
    })
    void applicationFailuresReturnSpecificStatusesWithoutInternalDetails(String kind, int status, String code)
            throws Exception {
        mvc.perform(get("/test/failure/{kind}", kind))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(content().string(not(containsString("internal-secret"))));
    }

    @Test
    void explicitStatusAndRetryHeaderArePreserved() throws Exception {
        mvc.perform(get("/test/rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(jsonPath("$.code").value("COMMON_429_TOO_MANY_REQUESTS"));
    }

    @ParameterizedTest
    @CsvSource({"401,502", "500,502", "429,503", "503,503", "504,504"})
    void upstreamErrorsAreNotReportedAsClientAuthenticationFailures(int upstream, int expected) throws Exception {
        mvc.perform(get("/test/upstream/{status}", upstream))
                .andExpect(status().is(expected))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(content().string(not(containsString("internal-secret"))));
    }

    @RestController
    static class ErrorTestController {
        @PostMapping(value = "/test/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        ValidationBody body(@Valid @RequestBody ValidationBody request) {
            return request;
        }

        @GetMapping(value = "/test/json", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, String> json() {
            return Map.of("name", "test");
        }

        @GetMapping("/test/parameter")
        int parameter(@RequestParam("count") int count) {
            return count;
        }

        @GetMapping("/test/failure/{kind}")
        void failure(@PathVariable("kind") String kind) {
            throw switch (kind) {
                case "domain" -> new GeneralException(RouteErrorCode.NODE_NOT_FOUND);
                case "conflict" -> new DataIntegrityViolationException("internal-secret");
                case "database" -> new DataAccessResourceFailureException("internal-secret");
                case "upload" -> new MaxUploadSizeExceededException(1);
                case "annotated" -> new ForbiddenTestException();
                default -> new IllegalStateException("internal-secret");
            };
        }

        @GetMapping("/test/rate-limit")
        void rateLimit() {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS) {
                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set(HttpHeaders.RETRY_AFTER, "30");
                    return headers;
                }
            };
        }

        @GetMapping("/test/upstream/{status}")
        void upstream(@PathVariable("status") int status) {
            Request request = Request.create(Request.HttpMethod.POST, "https://example.invalid",
                    Map.of(), new byte[0], StandardCharsets.UTF_8, null);
            throw FeignException.errorStatus("test", Response.builder().status(status)
                    .request(request).body("internal-secret", StandardCharsets.UTF_8).build());
        }
    }

    /** 공통 입력 검증을 예제 도메인과 독립적으로 검증하는 테스트 전용 본문이다. */
    record ValidationBody(@NotBlank String name) {}

    @ResponseStatus(HttpStatus.FORBIDDEN)
    static class ForbiddenTestException extends RuntimeException {
    }
}
