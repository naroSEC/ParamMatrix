package parammatrix.http;

import burp.api.montoya.http.message.requests.HttpRequest;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtensionRequestRegistry {
    private static final long TTL_SECONDS = 120;
    private final Map<String, Instant> fingerprints = new ConcurrentHashMap<>();

    public void register(HttpRequest request) {
        purge();
        fingerprints.put(fingerprint(request), Instant.now());
    }

    public boolean isGenerated(HttpRequest request) {
        purge();
        return fingerprints.containsKey(fingerprint(request));
    }

    private String fingerprint(HttpRequest request) {
        return request.httpService() + "|" + request.toString().hashCode() + "|" + request.toString().length();
    }

    private void purge() {
        Instant cutoff = Instant.now().minusSeconds(TTL_SECONDS);
        fingerprints.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}

