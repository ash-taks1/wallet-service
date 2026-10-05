package net.alishahidi.vehiclecrossing.walletservice.common;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceId {

        public static final String HEADER = "X-Trace-Id";

    public static final String MDC_KEY = "traceId";

    private TraceId() {
    }

    /** Trace id of the current thread's request or event, or null if none. */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    /** New random id, 32 hex characters. */
    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
