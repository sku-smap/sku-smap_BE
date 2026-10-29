package com.smap.domain.search;

import com.smap.domain.search.config.SearchProperties;
import com.smap.domain.search.controller.EmbeddingJobController;
import com.smap.domain.search.controller.SearchController;
import com.smap.domain.search.dto.response.CandidateDTO;
import com.smap.domain.search.dto.response.EmbeddingJobResponseDTO;
import com.smap.domain.search.dto.response.SearchResponseDTO;
import com.smap.domain.search.exception.SearchErrorCode;
import com.smap.domain.search.model.CandidateType;
import com.smap.domain.search.model.EmbeddingTarget;
import com.smap.domain.search.model.InputType;
import com.smap.domain.search.model.MatchedBy;
import com.smap.domain.search.model.SearchStatus;
import com.smap.domain.search.model.SearchTarget;
import com.smap.domain.search.service.EmbeddingJobService;
import com.smap.domain.search.service.SearchService;
import com.smap.global.common.exception.GeneralException;
import com.smap.global.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({SearchController.class, EmbeddingJobController.class})
@ContextConfiguration(classes = {SearchController.class, EmbeddingJobController.class, GlobalExceptionHandler.class})
class SearchControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean SearchService searchService;
    @MockitoBean EmbeddingJobService embeddingJobService;
    @MockitoBean SearchProperties searchProperties;

    private SearchResponseDTO response() {
        CandidateDTO candidate = CandidateDTO.builder()
                .candidateId("place:cvs").type(CandidateType.PLACE).placeId("cvs").nodeId("bukak_b1")
                .name("편의점").kind(com.smap.domain.place.entity.PlaceKind.FACILITY)
                .score(0.9).matchedBy(MatchedBy.BOTH).build();
        return SearchResponseDTO.builder()
                .searchId(1L).target(SearchTarget.ORIGIN).inputType(InputType.TEXT)
                .status(SearchStatus.CANDIDATES).message("후보").candidates(List.of(candidate)).build();
    }

    @Test
    void createsSearch() throws Exception {
        when(searchService.search(any())).thenReturn(response());
        mvc.perform(post("/api/v1/searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"ORIGIN\",\"query\":\"편의점\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", "/api/v1/searches/1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.data.searchId").value(1))
                .andExpect(jsonPath("$.data.status").value("CANDIDATES"))
                .andExpect(jsonPath("$.data.candidates[0].kind").value("facility"))
                .andExpect(jsonPath("$.data.candidates[0].candidateId").value("place:cvs"));
    }

    @Test
    void rejectsMissingTarget() throws Exception {
        mvc.perform(post("/api/v1/searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"편의점\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(searchService);
    }

    @Test
    void returnsInvalidQueryError() throws Exception {
        when(searchService.search(any())).thenThrow(new GeneralException(SearchErrorCode.INVALID_QUERY));
        mvc.perform(post("/api/v1/searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"ORIGIN\",\"query\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SEARCH_400_INVALID_QUERY"));
    }

    @Test
    void selectsCandidate() throws Exception {
        mvc.perform(put("/api/v1/searches/1/selection").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":\"node:bukak_7f_elev\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(searchService).select(eq(1L), any());
    }

    @Test
    void returnsSelectionErrors() throws Exception {
        doThrow(new GeneralException(SearchErrorCode.NOT_A_CANDIDATE)).when(searchService).select(anyLong(), any());
        mvc.perform(put("/api/v1/searches/1/selection").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":\"node:x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_400_NOT_A_CANDIDATE"));
        reset(searchService);
        doThrow(new GeneralException(SearchErrorCode.SEARCH_NOT_FOUND)).when(searchService).select(anyLong(), any());
        mvc.perform(put("/api/v1/searches/9/selection").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":\"node:x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SEARCH_404_NOT_FOUND"));
    }

    @Test
    void rejectsBlankCandidateId() throws Exception {
        mvc.perform(put("/api/v1/searches/1/selection").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(searchService);
    }

    @Test
    void rejectsEmbeddingJobWithoutAdminKey() throws Exception {
        when(searchProperties.isAdminKey(any())).thenReturn(false);
        mvc.perform(post("/api/v1/admin/embedding-jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_401_UNAUTHORIZED"));
        verifyNoInteractions(embeddingJobService);
    }

    @Test
    void runsEmbeddingJobWithAdminKey() throws Exception {
        when(searchProperties.isAdminKey("secret")).thenReturn(true);
        when(embeddingJobService.run(EmbeddingTarget.ALL, false)).thenReturn(EmbeddingJobResponseDTO.builder()
                .documentCount(3).serviceUpdated(1).placeUpdated(1).nodeUpdated(1).model("m").elapsedMs(5).build());
        mvc.perform(post("/api/v1/admin/embedding-jobs").header("X-Admin-Key", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentCount").value(3));
    }
}
