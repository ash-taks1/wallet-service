package net.alishahidi.vehiclecrossing.walletservice.exeption;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key header"),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required for transfers"),
    SAME_WALLET_TRANSFER(HttpStatus.BAD_REQUEST, "Source and destination wallets must differ"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "A valid access token is required"),
    WALLET_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Wallet does not belong to the authenticated user"),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "Wallet not found"),
    RECIPIENT_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "Recipient wallet not found"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email is already registered"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "A request with this Idempotency-Key is still in progress"),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient funds"),
    CURRENCY_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "Wallet currencies differ"),
    BALANCE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT, "Balance limit exceeded"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_CONTENT,
            "Idempotency-Key was already used with a different request"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    HttpStatus status;
    String title;
}
