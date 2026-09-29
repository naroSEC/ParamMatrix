package parammatrix.model;

import burp.api.montoya.http.message.HttpRequestResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ParameterCandidate {
    private final String name;
    private final PageIdentity pageIdentity;
    private final boolean existing;
    private final HttpRequestResponse originalExchange;
    private final List<DiscoveryEvidence> evidence = new ArrayList<>();
    private volatile ReflectionResult reflectionResult = ReflectionResult.notTested();

    public ParameterCandidate(String name, PageIdentity pageIdentity, boolean existing,
                              HttpRequestResponse originalExchange) {
        this.name = Objects.requireNonNull(name, "name");
        this.pageIdentity = Objects.requireNonNull(pageIdentity, "pageIdentity");
        this.existing = existing;
        this.originalExchange = Objects.requireNonNull(originalExchange, "originalExchange");
    }

    public String name() { return name; }
    public PageIdentity pageIdentity() { return pageIdentity; }
    public boolean existing() { return existing; }
    public HttpRequestResponse originalExchange() { return originalExchange; }

    public synchronized void addEvidence(DiscoveryEvidence item) {
        if (!evidence.contains(item)) {
            evidence.add(item);
        }
    }

    public synchronized List<DiscoveryEvidence> evidence() {
        return Collections.unmodifiableList(new ArrayList<>(evidence));
    }

    public DiscoveryConfidence confidence() {
        return evidence().stream().map(DiscoveryEvidence::confidence)
                .min(Enum::compareTo).orElse(DiscoveryConfidence.LOW);
    }

    public String sourceSummary() {
        return evidence().stream().map(e -> e.source().name()).distinct()
                .reduce((a, b) -> a + ", " + b).orElse("OTHER");
    }

    public ReflectionResult reflectionResult() { return reflectionResult; }
    public void setReflectionResult(ReflectionResult result) {
        reflectionResult = Objects.requireNonNull(result, "result");
    }
}

