package com.smap.domain.graph.entity;

import java.io.Serializable;
import lombok.*;

/** 출발 노드·도착 노드·이동 유형으로 구성된 간선의 복합 기본키이다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EdgeId implements Serializable {
    /** 이동을 시작하는 노드 ID. */
    private String fromNodeId;
    /** 이동을 마치는 노드 ID이며 역방향 연결은 별도 간선으로 관리한다. */
    private String toNodeId;
    /** 계단·엘리베이터 등 이동 유형으로 같은 노드 쌍의 복수 연결을 구분한다. */
    private String moveType;
}
