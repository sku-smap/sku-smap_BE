package com.smap.domain.search.dto.response;

import com.smap.domain.search.model.CandidateType;
import com.smap.domain.search.model.MatchedBy;
import com.smap.domain.place.entity.PlaceKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@Schema(description = "CandidateDTO: 검색 후보 응답 DTO")
public class CandidateDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "후보 ID (place:장소ID 또는 node:노드ID). 후보 선택 API에 그대로 보냄", example = "place:r-yudam-907")
    private String candidateId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "후보 종류. PLACE=장소, NODE=사진 지점(출발지 검색에서만)", example = "PLACE")
    private CandidateType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "장소 ID (type=NODE면 null)", example = "r-yudam-907")
    private String placeId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "경로 조회 API의 fromNodeId 또는 toNodeId로 사용할 노드 ID", example = "s1013_105937")
    private String nodeId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "화면에 보여 줄 이름", example = "로렐 카페 (907호)")
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "이름 아래 보조 설명 (건물·층·호수 등)", example = "유담관 9층 907호")
    private String displaySub;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "건물 이름", example = "유담관")
    private String building;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "표시용 building과 별도로 유지하는 고정 건물·야외 구역 ID", example = "YUDAM")
    private String buildingId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "층 (숫자=지상 층, B1=지하1층, L=L층)", example = "9")
    private String floorLabel;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "장소 분류 (room, professor, facility, office, building)", example = "facility")
    private PlaceKind kind;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "업무·시설로 찾았을 때 그 이유. 출발지는 '근처 시설: ', 목적지는 '업무: '로 시작 (없으면 null)", example = "근처 시설: 카페")
    private String reason;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "일치 점수 (0~1, 높을수록 정확)", example = "0.93")
    private double score;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, description = "찾은 방식. KEYWORD=글자 일치, SEMANTIC=의미 유사, BOTH=둘 다", example = "BOTH")
    private MatchedBy matchedBy;
}
