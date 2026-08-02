package com.example.ocare.health.ingest;

import com.example.ocare.common.exception.BusinessException;
import com.example.ocare.common.exception.ErrorCode;
import com.example.ocare.health.entity.HealthRecord;
import com.example.ocare.health.entity.HealthUpload;
import com.example.ocare.health.entity.HealthUploadRepository;
import com.example.ocare.health.payload.HealthPayload;
import com.example.ocare.health.payload.HealthPayloadNormalizer;
import com.example.ocare.health.payload.NormalizedHealthEntry;
import com.example.ocare.health.payload.NormalizedHealthPayload;
import com.example.ocare.health.summary.HealthSummaryService;
import com.example.ocare.recordkey.entity.RecordKey;
import com.example.ocare.recordkey.service.RecordKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 단말이 보낸 건강활동 데이터를 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthIngestService {

    /**
     * 멱등성 표시 보관 기간. 재전송은 대개 짧은 시간 안에 일어나고,
     * 오래된 중복은 DB UNIQUE 제약이 대신 막는다.
     */
    private static final Duration DEDUP_TTL = Duration.ofHours(48);

    private final HealthPayloadNormalizer normalizer;
    private final RecordKeyService recordKeyService;
    private final HealthUploadRepository uploadRepository;
    private final HealthRecordBatchWriter batchWriter;
    private final IdempotencyStore idempotencyStore;
    private final HealthSummaryService summaryService;

    @Transactional
    public HealthIngestResponse ingest(Long memberId, HealthPayload rawPayload) {
        NormalizedHealthPayload payload = normalizer.normalize(rawPayload);

        RecordKey recordKey = recordKeyService.getOwnedRecordKey(memberId, payload.recordKey());
        if (!recordKey.isActive()) {
            throw new BusinessException(ErrorCode.RECORD_KEY_INACTIVE);
        }

        List<String> entryKeys = payload.entries().stream()
                .map(entry -> HealthDedupKeys.entryKey(
                        payload.recordKey(), payload.source(), payload.dataType(), entry))
                .toList();
        String payloadHash = HealthDedupKeys.payloadHash(payload, entryKeys);

        // 같은 payload 가 통째로 다시 온 경우. 구간을 하나씩 검사할 필요 없이 기존 결과를 돌려준다.
        var existing = uploadRepository.findByPayloadHash(payloadHash);
        if (existing.isPresent()) {
            HealthUpload upload = existing.get();
            log.info("이미 수집된 payload 재전송: uploadId={}, recordKey={}", upload.getId(), payload.recordKey());
            return new HealthIngestResponse(upload.getId(), payload.entryCount(), 0,
                    payload.entryCount(), 0, List.of());
        }

        Set<String> newKeys = idempotencyStore.markIfAbsent(new LinkedHashSet<>(entryKeys), DEDUP_TTL);

        try {
            return save(payload, entryKeys, newKeys, payloadHash);
        } catch (RuntimeException e) {
            idempotencyStore.release(newKeys);
            throw e;
        }
    }

    private HealthIngestResponse save(NormalizedHealthPayload payload, List<String> entryKeys,
                                      Set<String> newKeys, String payloadHash) {
        HealthUpload upload = uploadRepository.save(HealthUpload.start(
                payload.recordKey(), payload.dataType(), payload.source(), payload.sourceMode(),
                payload.productName(), payload.productVendor(), payload.memo(),
                payload.lastUpdateAt(), payload.entryCount(), payloadHash));

        List<HealthRecord> records = new ArrayList<>(newKeys.size());
        Set<LocalDate> affectedDates = new TreeSet<>();

        for (int i = 0; i < payload.entries().size(); i++) {
            String dedupKey = entryKeys.get(i);
            if (!newKeys.contains(dedupKey)) {
                continue;
            }
            NormalizedHealthEntry entry = payload.entries().get(i);
            records.add(HealthRecord.create(
                    upload.getId(), payload.recordKey(), payload.dataType(), payload.source(),
                    entry.startAtUtc(), entry.endAtUtc(), entry.sourceOffset(), entry.measuredDate(),
                    entry.steps(), entry.caloriesKcal(), entry.distanceKm(), dedupKey));
            affectedDates.add(entry.measuredDate());
        }

        int saved = batchWriter.insertAll(upload.getId(), records);
        int duplicated = payload.entryCount() - saved;

        upload.complete(saved, duplicated, 0);

        summaryService.refresh(payload.recordKey(), affectedDates);

        log.info("수집 완료: uploadId={}, recordKey={}, received={}, saved={}, duplicated={}",
                upload.getId(), payload.recordKey(), payload.entryCount(), saved, duplicated);

        return new HealthIngestResponse(upload.getId(), payload.entryCount(), saved, duplicated, 0,
                List.copyOf(affectedDates));
    }
}
