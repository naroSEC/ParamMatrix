package parammatrix.testing.database;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.analysis.DatabaseErrorSignatureAnalyzer;
import parammatrix.http.PayloadEncodingMode;
import parammatrix.http.PayloadMutation;
import parammatrix.http.RequestMutator;
import parammatrix.http.RequestSender;
import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterCandidate;

import java.util.Optional;
import java.util.Set;

public final class DatabaseErrorTestEngine {
    private final RequestMutator requestMutator;
    private final RequestSender sender;
    private final DatabaseErrorSignatureAnalyzer analyzer;

    public DatabaseErrorTestEngine(RequestMutator requestMutator, RequestSender sender,
                                   DatabaseErrorSignatureAnalyzer analyzer) {
        this.requestMutator = requestMutator;
        this.sender = sender;
        this.analyzer = analyzer;
    }

    public DatabaseTestResult execute(HttpRequestResponse base, ParameterCandidate parameter,
                                      DatabaseStressPayload payload,
                                      Set<DatabaseType> enabledDatabases,
                                      PayloadEncodingMode encodingMode) {
        PayloadMutation mutation = null;
        try {
            String baseline = base.hasResponse() ? base.response().bodyToString() : "";
            short originalStatus = base.hasResponse() ? base.response().statusCode() : 0;
            mutation = requestMutator.injectPayload(base.request(), parameter.name(),
                    payload.value(), encodingMode);
            HttpRequestResponse exchange = sender.send(mutation.request());
            if (!exchange.hasResponse()) {
                return result(parameter, payload, DatabaseTestStatus.ERROR, DatabaseType.GENERIC,
                        "", DiscoveryConfidence.LOW, false, originalStatus, (short) 0, 0,
                        "No response received", base, exchange, mutation);
            }
            String tested = exchange.response().bodyToString();
            short testStatus = exchange.response().statusCode();
            int lengthDelta = tested.length() - baseline.length();
            Optional<DatabaseErrorMatch> error = analyzer.findNewError(baseline, tested,
                    enabledDatabases);
            if (error.isPresent()) {
                DatabaseErrorMatch match = error.get();
                return result(parameter, payload, DatabaseTestStatus.DB_ERROR_DETECTED,
                        match.database(), match.signature(), DiscoveryConfidence.MEDIUM, false,
                        originalStatus, testStatus, lengthDelta, match.evidence(), base, exchange,
                        mutation);
            }
            if (behaviorChanged(originalStatus, testStatus, baseline.length(), tested.length())) {
                return result(parameter, payload, DatabaseTestStatus.BEHAVIOR_CHANGED,
                        DatabaseType.GENERIC, "", DiscoveryConfidence.LOW, false,
                        originalStatus, testStatus, lengthDelta,
                        "Response behavior changed without a recognized database error signature",
                        base, exchange, mutation);
            }
            return result(parameter, payload, DatabaseTestStatus.NOT_DETECTED,
                    DatabaseType.GENERIC, "", DiscoveryConfidence.LOW, false,
                    originalStatus, testStatus, lengthDelta,
                    "No new database error signature or significant response change was observed",
                    base, exchange, mutation);
        } catch (RuntimeException exception) {
            return result(parameter, payload, DatabaseTestStatus.ERROR, DatabaseType.GENERIC,
                    "", DiscoveryConfidence.LOW, false,
                    base.hasResponse() ? base.response().statusCode() : 0,
                    (short) 0, 0, exception.getClass().getSimpleName() + ": "
                            + exception.getMessage(), base, null, mutation);
        }
    }

    private boolean behaviorChanged(short originalStatus, short testStatus,
                                    int originalLength, int testLength) {
        if (originalStatus < 500 && testStatus >= 500) return true;
        int difference = Math.abs(testLength - originalLength);
        int threshold = Math.max(500, (int) (Math.max(1, originalLength) * 0.30));
        return difference >= threshold;
    }

    private DatabaseTestResult result(ParameterCandidate parameter, DatabaseStressPayload payload,
                                      DatabaseTestStatus status, DatabaseType database,
                                      String signature, DiscoveryConfidence confidence,
                                      boolean verified, short originalStatus, short testStatus,
                                      int lengthDelta, String evidence, HttpRequestResponse original,
                                      HttpRequestResponse test, PayloadMutation mutation) {
        return new DatabaseTestResult(parameter.name(), status, database, payload.name(),
                payload.value(), mutation == null ? "" : mutation.wireValue(),
                mutation == null ? "Not applied" : mutation.encodingDescription(), signature,
                confidence, verified, originalStatus, testStatus, lengthDelta, evidence, original,
                test);
    }
}
