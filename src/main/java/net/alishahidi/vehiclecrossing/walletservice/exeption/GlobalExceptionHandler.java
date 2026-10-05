package net.alishahidi.vehiclecrossing.walletservice.exeption;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GlobalExceptionHandler {

    ProblemFactory problems;

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        log.atInfo()
                .setMessage("Request rejected: {}")
                .addArgument(ex.getCode())
                .addKeyValue("event.action", "request.rejected")
                .addKeyValue("error.code", ex.getCode().name())
                .log();
        return problem(ex.getCode(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return problem(ErrorCode.VALIDATION_FAILED, "One or more fields are invalid", Map.of("errors", errors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class, HandlerMethodValidationException.class})
    ResponseEntity<ProblemDetail> handleMalformed(Exception ex) {
        return problem(ErrorCode.MALFORMED_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ArithmeticException.class)
    ResponseEntity<ProblemDetail> handleOverflow(ArithmeticException ex) {
        return problem(ErrorCode.BALANCE_LIMIT_EXCEEDED, ErrorCode.BALANCE_LIMIT_EXCEEDED.getTitle(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.atError()
                .setMessage("Unexpected error while processing request")
                .setCause(ex)
                .addKeyValue("event.action", "request.failed")
                .log();
        return problem(ErrorCode.INTERNAL_ERROR, "Unexpected error", Map.of());
    }

    private ResponseEntity<ProblemDetail> problem(ErrorCode code, String detail, Map<String, ?> properties) {
        return ResponseEntity.status(code.getStatus()).body(problems.create(code, detail, properties));
    }
}
