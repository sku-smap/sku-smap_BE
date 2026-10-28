package com.smap.domain.search.entity;

import com.smap.domain.route.model.RouteOption;
import com.smap.domain.search.model.InputType;
import com.smap.domain.search.model.SearchStatus;
import com.smap.domain.search.model.SearchTarget;
import com.smap.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@Entity
@Table(name = "search_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false, columnDefinition = "text")
    private SearchTarget target;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, columnDefinition = "text")
    private InputType inputType;

    @Column(name = "query_text", nullable = false, columnDefinition = "text")
    private String queryText;

    @Column(name = "normalized_query", nullable = false, columnDefinition = "text")
    private String normalizedQuery;

    @Column(name = "transcript", columnDefinition = "text")
    private String transcript;

    @Column(name = "stt_engine", columnDefinition = "text")
    private String sttEngine;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_option", columnDefinition = "text")
    private RouteOption routeOption;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "text")
    private SearchStatus status;

    @Column(name = "candidate_ids", columnDefinition = "text")
    private String candidateIds;

    @Column(name = "selected_candidate_id", columnDefinition = "text")
    private String selectedCandidateId;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Builder
    private SearchLog(SearchTarget target, InputType inputType, String queryText, String normalizedQuery,
                      String transcript, String sttEngine, RouteOption routeOption, SearchStatus status,
                      List<String> candidateIds, Integer latencyMs) {
        this.target = target;
        this.inputType = inputType;
        this.queryText = queryText;
        this.normalizedQuery = normalizedQuery;
        this.transcript = transcript;
        this.sttEngine = sttEngine;
        this.routeOption = routeOption;
        this.status = status;
        this.candidateIds = String.join(",", candidateIds);
        this.latencyMs = latencyMs;
    }

    public boolean hasCandidate(String candidateId) {
        return candidateIds != null && !candidateIds.isEmpty()
                && Arrays.asList(candidateIds.split(",")).contains(candidateId);
    }

    public void select(String candidateId) {
        this.selectedCandidateId = candidateId;
    }
}
