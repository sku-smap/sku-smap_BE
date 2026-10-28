package com.smap.global.external.stt.dto;

import lombok.Getter;

@Getter
public class TranscribeResponseDTO {

    private String text;
    private String language;
    private double durationSec;
    private long processingMs;
    private boolean noSpeech;
}
