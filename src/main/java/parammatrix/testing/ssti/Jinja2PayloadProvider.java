package parammatrix.testing.ssti;

import java.util.List;

public final class Jinja2PayloadProvider implements SstiPayloadProvider {
    private final ArithmeticPayloadFactory factory = new ArithmeticPayloadFactory();
    @Override public SstiEngine engine() { return SstiEngine.JINJA2; }
    @Override public List<SstiPayload> payloads() {
        return List.of(factory.create(engine(), "%3$s{{%1$d*%2$d}}%4$s", "Arithmetic Evaluation"));
    }
}

