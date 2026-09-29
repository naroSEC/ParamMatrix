package parammatrix.testing.ssti;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.model.DiscoveryConfidence;

public record SstiTestResult(
        String parameter,
        SstiEngine templateEngine,
        String payload,
        String expectedResult,
        String actualResult,
        String detectionMethod,
        DiscoveryConfidence confidence,
        String evidence,
        HttpRequestResponse exchange) {
}

