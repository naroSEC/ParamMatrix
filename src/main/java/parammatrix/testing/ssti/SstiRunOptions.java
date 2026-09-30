package parammatrix.testing.ssti;

import parammatrix.http.PayloadEncodingMode;

import java.util.Set;

public record SstiRunOptions(
        Set<SstiEngine> engines,
        boolean onlyReflectedParameters,
        int maximumRequestsPerPage,
        int requestDelayMillis,
        PayloadEncodingMode payloadEncodingMode) {

    public SstiRunOptions {
        engines = Set.copyOf(engines);
        if (maximumRequestsPerPage < 1) throw new IllegalArgumentException("maximumRequestsPerPage");
        if (requestDelayMillis < 0) throw new IllegalArgumentException("requestDelayMillis");
        if (payloadEncodingMode == null) throw new IllegalArgumentException("payloadEncodingMode");
    }
}

