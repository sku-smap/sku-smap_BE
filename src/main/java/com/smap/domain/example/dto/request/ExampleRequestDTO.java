package com.smap.domain.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExampleRequestDTO {

    @NotBlank
    private String name;
}
