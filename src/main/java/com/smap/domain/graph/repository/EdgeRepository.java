package com.smap.domain.graph.repository;

import com.smap.domain.graph.entity.Edge;
import com.smap.domain.graph.entity.EdgeId;
import org.springframework.data.jpa.repository.JpaRepository;

/** 복합 기본키로 방향별·이동 유형별 간선을 조회한다. */
public interface EdgeRepository extends JpaRepository<Edge, EdgeId> {}
