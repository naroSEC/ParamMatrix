package parammatrix.model;

import java.util.Objects;

public record DiscoveryEvidence(
        ParameterSource source,
        String detail,
        String endpoint,
        DiscoveryConfidence confidence) {

    public DiscoveryEvidence {
        Objects.requireNonNull(source, "source");
        detail = detail == null ? "" : detail;
        endpoint = endpoint == null ? "" : endpoint;
        Objects.requireNonNull(confidence, "confidence");
    }
}

