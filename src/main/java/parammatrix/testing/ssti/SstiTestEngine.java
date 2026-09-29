package parammatrix.testing.ssti;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.model.ParameterCandidate;

import java.util.List;
import java.util.Set;

/**
 * Phase-6 extension point only. Reflection filtering is a caller-selected policy, not a dependency.
 */
public interface SstiTestEngine {
    List<SstiTestResult> execute(HttpRequestResponse base, ParameterCandidate parameter,
                                 Set<SstiEngine> engines);
}
