package parammatrix.testing.ssti;

import parammatrix.config.ExtensionConfig;
import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterCandidate;
import parammatrix.model.ReflectionStatus;
import parammatrix.storage.SstiResultRepository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SstiCoordinator {
    private final SstiTestEngine testEngine;
    private final Map<SstiEngine, SstiPayloadProvider> providers = new EnumMap<>(SstiEngine.class);
    private final SstiResultRepository repository;
    private final ExtensionConfig extensionConfig;

    public SstiCoordinator(SstiTestEngine testEngine, List<SstiPayloadProvider> providers,
                           SstiResultRepository repository, ExtensionConfig extensionConfig) {
        this.testEngine = testEngine;
        for (SstiPayloadProvider provider : providers) this.providers.put(provider.engine(), provider);
        this.repository = repository;
        this.extensionConfig = extensionConfig;
    }

    public List<SstiTestResult> testPage(List<ParameterCandidate> pageCandidates,
                                         SstiRunOptions options) {
        List<ParameterCandidate> candidates = pageCandidates.stream()
                .filter(candidate -> !options.onlyReflectedParameters()
                        || candidate.reflectionResult().status() == ReflectionStatus.REFLECTED)
                .toList();
        if (candidates.isEmpty() || options.engines().isEmpty()) return List.of();

        List<SstiTestResult> results = new ArrayList<>();
        int requests = 0;
        boolean inScope = !extensionConfig.inScopeOnly.get()
                || candidates.getFirst().originalExchange().request().isInScope();
        for (ParameterCandidate candidate : candidates) {
            for (SstiEngine engine : options.engines()) {
                SstiPayloadProvider provider = providers.get(engine);
                if (provider == null) continue;
                for (SstiPayload payload : provider.payloads()) {
                    SstiTestResult result;
                    if (!inScope) {
                        result = skipped(candidate, payload,
                                "Active testing is restricted to Burp scope");
                    } else if (requests >= options.maximumRequestsPerPage()) {
                        result = skipped(candidate, payload,
                                "Maximum SSTI requests per page reached");
                    } else {
                        if (requests > 0) delay(options.requestDelayMillis());
                        result = testEngine.execute(candidate.originalExchange(), candidate, payload);
                        requests++;
                    }
                    results.add(result);
                    repository.save(result);
                }
            }
        }
        return List.copyOf(results);
    }

    private SstiTestResult skipped(ParameterCandidate candidate, SstiPayload payload, String reason) {
        return new SstiTestResult(candidate.name(), payload.engine(), SstiTestStatus.SKIPPED,
                payload.payload(), payload.expectedResult(), "Skipped", payload.detectionMethod(),
                DiscoveryConfidence.LOW, reason, candidate.originalExchange(), null);
    }

    private void delay(int millis) {
        if (millis <= 0) return;
        try { Thread.sleep(millis); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}

