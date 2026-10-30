package com.smap.domain.search;

import com.smap.domain.search.controller.RouteSearchController;
import com.smap.domain.search.dto.response.RouteSearchResponseDTO;
import com.smap.domain.search.dto.response.SearchResponseDTO;
import com.smap.domain.search.exception.SearchErrorCode;
import com.smap.domain.search.model.InputType;
import com.smap.domain.search.model.RouteSearchMode;
import com.smap.domain.search.model.SearchStatus;
import com.smap.domain.search.model.SearchTarget;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RouteSearchController.class)
@ContextConfiguration(classes = {RouteSearchController.class, GlobalExceptionHandler.class})
class RouteSearchControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean SearchService searchService;

    private SearchResponseDTO part(long id, SearchTarget target) {
        return SearchResponseDTO.builder()
                .searchId(id).target(target).inputType(InputType.TEXT)
                .status(SearchStatus.NOT_FOUND).message("후보").candidates(List.of()).build();
    }

    @Test
    void createsRouteSearch() throws Exception {
        when(searchService.searchRoute(any())).thenReturn(RouteSearchResponseDTO.of(RouteSearchMode.ROUTE, null,
                part(1L, SearchTarget.ORIGIN), part(2L, SearchTarget.DESTINATION)));
        mvc.perform(post("/api/v1/route-searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"북악관에서 청운관 어떻게 가?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.data.mode").value("ROUTE"))
                .andExpect(jsonPath("$.data.origin.searchId").value(1))
                .andExpect(jsonPath("$.data.origin.target").value("ORIGIN"))
                .andExpect(jsonPath("$.data.destination.searchId").value(2));
    }

    @Test
    void returnsNullOriginForDestinationOnly() throws Exception {
        when(searchService.searchRoute(any())).thenReturn(RouteSearchResponseDTO.of(RouteSearchMode.DESTINATION_ONLY,
                null, null, part(3L, SearchTarget.DESTINATION)));
        mvc.perform(post("/api/v1/route-searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"청운관 어떻게 가\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mode").value("DESTINATION_ONLY"))
                .andExpect(jsonPath("$.data.origin").doesNotExist())
                .andExpect(jsonPath("$.data.destination.searchId").value(3));
    }

    @Test
    void returnsInvalidQueryError() throws Exception {
        when(searchService.searchRoute(any())).thenThrow(new GeneralException(SearchErrorCode.INVALID_QUERY));
        mvc.perform(post("/api/v1/route-searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_400_INVALID_QUERY"));
    }

    @Test
    void defaultsToTopCandidateOnly() throws Exception {
        when(searchService.searchRoute(any())).thenReturn(RouteSearchResponseDTO.of(RouteSearchMode.DESTINATION_ONLY,
                null, null, part(4L, SearchTarget.DESTINATION)));
        mvc.perform(post("/api/v1/route-searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"청운관\"}"))
                .andExpect(status().isCreated());
        verify(searchService).searchRoute(argThat(request -> request.limitOrDefault() == 1));
    }

    @Test
    void rejectsLimitOutOfRange() throws Exception {
        mvc.perform(post("/api/v1/route-searches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"청운관\",\"limit\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(searchService);
    }
}
