package net.alishahidi.vehiclecrossing.walletservice.exeption;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        this(code, code.getTitle());
    }

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }
}
