package parammatrix.testing.ssti;

import java.util.List;

public final class Jinja2PayloadProvider implements SstiPayloadProvider {
    private final ArithmeticPayloadFactory factory = new ArithmeticPayloadFactory();
    @Override public SstiEngine engine() { return SstiEngine.JINJA2; }
    @Override public List<SstiPayload> payloads() {
        return SstiPayloadPatternCatalog.patternsFor(engine()).stream()
                .map(pattern -> factory.create(engine(), pattern, "Arithmetic Evaluation"))
                .toList();
    }
}

