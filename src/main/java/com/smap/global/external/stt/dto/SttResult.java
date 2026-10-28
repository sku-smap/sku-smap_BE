package com.smap.global.external.stt.dto;

public record SttResult(String text, SttEngine engine, long elapsedMs) {
}
