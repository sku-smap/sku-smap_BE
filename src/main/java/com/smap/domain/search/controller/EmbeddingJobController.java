package com.smap.domain.search.controller;

import com.smap.domain.search.config.SearchProperties;
import com.smap.domain.search.dto.response.EmbeddingJobResponseDTO;
import com.smap.domain.search.exception.SearchErrorCode;
import com.smap.domain.search.model.EmbeddingTarget;
import com.smap.domain.search.service.EmbeddingJobService;
import com.smap.global.common.exception.GeneralException;
import com.smap.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/embedding-jobs")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API (FE 연동 대상 아님)")
public class EmbeddingJobController {

    private final EmbeddingJobService embeddingJobService;
    private final SearchProperties searchProperties;

    @Operation(summary = "검색 문서 갱신 API",
            description = "장소·노드·업무 데이터로 검색 문서를 다시 만들고, 내용이 바뀐 문서만 임베딩하는 API. "
                    + "배포 후 처음 한 번, 그리고 장소 데이터가 바뀌었을 때 실행합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<EmbeddingJobResponseDTO>> run(
            @Parameter(description = "서버 환경 변수 ADMIN_API_KEY 값")
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @Parameter(description = "갱신할 문서 종류", example = "ALL")
            @RequestParam(defaultValue = "ALL") EmbeddingTarget target,
            @Parameter(description = "true면 바뀌지 않은 문서도 전부 다시 임베딩", example = "false")
            @RequestParam(defaultValue = "false") boolean force) {
        if (!searchProperties.isAdminKey(adminKey)) {
            throw new GeneralException(SearchErrorCode.UNAUTHORIZED_ADMIN);
        }
        return ResponseEntity.ok(ApiResponse.success(embeddingJobService.run(target, force)));
    }
}
