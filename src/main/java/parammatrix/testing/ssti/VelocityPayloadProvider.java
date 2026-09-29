package parammatrix.testing.ssti;

import java.util.List;

public final class VelocityPayloadProvider implements SstiPayloadProvider {
    private final ArithmeticPayloadFactory factory = new ArithmeticPayloadFactory();
    @Override public SstiEngine engine() { return SstiEngine.VELOCITY; }
    @Override public List<SstiPayload> payloads() {
        return List.of(factory.create(engine(), "#set($pmx=%1$d*%2$d)%3$s${pmx}%4$s",
                "Arithmetic Evaluation"));
    }
}

