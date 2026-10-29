package com.smap.domain.graph.repository;

import com.smap.domain.graph.dto.response.BuildingResponseDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 별도 건물 테이블 없이 노드·장소에 저장된 ID와 이름을 조회한다. */
@Repository
@RequiredArgsConstructor
public class BuildingRepository {
    private final JdbcTemplate jdbcTemplate;

    /** 두 테이블 사이의 중복을 제거하며 표시명이 같아도 ID가 다르면 유지한다. */
    public List<BuildingResponseDTO> findAll() {
        return jdbcTemplate.query("""
                SELECT building_id, building FROM nodes
                WHERE building_id IS NOT NULL AND building IS NOT NULL
                UNION
                SELECT building_id, building FROM places
                WHERE building_id IS NOT NULL AND building IS NOT NULL
                ORDER BY building_id, building
                """, (rs, rowNum) -> new BuildingResponseDTO(rs.getString("building_id"), rs.getString("building")));
    }
}
