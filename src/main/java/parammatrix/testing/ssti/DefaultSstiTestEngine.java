package parammatrix.testing.ssti;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.http.PayloadEncodingMode;
import parammatrix.http.PayloadMutation;
import parammatrix.http.RequestMutator;
import parammatrix.http.RequestSender;
import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterCandidate;

public final class DefaultSstiTestEngine implements SstiTestEngine {
    private final RequestMutator requestMutator;
    private final RequestSender sender;

    public DefaultSstiTestEngine(RequestMutator requestMutator, RequestSender sender) {
        this.requestMutator = requestMutator;
        this.sender = sender;
    }

    @Override
    public SstiTestResult execute(HttpRequestResponse base, ParameterCandidate parameter,
                                  SstiPayload payload, PayloadEncodingMode encodingMode) {
        PayloadMutation mutation = null;
        try {
            if (base.hasResponse() && base.response().bodyToString().contains(payload.expectedResult())) {
                return result(parameter, payload, SstiTestStatus.SKIPPED,
                        "Expected token already existed in the original response", "Baseline collision",
                        DiscoveryConfidence.LOW, base, null);
            }
            mutation = requestMutator.injectPayload(base.request(), parameter.name(),
                    payload.payload(), encodingMode);
            HttpRequestResponse exchange = sender.send(mutation.request());
            if (!exchange.hasResponse()) {
                return result(parameter, payload, SstiTestStatus.ERROR,
                        "No response received", "No response", DiscoveryConfidence.LOW, base,
                        exchange, mutation);
            }
            String body = exchange.response().bodyToString();
            boolean detected = body.contains(payload.expectedResult());
            return result(parameter, payload,
                    detected ? SstiTestStatus.DETECTED : SstiTestStatus.NOT_DETECTED,
                    detected ? evidence(body, payload.expectedResult())
                            : "Expected evaluated token was not present in the response",
                    detected ? payload.expectedResult() : "Not observed",
                    detected ? DiscoveryConfidence.HIGH : DiscoveryConfidence.LOW,
                    base, exchange, mutation);
        } catch (RuntimeException exception) {
            return result(parameter, payload, SstiTestStatus.ERROR,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    "Error", DiscoveryConfidence.LOW, base, null, mutation);
        }
    }

    private SstiTestResult result(ParameterCandidate parameter, SstiPayload payload,
                                  SstiTestStatus status, String evidence, String actual,
                                  DiscoveryConfidence confidence, HttpRequestResponse original,
                                  HttpRequestResponse test) {
        return result(parameter, payload, status, evidence, actual, confidence, original, test,
                null);
    }

    private SstiTestResult result(ParameterCandidate parameter, SstiPayload payload,
                                  SstiTestStatus status, String evidence, String actual,
                                  DiscoveryConfidence confidence, HttpRequestResponse original,
                                  HttpRequestResponse test, PayloadMutation mutation) {
        return new SstiTestResult(parameter.name(), payload.engine(), status, payload.payload(),
                mutation == null ? "" : mutation.wireValue(),
                mutation == null ? "Not applied" : mutation.encodingDescription(),
                payload.expectedResult(), actual, payload.detectionMethod(), confidence,
                evidence, original, test);
    }

    private String evidence(String body, String expected) {
        int index = body.indexOf(expected);
        int start = Math.max(0, index - 120);
        int end = Math.min(body.length(), index + expected.length() + 120);
        return body.substring(start, end).replace("\r", "\\r").replace("\n", "\\n");
    }
}

