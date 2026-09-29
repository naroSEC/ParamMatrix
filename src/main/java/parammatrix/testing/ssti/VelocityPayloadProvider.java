package parammatrix.testing.ssti;

import java.util.List;

public final class VelocityPayloadProvider implements SstiPayloadProvider {
    private final ArithmeticPayloadFactory factory = new ArithmeticPayloadFactory();
    @Override public SstiEngine engine() { return SstiEngine.VELOCITY; }
    @Override public List<SstiPayload> payloads() {
        return SstiPayloadPatternCatalog.patternsFor(engine()).stream()
                .map(pattern -> factory.create(engine(), pattern, "Arithmetic Evaluation"))
                .toList();
    }
}

