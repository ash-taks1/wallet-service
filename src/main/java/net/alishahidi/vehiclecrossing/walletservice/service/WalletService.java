package net.alishahidi.vehiclecrossing.walletservice.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.alishahidi.vehiclecrossing.walletservice.common.TraceId;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.TransactionDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.TransactionPageDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.WalletDto;
import net.alishahidi.vehiclecrossing.walletservice.entity.LedgerEntryEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.TransactionEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.WalletEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionStatus;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionType;
import net.alishahidi.vehiclecrossing.walletservice.exeption.ApiException;
import net.alishahidi.vehiclecrossing.walletservice.exeption.ErrorCode;
import net.alishahidi.vehiclecrossing.walletservice.mapper.TransactionMapper;
import net.alishahidi.vehiclecrossing.walletservice.mapper.WalletMapper;
import net.alishahidi.vehiclecrossing.walletservice.repository.LedgerEntryRepository;
import net.alishahidi.vehiclecrossing.walletservice.repository.TransactionRepository;
import net.alishahidi.vehiclecrossing.walletservice.repository.WalletRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WalletService {

    private static final UUID MAX_UUID = new UUID(-1L, -1L);

    WalletRepository wallets;
    TransactionRepository transactions;
    LedgerEntryRepository ledger;
    IdempotencyService idempotency;
    WalletMapper walletMapper;
    TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public List<WalletDto> walletsOf(UUID userId) {
        return walletMapper.toDtos(wallets.findByUserIdOrderByCreatedAt(userId));
    }

    @Transactional(readOnly = true)
    public WalletDto get(UUID userId, UUID walletId) {
        return walletMapper.toDto(findOwned(userId, walletId));
    }

    @Transactional
    public OperationResult deposit(UUID userId, UUID walletId, long amount, String idempotencyKey) {
        String fingerprint = RequestFingerprint.of("DEPOSIT", walletId, amount);
        return idempotent(userId, idempotencyKey, fingerprint, walletId, () -> {
            WalletEntity wallet = lockOwnedWallet(userId, walletId);
            long balanceAfter = wallet.credit(amount);
            TransactionEntity tx = transactions.save(newTransaction(TransactionType.DEPOSIT, walletId, amount,
                    idempotencyKey).status(TransactionStatus.COMPLETED).build());
            LedgerEntryEntity entry = ledger.save(LedgerEntryEntity.credit(tx, walletId, balanceAfter));
            logApplied(tx, balanceAfter);
            return result(tx, entry, walletId);
        });
    }

    @Transactional
    public OperationResult withdraw(UUID userId, UUID walletId, long amount, String idempotencyKey) {
        String fingerprint = RequestFingerprint.of("WITHDRAWAL", walletId, amount);
        return idempotent(userId, idempotencyKey, fingerprint, walletId, () -> {
            WalletEntity wallet = lockOwnedWallet(userId, walletId);
            TransactionEntity.TransactionEntityBuilder<?, ?> builder =
                    newTransaction(TransactionType.WITHDRAWAL, walletId, amount, idempotencyKey);
            if (!wallet.hasSufficientFunds(amount)) {
                return reject(builder, wallet);
            }
            long balanceAfter = wallet.debit(amount);
            TransactionEntity tx = transactions.save(builder.status(TransactionStatus.COMPLETED).build());
            LedgerEntryEntity entry = ledger.save(LedgerEntryEntity.debit(tx, walletId, balanceAfter));
            logApplied(tx, balanceAfter);
            return result(tx, entry, walletId);
        });
    }

    @Transactional
    public OperationResult transfer(UUID userId, UUID sourceWalletId, UUID targetWalletId, long amount,
                                    String description, String idempotencyKey) {
        if (sourceWalletId.equals(targetWalletId)) {
            throw new ApiException(ErrorCode.SAME_WALLET_TRANSFER);
        }
        if (idempotencyKey == null) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        String fingerprint = RequestFingerprint.of("TRANSFER", sourceWalletId, targetWalletId, amount, description);
        return idempotent(userId, idempotencyKey, fingerprint, sourceWalletId, () -> {
            Map<UUID, WalletEntity> locked = wallets.findAllByIdForUpdate(List.of(sourceWalletId, targetWalletId))
                    .stream()
                    .collect(Collectors.toMap(WalletEntity::getId, Function.identity()));
            WalletEntity source = locked.get(sourceWalletId);
            if (source == null) {
                throw new ApiException(ErrorCode.WALLET_NOT_FOUND);
            }
            requireOwner(source, userId);
            WalletEntity target = locked.get(targetWalletId);
            if (target == null) {
                throw new ApiException(ErrorCode.RECIPIENT_WALLET_NOT_FOUND);
            }
            if (!source.getCurrency().equals(target.getCurrency())) {
                throw new ApiException(ErrorCode.CURRENCY_MISMATCH);
            }
            TransactionEntity.TransactionEntityBuilder<?, ?> builder =
                    newTransaction(TransactionType.TRANSFER, sourceWalletId, amount, idempotencyKey)
                            .counterpartyWalletId(targetWalletId)
                            .description(description);
            if (!source.hasSufficientFunds(amount)) {
                return reject(builder, source);
            }
            long sourceBalanceAfter = source.debit(amount);
            long targetBalanceAfter = target.credit(amount);
            TransactionEntity tx = transactions.save(builder.status(TransactionStatus.COMPLETED).build());
            LedgerEntryEntity debit = ledger.save(LedgerEntryEntity.debit(tx, sourceWalletId, sourceBalanceAfter));
            ledger.save(LedgerEntryEntity.credit(tx, targetWalletId, targetBalanceAfter));
            logApplied(tx, sourceBalanceAfter);
            return result(tx, debit, sourceWalletId);
        });
    }

    @Transactional(readOnly = true)
    public TransactionPageDto history(UUID userId, UUID walletId, UUID cursor, int limit) {
        findOwned(userId, walletId);
        List<TransactionEntity> page = transactions.findHistory(walletId, TransactionStatus.COMPLETED,
                cursor == null ? MAX_UUID : cursor, Limit.of(limit + 1));
        boolean hasMore = page.size() > limit;
        List<TransactionEntity> items = hasMore ? page.subList(0, limit) : page;
        Map<UUID, LedgerEntryEntity> entries = items.isEmpty() ? Map.of()
                : ledger.findByWalletIdAndTransactionIdIn(walletId, items.stream().map(TransactionEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(LedgerEntryEntity::getTransactionId, Function.identity()));
        List<TransactionDto> dtos = items.stream()
                .map(tx -> transactionMapper.toDto(tx, entries.get(tx.getId()), walletId))
                .toList();
        return new TransactionPageDto(dtos, hasMore ? items.getLast().getId() : null);
    }

    private OperationResult idempotent(UUID userId, String idempotencyKey, String fingerprint, UUID walletId,
                                       Supplier<OperationResult> action) {
        Optional<UUID> previous = idempotency.claim(userId, idempotencyKey, fingerprint);
        if (previous.isPresent()) {
            TransactionEntity tx = transactions.findById(previous.get()).orElseThrow();
            LedgerEntryEntity entry = ledger.findByTransactionIdAndWalletId(tx.getId(), walletId).orElse(null);
            return new OperationResult(transactionMapper.toDto(tx, entry, walletId), true);
        }
        OperationResult result = action.get();
        idempotency.complete(userId, idempotencyKey, result.getTransaction().getTransactionId());
        return result;
    }

    private TransactionEntity.TransactionEntityBuilder<?, ?> newTransaction(TransactionType type, UUID walletId,
                                                                            long amount, String idempotencyKey) {
        return TransactionEntity.builder()
                .type(type)
                .walletId(walletId)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .traceId(TraceId.current());
    }

    private OperationResult reject(TransactionEntity.TransactionEntityBuilder<?, ?> builder, WalletEntity wallet) {
        TransactionEntity rejected = transactions.save(builder
                .status(TransactionStatus.FAILED)
                .failureReason(ErrorCode.INSUFFICIENT_FUNDS.name())
                .build());
        log.atWarn()
                .setMessage("{} {} rejected: {}")
                .addArgument(rejected.getType())
                .addArgument(rejected.getId())
                .addArgument(rejected.getFailureReason())
                .addKeyValue("event.action", "transaction.rejected")
                .addKeyValue("transaction.id", rejected.getId())
                .addKeyValue("transaction.type", rejected.getType())
                .addKeyValue("wallet.id", rejected.getWalletId())
                .addKeyValue("amount", rejected.getAmount())
                .addKeyValue("balance", wallet.getBalance())
                .addKeyValue("failure_reason", rejected.getFailureReason())
                .log();
        logAfterCommit(rejected);
        return result(rejected, null, wallet.getId());
    }

    private OperationResult result(TransactionEntity tx, LedgerEntryEntity entry, UUID walletId) {
        return new OperationResult(transactionMapper.toDto(tx, entry, walletId), false);
    }

    private WalletEntity findOwned(UUID userId, UUID walletId) {
        WalletEntity wallet = wallets.findById(walletId)
                .orElseThrow(() -> new ApiException(ErrorCode.WALLET_NOT_FOUND));
        requireOwner(wallet, userId);
        return wallet;
    }

    private WalletEntity lockOwnedWallet(UUID userId, UUID walletId) {
        WalletEntity wallet = wallets.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ApiException(ErrorCode.WALLET_NOT_FOUND));
        requireOwner(wallet, userId);
        return wallet;
    }

    private static void requireOwner(WalletEntity wallet, UUID userId) {
        if (!wallet.isOwnedBy(userId)) {
            log.atWarn()
                    .setMessage("Access to a wallet owned by another user was denied")
                    .addKeyValue("event.action", "wallet.access.denied")
                    .addKeyValue("wallet.id", wallet.getId())
                    .addKeyValue("user.id", userId)
                    .log();
            throw new ApiException(ErrorCode.WALLET_ACCESS_DENIED);
        }
    }

    private static void logApplied(TransactionEntity tx, long balanceAfter) {
        log.atInfo()
                .setMessage("{} {} applied to wallet")
                .addArgument(tx.getType())
                .addArgument(tx.getTransactionCode())
                .addKeyValue("event.action", "transaction.applied")
                .addKeyValue("transaction.id", tx.getId())
                .addKeyValue("transaction.code", tx.getTransactionCode())
                .addKeyValue("transaction.type", tx.getType())
                .addKeyValue("wallet.id", tx.getWalletId())
                .addKeyValue("amount", tx.getAmount())
                .addKeyValue("balance_after", balanceAfter)
                .log();
        logAfterCommit(tx);
    }

    /** Confirms in the log that the transaction (and its outbox event) is durably committed. */
    private static void logAfterCommit(TransactionEntity tx) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.atInfo()
                        .setMessage("{} {} committed to database with status {}")
                        .addArgument(tx.getType())
                        .addArgument(tx.getTransactionCode())
                        .addArgument(tx.getStatus())
                        .addKeyValue("event.action", "transaction.committed")
                        .addKeyValue("transaction.id", tx.getId())
                        .addKeyValue("transaction.status", tx.getStatus())
                        .log();
            }
        });
    }
}
