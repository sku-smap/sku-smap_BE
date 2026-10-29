package com.smap.global.external.s3;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailUrlService {
    private final S3Client photoS3Client;
    private final PhotoUrlService photoUrlService;
    private final PhotoStorageProperties properties;
    private volatile Set<String> filenames = Set.of();

    public String createUrl(String filename) {
        if (filename == null || !filenames.contains(filename)) return null;
        return photoUrlService.createThumbnailUrl(filename);
    }

    @Scheduled(fixedDelayString = "${storage.photos.thumbnail-refresh-ms:600000}", initialDelay = 0)
    public void refresh() {
        if (!properties.isEnabled() || !properties.isThumbnailEnabled()) {
            filenames = Set.of();
            return;
        }
        String folder = properties.thumbnailFolderKey();
        try {
            var found = new HashSet<String>();
            photoS3Client.listObjectsV2Paginator(request -> request.bucket(properties.getBucket()).prefix(folder))
                    .contents().forEach(object -> {
                        String name = object.key().substring(folder.length());
                        if (!name.isEmpty() && !name.contains("/")) found.add(name);
                    });
            if (found.size() != filenames.size()) {
                log.info("썸네일 목록 갱신: {}개", found.size());
            }
            filenames = Set.copyOf(found);
        } catch (SdkException exception) {
            String reason = exception instanceof S3Exception s3 && s3.awsErrorDetails() != null
                    ? s3.awsErrorDetails().errorCode() : exception.getClass().getSimpleName();
            log.warn("썸네일 목록 조회 실패({}), 기존 목록 유지: {}개", reason, filenames.size());
        }
    }
}
