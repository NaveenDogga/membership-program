package com.firstclub.membership.subscription;

import com.firstclub.membership.common.exception.ConflictException;
import com.firstclub.membership.domain.model.IdempotencyRecord;
import com.firstclub.membership.domain.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration RETENTION = Duration.ofDays(7);

    private final IdempotencyRecordRepository repository;

    @Transactional(readOnly = true)
    public Optional<Long> findExistingResource(String key, String operation, String requestHash) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        return repository.findByIdempotencyKey(key)
                .map(record -> {
                    if (!record.getOperation().equals(operation)
                            || !record.getRequestHash().equals(requestHash)) {
                        throw new ConflictException(
                                "Idempotency-Key has already been used for a different request");
                    }
                    return record.getResourceId();
                });
    }

    @Transactional
    public void record(String key, String operation, String requestHash, Long resourceId) {
        if (key == null || key.isBlank()) {
            return;
        }

        repository.save(IdempotencyRecord.builder()
                .idempotencyKey(key)
                .operation(operation)
                .requestHash(requestHash)
                .resourceId(resourceId)
                .expiresAt(Instant.now().plus(RETENTION))
                .build());
    }

    public String fingerprint(String... components) {
        StringBuilder canonical = new StringBuilder();
        for (String component : components) {
            canonical.append(component.length()).append(':').append(component);
        }
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available", impossible);
        }
        return HexFormat.of().formatHex(digest);
    }
}
