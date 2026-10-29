package com.smap.domain.graph.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

/** 확정 연결 정보와 출발 사진에서 도착 지점을 가리키는 화살표 각도를 저장한다. */
@Getter
@Entity
@Table(name = "edges")
@IdClass(EdgeId.class)
@Check(name = "edges_move_type_check", constraints = "move_type IN ('', 'WALK', 'STAIRS', 'ELEVATOR', 'DOOR')")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Edge {
    /** 출발 노드 ID. */
    @Id
    @Column(name = "from_node_id", columnDefinition = "text")
    private String fromNodeId;

    /** 원본 출발 건물명. */
    @Column(name = "from_building", columnDefinition = "text")
    private String fromBuilding;

    /** 원본 출발 층 표기. */
    @Column(name = "from_floor", columnDefinition = "text")
    private String fromFloor;

    /** 원본 출발 사진 파일명. */
    @Column(name = "from_photo", columnDefinition = "text")
    private String fromPhoto;

    /** 도착 노드 ID. */
    @Id
    @Column(name = "to_node_id", columnDefinition = "text")
    private String toNodeId;

    /** 원본 도착 건물명. */
    @Column(name = "to_building", columnDefinition = "text")
    private String toBuilding;

    /** 원본 도착 층 표기. */
    @Column(name = "to_floor", columnDefinition = "text")
    private String toFloor;

    /** 원본 도착 사진 파일명. */
    @Column(name = "to_photo", columnDefinition = "text")
    private String toPhoto;

    /** 원본 이동 유형. 빈 값도 임의로 WALK 등으로 바꾸지 않는다. */
    @Id
    @Column(name = "move_type", columnDefinition = "text")
    private String moveType;

    /** 사용자가 작성한 원본 이동 안내 문구. */
    @Column(columnDefinition = "text")
    private String description;

    /** CSV에서 제공한 수평 각도. 매칭 데이터가 없으면 NULL이며 유효 범위는 0 이상 360 미만이다. */
    @Column(name = "arrow_yaw_deg")
    private Double arrowYawDeg;
}
