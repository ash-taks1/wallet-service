package net.alishahidi.vehiclecrossing.walletservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.alishahidi.vehiclecrossing.walletservice.dto.request.AmountRequestDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.request.TransferRequest;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.TransactionPageDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.WalletDto;
import net.alishahidi.vehiclecrossing.walletservice.exeption.ErrorCode;
import net.alishahidi.vehiclecrossing.walletservice.exeption.ProblemFactory;
import net.alishahidi.vehiclecrossing.walletservice.security.AuthenticatedUser;
import net.alishahidi.vehiclecrossing.walletservice.service.OperationResult;
import net.alishahidi.vehiclecrossing.walletservice.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Wallets", description = "Balance, deposits, withdrawals, transfers and history of your own wallets")
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WalletController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    static final String REPLAYED_HEADER = "Idempotent-Replayed";

    WalletService walletService;
    ProblemFactory problems;

    @Operation(summary = "List my wallets")
    @GetMapping
    public List<WalletDto> myWallets(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return walletService.walletsOf(user.getId());
    }

    @Operation(summary = "Get balance", description = "403 if the wallet belongs to another user.")
    @GetMapping("/{walletId}")
    public WalletDto wallet(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID walletId) {
        return walletService.get(user.getId(), walletId);
    }

    @Operation(summary = "Deposit", description = "201 with the transaction.")
    @PostMapping("/{walletId}/deposits")
    public ResponseEntity<?> deposit(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID walletId,
                                     @Parameter(description = "Client-generated unique key, e.g. a UUID") @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
                                     @Valid @RequestBody AmountRequestDto request) {
        return respond(walletService.deposit(user.getId(), walletId, request.getAmount(), idempotencyKey));
    }

    @Operation(summary = "Withdraw", description = "201, or 422 INSUFFICIENT_FUNDS.")
    @PostMapping("/{walletId}/withdrawals")
    public ResponseEntity<?> withdraw(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID walletId,
                                      @Parameter(description = "Client-generated unique key, e.g. a UUID") @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
                                      @Valid @RequestBody AmountRequestDto request) {
        return respond(walletService.withdraw(user.getId(), walletId, request.getAmount(), idempotencyKey));
    }

    @Operation(summary = "Transfer", description = "Atomic transfer to another wallet.")
    @PostMapping("/{walletId}/transfers")
    public ResponseEntity<?> transfer(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID walletId,
                                      @Parameter(description = "Client-generated unique key, e.g. a UUID") @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
                                      @Valid @RequestBody TransferRequest request) {
        return respond(walletService.transfer(user.getId(), walletId, request.getToWalletId(), request.getAmount(),
                request.getDescription(), idempotencyKey));
    }

    @Operation(summary = "Transaction history", description = "Newest first; pass nextCursor as cursor for the next page.")
    @GetMapping("/{walletId}/transactions")
    public TransactionPageDto history(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID walletId,
                                      @RequestParam(required = false) UUID cursor,
                                      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return walletService.history(user.getId(), walletId, cursor, limit);
    }

    private ResponseEntity<?> respond(OperationResult result) {
        ResponseEntity.BodyBuilder builder;
        Object body;
        if (result.succeeded()) {
            builder = ResponseEntity.status(HttpStatus.CREATED);
            body = result.getTransaction();
        } else {
            ErrorCode code = ErrorCode.valueOf(result.getTransaction().getFailureReason());
            builder = ResponseEntity.status(code.getStatus());
            body = problems.create(code, code.getTitle(), Map.of("transaction", result.getTransaction()));
        }
        if (result.isReplayed()) {
            builder.header(REPLAYED_HEADER, "true");
        }
        return builder.body(body);
    }
}
