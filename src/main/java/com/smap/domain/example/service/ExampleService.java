package com.smap.domain.example.service;

import com.smap.domain.example.dto.request.ExampleRequestDTO;
import com.smap.domain.example.dto.response.ExampleResponseDTO;
import com.smap.domain.example.entity.Example;
import com.smap.domain.example.exception.ExampleErrorCode;
import com.smap.domain.example.repository.ExampleRepository;
import com.smap.global.common.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExampleService {

    private final ExampleRepository exampleRepository;

    @Transactional
    public ExampleResponseDTO create(ExampleRequestDTO request) {
        Example example = Example.builder()
                .name(request.getName())
                .build();
        return ExampleResponseDTO.from(exampleRepository.save(example));
    }

    @Transactional(readOnly = true)
    public ExampleResponseDTO get(Long id) {
        Example example = exampleRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ExampleErrorCode.EXAMPLE_NOT_FOUND));
        return ExampleResponseDTO.from(example);
    }
}
