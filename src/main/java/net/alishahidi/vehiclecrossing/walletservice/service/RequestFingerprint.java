package net.alishahidi.vehiclecrossing.walletservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.stream.Collectors;

public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String of(String operation, Object... fields) {
        String canonical = operation + "|" + Arrays.stream(fields)
                .map(field -> Objects.toString(field, ""))
                .collect(Collectors.joining("|"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
