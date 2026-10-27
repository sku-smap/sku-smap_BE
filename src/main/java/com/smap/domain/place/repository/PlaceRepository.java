package com.smap.domain.place.repository;

import com.smap.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 장소와 도착 노드 매핑 데이터를 조회한다. */
public interface PlaceRepository extends JpaRepository<Place, String> {
    /** 특정 도착 노드에 연결된 장소를 이름 순서로 조회한다. */
    List<Place> findByArriveNodeIdOrderByNameAsc(String arriveNodeId);

    /** 경로상의 노드에 연결된 장소를 한 번에 조회하여 반복 쿼리를 방지한다. */
    List<Place> findByArriveNodeIdInOrderByNameAscPlaceIdAsc(List<String> arriveNodeIds);

    /** 실제 노드가 연결된 장소만 검색하며 검색어의 %·_도 일반 문자로 처리한다. */
    @Query("""
            select p from Place p
            where exists (select n.nodeId from Node n where n.nodeId = p.arriveNodeId)
              and (:keyword = ''
                or locate(:keyword, lower(p.name)) > 0
                or locate(:keyword, lower(coalesce(p.aliases, ''))) > 0
                or locate(:keyword, lower(coalesce(p.building, ''))) > 0
                or locate(:keyword, lower(coalesce(p.roomNo, ''))) > 0)
            order by p.name asc, p.placeId asc
            """)
    List<Place> searchConnected(@Param("keyword") String keyword);
}
