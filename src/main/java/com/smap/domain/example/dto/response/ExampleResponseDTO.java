package com.smap.domain.example.dto.response;

import com.smap.domain.example.entity.Example;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExampleResponseDTO {

    private Long id;
    private String name;

    public static ExampleResponseDTO from(Example example) {
        return ExampleResponseDTO.builder()
                .id(example.getId())
                .name(example.getName())
                .build();
    }
}
