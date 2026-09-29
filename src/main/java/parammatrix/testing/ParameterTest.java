package parammatrix.testing;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.model.ParameterCandidate;
import parammatrix.model.TestResult;

public interface ParameterTest {
    String id();
    TestResult execute(HttpRequestResponse base, ParameterCandidate parameter);
}

