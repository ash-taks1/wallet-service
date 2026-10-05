package net.alishahidi.vehiclecrossing.walletservice.exeption;

import net.alishahidi.vehiclecrossing.walletservice.common.TraceId;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProblemFactory {

//    RFC 9457
    public ProblemDetail create(ErrorCode code, String detail, Map<String, ?> properties) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(code.getStatus(), detail);
        body.setTitle(code.getTitle());
        body.setProperty("code", code.name());
        properties.forEach(body::setProperty);
        body.setProperty("traceId", TraceId.current());
        return body;
    }
}
