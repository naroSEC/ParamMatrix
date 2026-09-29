package parammatrix.discovery;

import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterSource;

public record DiscoveredParameter(
        String name,
        ParameterSource source,
        String detail,
        String endpoint,
        DiscoveryConfidence confidence) {
}

