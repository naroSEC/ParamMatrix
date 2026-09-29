package parammatrix.testing.ssti;

import java.util.Set;

public record SstiRunOptions(
        Set<SstiEngine> engines,
        boolean onlyReflectedParameters,
        int maximumRequestsPerPage,
        int requestDelayMillis) {

    public SstiRunOptions {
        engines = Set.copyOf(engines);
        if (maximumRequestsPerPage < 1) throw new IllegalArgumentException("maximumRequestsPerPage");
        if (requestDelayMillis < 0) throw new IllegalArgumentException("requestDelayMillis");
    }
}

