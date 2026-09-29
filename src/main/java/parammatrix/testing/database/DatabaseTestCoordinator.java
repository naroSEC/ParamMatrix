package parammatrix.testing.database;

import parammatrix.config.ExtensionConfig;
import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterCandidate;
import parammatrix.model.ReflectionStatus;
import parammatrix.storage.DatabaseResultRepository;

import java.util.ArrayList;
import java.util.List;

public final class DatabaseTestCoordinator {
    private final DatabaseErrorTestEngine testEngine;
    private final DatabaseStressPayloadProvider payloadProvider;
    private final DatabaseResultRepository repository;
    private final ExtensionConfig extensionConfig;

    public DatabaseTestCoordinator(DatabaseErrorTestEngine testEngine,
                                   DatabaseStressPayloadProvider payloadProvider,
                                   DatabaseResultRepository repository,
                                   ExtensionConfig extensionConfig) {
        this.testEngine = testEngine;
        this.payloadProvider = payloadProvider;
        this.repository = repository;
        this.extensionConfig = extensionConfig;
    }

    public List<DatabaseTestResult> testPage(List<ParameterCandidate> pageCandidates,
                                             DatabaseRunOptions options) {
        List<ParameterCandidate> candidates = pageCandidates.stream()
                .filter(candidate -> !options.onlyReflectedParameters()
                        || candidate.reflectionResult().status() == ReflectionStatus.REFLECTED)
                .toList();
        if (candidates.isEmpty() || options.databases().isEmpty()) return List.of();
        List<DatabaseTestResult> results = new ArrayList<>();
        int requests = 0;
        boolean inScope = !extensionConfig.inScopeOnly.get()
                || candidates.getFirst().originalExchange().request().isInScope();
        for (ParameterCandidate candidate : candidates) {
            for (DatabaseStressPayload payload : payloadProvider.payloads()) {
                DatabaseTestResult result;
                if (!inScope) {
                    result = skipped(candidate, payload,
                            "Active testing is restricted to Burp scope");
                } else if (requests >= options.maximumRequestsPerPage()) {
                    result = skipped(candidate, payload,
                            "Maximum database stress requests per page reached");
                } else {
                    if (requests > 0) delay(options.requestDelayMillis());
                    result = testEngine.execute(candidate.originalExchange(), candidate, payload,
                            options.databases());
                    requests++;
                    if (result.status() == DatabaseTestStatus.DB_ERROR_DETECTED
                            && options.verifyPositiveResults()
                            && requests < options.maximumRequestsPerPage()) {
                        delay(options.requestDelayMillis());
                        DatabaseTestResult confirmation = testEngine.execute(
                                candidate.originalExchange(), candidate, payload, options.databases());
                        requests++;
                        boolean verified = confirmation.status()
                                == DatabaseTestStatus.DB_ERROR_DETECTED
                                && confirmation.suspectedDatabase() == result.suspectedDatabase();
                        result = result.withVerification(verified, confirmation.evidence());
                    }
                }
                results.add(result);
                repository.save(result);
            }
        }
        return List.copyOf(results);
    }

    private DatabaseTestResult skipped(ParameterCandidate candidate,
                                       DatabaseStressPayload payload, String reason) {
        short originalStatus = candidate.originalExchange().hasResponse()
                ? candidate.originalExchange().response().statusCode() : 0;
        return new DatabaseTestResult(candidate.name(), DatabaseTestStatus.SKIPPED,
                DatabaseType.GENERIC, payload.name(), payload.value(), "",
                DiscoveryConfidence.LOW, false, originalStatus, (short) 0, 0, reason,
                candidate.originalExchange(), null);
    }

    private void delay(int millis) {
        if (millis <= 0) return;
        try { Thread.sleep(millis); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}
