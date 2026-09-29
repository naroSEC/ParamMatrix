package parammatrix.testing.ssti;

import java.util.List;

public interface SstiPayloadProvider {
    SstiEngine engine();
    List<SstiPayload> payloads();
}

