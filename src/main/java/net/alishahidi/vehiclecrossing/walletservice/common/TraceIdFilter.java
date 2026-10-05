package net.alishahidi.vehiclecrossing.walletservice.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(TraceId.HEADER);
        String traceId = incoming != null && VALID_ID.matcher(incoming).matches() ? incoming : TraceId.newId();
        MDC.put(TraceId.MDC_KEY, traceId);
        response.setHeader(TraceId.HEADER, traceId);
        long startedAt = System.nanoTime();
        log.atInfo()
                .setMessage("HTTP {} {} received")
                .addArgument(request.getMethod())
                .addArgument(request.getRequestURI())
                .addKeyValue("event.action", "http.request.received")
                .log();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.atInfo()
                    .setMessage("HTTP {} {} completed with {} in {} ms")
                    .addArgument(request.getMethod())
                    .addArgument(request.getRequestURI())
                    .addArgument(response.getStatus())
                    .addArgument(durationMs)
                    .addKeyValue("event.action", "http.request.completed")
                    .addKeyValue("http.status_code", response.getStatus())
                    .addKeyValue("duration_ms", durationMs)
                    .log();
            MDC.remove(TraceId.MDC_KEY);
        }
    }
}
