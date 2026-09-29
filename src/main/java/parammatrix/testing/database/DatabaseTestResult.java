package parammatrix.testing.database;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.model.DiscoveryConfidence;

public record DatabaseTestResult(
        String parameter,
        DatabaseTestStatus status,
        DatabaseType suspectedDatabase,
        String payloadName,
        String payload,
        String errorSignature,
        DiscoveryConfidence confidence,
        boolean verified,
        short originalStatus,
        short testStatus,
        int responseLengthDelta,
        String evidence,
        HttpRequestResponse originalExchange,
        HttpRequestResponse testExchange) {

    public DatabaseTestResult withVerification(boolean confirmed, String verificationEvidence) {
        return new DatabaseTestResult(parameter, status, suspectedDatabase, payloadName, payload,
                errorSignature, confirmed ? DiscoveryConfidence.HIGH : DiscoveryConfidence.MEDIUM,
                confirmed, originalStatus, testStatus, responseLengthDelta,
                evidence + "\n\nVerification: " + verificationEvidence,
                originalExchange, testExchange);
    }
}

