package parammatrix.testing.ssti;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.model.DiscoveryConfidence;

public record SstiTestResult(
        String parameter,
        SstiEngine templateEngine,
        SstiTestStatus status,
        String payload,
        String wirePayload,
        String encodingDescription,
        String expectedResult,
        String actualResult,
        String detectionMethod,
        DiscoveryConfidence confidence,
        String evidence,
        HttpRequestResponse originalExchange,
        HttpRequestResponse testExchange) {
}
