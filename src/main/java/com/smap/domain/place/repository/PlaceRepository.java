package com.smap.domain.place.repository;

import com.smap.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** 장소와 도착 노드 매핑 데이터를 조회한다. */
public interface PlaceRepository extends JpaRepository<Place, String> {
    /** 특정 도착 노드에 연결된 장소를 이름 순서로 조회한다. */
    List<Place> findByArriveNodeIdOrderByNameAsc(String arriveNodeId);

    /** 경로상의 노드에 연결된 장소를 한 번에 조회하여 반복 쿼리를 방지한다. */
    List<Place> findByArriveNodeIdInOrderByNameAscPlaceIdAsc(List<String> arriveNodeIds);
}
