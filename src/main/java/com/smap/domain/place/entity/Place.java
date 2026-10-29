package com.smap.domain.place.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 검색할 장소와 실제 도착 촬영 지점의 매핑 정보를 보존한다. */
@Getter
@Entity
@Table(name = "places")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {
    @Id
    @Column(name = "place_id", columnDefinition = "text")
    /** 원본 CSV의 장소 고유 ID. */
    private String placeId;

    @Column(name = "name", columnDefinition = "text")
    /** 사용자에게 표시할 장소 이름. */
    private String name;

    @Column(name = "kind", columnDefinition = "text")
    /** 건물·호실 등 장소 분류. */
    private String kind;

    @Column(name = "building", columnDefinition = "text")
    /** 건물명 또는 야외 구분값. */
    private String building;

    /** 장소 자체의 건물 ID이며 도착 노드의 건물과 다를 수 있다. */
    @Column(name = "building_id", columnDefinition = "text")
    private String buildingId;

    @Column(name = "floor_label", columnDefinition = "text")
    /** 원본 층 표기이며 L층과 1층을 임의로 합치지 않는다. */
    private String floorLabel;

    @Column(name = "room_no", columnDefinition = "text")
    /** 호실 번호이며 건물 자체에는 없을 수 있다. */
    private String roomNo;

    @Column(name = "arrive_node_id", columnDefinition = "text")
    /** 도착 노드 ID이며 미매핑 값은 NULL로 유지하고 임의 연결하지 않는다. */
    private String arriveNodeId;

    @Column(name = "arrive_node_title", columnDefinition = "text")
    /** 도착 노드의 원본 표시 이름. */
    private String arriveNodeTitle;

    @Column(name = "door_photo", columnDefinition = "text")
    /** 원본에 기록된 출입문 사진 정보. */
    private String doorPhoto;

    @Column(name = "mapping_basis", columnDefinition = "text")
    /** 장소와 도착 노드를 연결한 근거. */
    private String mappingBasis;

    @Column(name = "aliases", columnDefinition = "text")
    /** 장소 별명 목록을 원본 문자열로 보존한다. */
    private String aliases;

    @Column(name = "sub", columnDefinition = "text")
    /** 검색 결과 등에 사용할 부가 설명. */
    private String sub;
}

