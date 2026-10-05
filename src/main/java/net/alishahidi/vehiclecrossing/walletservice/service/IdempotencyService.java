package net.alishahidi.vehiclecrossing.walletservice.service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.alishahidi.vehiclecrossing.walletservice.entity.IdempotencyKeyEntity;
import net.alishahidi.vehiclecrossing.walletservice.exeption.ApiException;
import net.alishahidi.vehiclecrossing.walletservice.exeption.ErrorCode;
import net.alishahidi.vehiclecrossing.walletservice.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdempotencyService {

    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9._:\\-]{1,128}");

    IdempotencyKeyRepository keys;

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<UUID> claim(UUID userId, String key, String fingerprint) {
        if (key == null) {
            return Optional.empty();
        }
        if (!VALID_KEY.matcher(key).matches()) {
            throw new ApiException(ErrorCode.INVALID_IDEMPOTENCY_KEY,
                    "Idempotency-Key must be 1-128 characters of [A-Za-z0-9._:-]");
        }
        if (keys.tryClaim(userId, key, fingerprint) == 1) {
            log.atInfo()
                    .setMessage("Idempotency key claimed")
                    .addKeyValue("event.action", "idempotency.key.claimed")
                    .addKeyValue("idempotency.key", key)
                    .log();
            return Optional.empty();
        }
        IdempotencyKeyEntity stored = keys.findById(new IdempotencyKeyEntity.Key(userId, key)).orElseThrow();
        if (!stored.getRequestHash().equals(fingerprint)) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        if (stored.getTransactionId() == null) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS);
        }
        log.atInfo()
                .setMessage("Duplicate request detected; replaying stored result")
                .addKeyValue("event.action", "idempotency.replayed")
                .addKeyValue("idempotency.key", key)
                .addKeyValue("transaction.id", stored.getTransactionId())
                .log();
        return Optional.of(stored.getTransactionId());
    }

    /** Records the transaction produced for a claimed key (same DB transaction as the claim). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(UUID userId, String key, UUID transactionId) {
        if (key != null) {
            keys.attachTransaction(userId, key, transactionId);
        }
    }
}
