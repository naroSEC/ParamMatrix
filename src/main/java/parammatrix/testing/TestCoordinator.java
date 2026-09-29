package parammatrix.testing;

import parammatrix.config.ExtensionConfig;
import parammatrix.model.ParameterCandidate;
import parammatrix.model.ReflectionResult;
import parammatrix.model.ReflectionStatus;
import parammatrix.storage.ResultRepository;

import java.util.List;
import java.util.Map;

public final class TestCoordinator {
    private final ReflectionTestEngine reflection;
    private final ExtensionConfig config;
    private final ResultRepository repository;

    public TestCoordinator(ReflectionTestEngine reflection, ExtensionConfig config,
                           ResultRepository repository) {
        this.reflection = reflection;
        this.config = config;
        this.repository = repository;
    }

    public void testReflections(List<ParameterCandidate> candidates) {
        if (candidates.isEmpty() || !config.reflectionEnabled.get()) return;
        if (config.inScopeOnly.get() && !candidates.getFirst().originalExchange().request().isInScope()) {
            candidates.forEach(candidate -> candidate.setReflectionResult(
                    ReflectionResult.skipped("Active testing is restricted to Burp scope")));
            repository.changed();
            return;
        }
        int remaining = config.maximumRequestsPerPage.get();
        if (remaining <= 0) return;
        ExtensionConfig.ReflectionMode mode = config.reflectionMode;
        if (mode == ExtensionConfig.ReflectionMode.INDIVIDUAL_ONLY) {
            for (ParameterCandidate candidate : candidates) {
                if (remaining-- <= 0) {
                    candidate.setReflectionResult(ReflectionResult.skipped("Maximum requests per page reached"));
                    continue;
                }
                apply(reflection.executeBatch(candidate.originalExchange(), List.of(candidate)));
                delay();
            }
        } else {
            Map<ParameterCandidate, ReflectionResult> batch = reflection.executeBatch(
                    candidates.getFirst().originalExchange(), candidates);
            remaining--;
            apply(batch);
            if (mode == ExtensionConfig.ReflectionMode.BATCH_AND_VERIFY) {
                for (ParameterCandidate candidate : candidates) {
                    if (candidate.reflectionResult().status() != ReflectionStatus.REFLECTED) continue;
                    if (remaining-- <= 0) break;
                    delay();
                    apply(reflection.executeBatch(candidate.originalExchange(), List.of(candidate)));
                }
            }
        }
        repository.changed();
    }

    private void apply(Map<ParameterCandidate, ReflectionResult> results) {
        results.forEach(ParameterCandidate::setReflectionResult);
    }

    private void delay() {
        int millis = config.requestDelayMillis.get();
        if (millis <= 0) return;
        try { Thread.sleep(millis); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}
