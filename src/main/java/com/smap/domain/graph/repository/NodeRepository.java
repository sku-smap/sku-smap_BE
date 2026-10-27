package com.smap.domain.graph.repository;

import com.smap.domain.graph.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;

/** 문자열 노드 ID를 기준으로 촬영 지점 데이터를 조회한다. */
public interface NodeRepository extends JpaRepository<Node, String> {}
