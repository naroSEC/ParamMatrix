package parammatrix.testing.ssti;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.model.ParameterCandidate;

public interface SstiTestEngine {
    SstiTestResult execute(HttpRequestResponse base, ParameterCandidate parameter,
                           SstiPayload payload);
}
