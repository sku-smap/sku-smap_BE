package com.smap.domain.graph.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 새 연결 CSV의 출발·도착 지점을 중복 제거하여 저장한 노드이다. */
@Getter
@Entity
@Table(name = "nodes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Node {
    /** 연결 CSV에서 확정한 고유 ID이며 원본 값을 유지한다. */
    @Id
    @Column(name = "node_id", columnDefinition = "text")
    private String nodeId;

    /** 건물명 또는 야외 구분값이다. */
    @Column(name = "building", nullable = false, columnDefinition = "text")
    private String building;

    /** 표시명과 별도로 유지하는 고정 건물·야외 구역 ID이다. */
    @Column(name = "building_id", columnDefinition = "text")
    private String buildingId;

    /** 원본 층 표기이며 L층·지하층·미지정 값을 그대로 보존한다. */
    @Column(name = "floor_label", columnDefinition = "text")
    private String floorLabel;

    /** 사용자 확인으로 통일된 사진 파일명이며 S3 키를 추정하지 않는다. */
    @Column(name = "original_filename", nullable = false, columnDefinition = "text")
    private String originalFilename;
}
