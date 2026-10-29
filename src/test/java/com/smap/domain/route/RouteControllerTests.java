package com.smap.domain.route;

import com.smap.domain.graph.entity.MoveType;
import com.smap.domain.graph.service.GraphSnapshot.Point;
import com.smap.domain.graph.service.GraphSnapshot.Segment;
import com.smap.domain.route.controller.RouteController;
import com.smap.domain.route.exception.RouteErrorCode;
import com.smap.domain.route.exception.RouteException;
import com.smap.domain.route.service.RouteResult;
import com.smap.domain.route.service.RouteService;
import com.smap.global.common.exception.GlobalExceptionHandler;
import java.util.List;
import com.smap.domain.route.model.RouteOption;
import com.smap.global.external.s3.PhotoUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP 입력 검증, 응답 구조 및 도메인 예외의 공통 응답 변환을 검증한다. */
@WebMvcTest(RouteController.class)
@ContextConfiguration(classes = {RouteController.class, GlobalExceptionHandler.class})
class RouteControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean RouteService service;
    @MockitoBean PhotoUrlService photoUrls;

    /** 네 조건을 서비스로 전달하며 잘못된 조건은 입력 오류로 처리한다. */
    @Test
    void forwardsEveryOption() throws Exception {
        for (RouteOption option : RouteOption.values()) {
            when(service.find("a", "a", option)).thenReturn(new RouteResult(
                    List.of(new Point("a", "야외", null, "출발", "a.jpg")), List.of()));
            mvc.perform(post("/api/v1/routes").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fromNodeId\":\"a\",\"toNodeId\":\"a\",\"option\":\"" + option.name() + "\"}"))
                    .andExpect(status().isOk());
            verify(service).find("a", "a", option);
        }
        mvc.perform(post("/api/v1/routes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromNodeId\":\"a\",\"toNodeId\":\"b\",\"option\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    /** 사진 순서와 안내 문구를 확인하고 시간 필드가 반환되지 않는지 검증한다. */
    @Test
    void returnsOrderedRoute() throws Exception {
        when(photoUrls.createUrl("a.jpg")).thenReturn("https://example.com/a.jpg?signature=test");
        when(service.find("a", "b", RouteOption.OUTDOOR_FIRST)).thenReturn(new RouteResult(
                List.of(new Point("a", "야외", null, "출발", "a.jpg", "OUTDOOR_GROUND"),
                        new Point("b", "본관", "1", "도착", "b.jpg")),
                List.of(new Segment("a", "b", MoveType.WALK, "본관으로 이동합니다."))));
        mvc.perform(post("/api/v1/routes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromNodeId\":\"a\",\"toNodeId\":\"b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodes[0].nodeId").value("a"))
                .andExpect(jsonPath("$.data.nodes[0].photoType").value("PANORAMA"))
                .andExpect(jsonPath("$.data.nodes[0].buildingId").value("OUTDOOR_GROUND"))
                .andExpect(jsonPath("$.data.nodes[0].imageUrl").value("https://example.com/a.jpg?signature=test"))
                .andExpect(jsonPath("$.data.nodes[1].originalFilename").value("b.jpg"))
                .andExpect(jsonPath("$.data.nodes[0].description").value("본관으로 이동합니다."))
                .andExpect(jsonPath("$.data.nodes[0].places").isEmpty())
                .andExpect(jsonPath("$.data.nodes[1].description").isEmpty())
                .andExpect(jsonPath("$.data.segments[0].moveType").value("WALK"))
                .andExpect(jsonPath("$.data.segments[0].timeSeconds").doesNotExist())
                .andExpect(jsonPath("$.data.segments[0].description").value("본관으로 이동합니다."))
                .andExpect(jsonPath("$.data.totalTimeSeconds").doesNotExist())
                .andExpect(jsonPath("$.data.totalSteps").value(1));
    }

    /** DB에 장소 사진이 있어도 경로 응답에는 노출하지 않는다. */
    @Test
    void routePlacesDoNotExposeDoorPhotos() throws Exception {
        var place = mock(com.smap.domain.place.entity.Place.class);
        when(place.getPlaceId()).thenReturn("place-a");
        when(place.getBuildingId()).thenReturn("BON");
        when(place.getDoorPhoto()).thenReturn("door/example.jpg");
        when(service.find("a", "a", RouteOption.OUTDOOR_FIRST)).thenReturn(new RouteResult(
                List.of(new Point("a", "본관", "1층", "출발", "a.jpg")), List.of(),
                java.util.Map.of("a", List.of(place))));
        mvc.perform(post("/api/v1/routes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromNodeId\":\"a\",\"toNodeId\":\"a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes[0].places[0].placeId").value("place-a"))
                .andExpect(jsonPath("$.data.nodes[0].places[0].buildingId").value("BON"))
                .andExpect(jsonPath("$.data.nodes[0].places[0].doorPhoto").doesNotExist());
    }

    /** 누락·공백 ID 및 잘못된 JSON은 서비스 호출 전에 거부한다. */
    @Test
    void rejectsInvalidRequests() throws Exception {
        for (String body : List.of("{}", "{broken", "{\"fromNodeId\":\"a\"}")) {
            mvc.perform(post("/api/v1/routes").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        verifyNoInteractions(service);
    }

    /** 공통 예외 처리기가 도메인별 오류 코드와 404 상태를 보존하는지 확인한다. */
    @Test
    void returnsDomainErrors() throws Exception {
        for (RouteErrorCode code : RouteErrorCode.values()) {
            when(service.find("a", "b", RouteOption.OUTDOOR_FIRST)).thenThrow(new RouteException(code));
            mvc.perform(post("/api/v1/routes").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fromNodeId\":\"a\",\"toNodeId\":\"b\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(code.getCode()));
            reset(service);
        }
    }
}
