package parammatrix.testing.ssti;

import parammatrix.http.MarkerGenerator;

import java.security.SecureRandom;

final class ArithmeticPayloadFactory {
    private final SecureRandom random = new SecureRandom();
    private final MarkerGenerator markers = new MarkerGenerator();

    SstiPayload create(SstiEngine engine, String expressionFormat, String detectionMethod) {
        int left = 100 + random.nextInt(800);
        int right = 100 + random.nextInt(800);
        String prefix = markers.generate("SSTI").replace("NARO_", "PMX_");
        String suffix = "_END";
        String expression = expressionFormat.formatted(left, right, prefix, suffix);
        String expected = prefix + ((long) left * right) + suffix;
        return new SstiPayload(engine, expression, expected, detectionMethod);
    }
}

