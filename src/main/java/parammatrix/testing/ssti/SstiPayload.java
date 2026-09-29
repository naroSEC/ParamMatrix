package parammatrix.testing.ssti;

public record SstiPayload(
        SstiEngine engine,
        String payload,
        String expectedResult,
        String detectionMethod) {
}

