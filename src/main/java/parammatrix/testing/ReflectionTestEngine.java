package parammatrix.testing;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import parammatrix.analysis.ReflectionContextAnalyzer;
import parammatrix.http.MarkerGenerator;
import parammatrix.http.RequestMutator;
import parammatrix.http.RequestSender;
import parammatrix.model.ParameterCandidate;
import parammatrix.model.ReflectionContext;
import parammatrix.model.ReflectionResult;
import parammatrix.model.ReflectionStatus;
import parammatrix.model.TestResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReflectionTestEngine implements ParameterTest {
    private final MarkerGenerator markerGenerator;
    private final RequestMutator requestMutator;
    private final RequestSender sender;
    private final ReflectionContextAnalyzer analyzer;

    public ReflectionTestEngine(MarkerGenerator markerGenerator, RequestMutator requestMutator,
                                RequestSender sender, ReflectionContextAnalyzer analyzer) {
        this.markerGenerator = markerGenerator;
        this.requestMutator = requestMutator;
        this.sender = sender;
        this.analyzer = analyzer;
    }

    @Override public String id() { return "reflection"; }

    @Override
    public TestResult execute(HttpRequestResponse base, ParameterCandidate parameter) {
        ReflectionResult result = executeBatch(base, List.of(parameter)).get(parameter);
        return new TestResult(id(), parameter.name(), result.status() == ReflectionStatus.REFLECTED,
                result.evidence(), result.exchange());
    }

    public Map<ParameterCandidate, ReflectionResult> executeBatch(
            HttpRequestResponse base, List<ParameterCandidate> candidates) {
        Map<ParameterCandidate, String> markers = new LinkedHashMap<>();
        Map<String, String> values = new LinkedHashMap<>();
        for (ParameterCandidate candidate : candidates) {
            String marker = markerGenerator.generate(candidate.name());
            markers.put(candidate, marker);
            values.put(candidate.name(), marker);
        }
        Map<ParameterCandidate, ReflectionResult> results = new LinkedHashMap<>();
        try {
            HttpRequest testRequest = requestMutator.inject(base.request(), values);
            HttpRequestResponse testExchange = sender.send(testRequest);
            if (!testExchange.hasResponse()) {
                markers.forEach((candidate, marker) -> results.put(candidate,
                        new ReflectionResult(ReflectionStatus.ERROR, ReflectionContext.UNKNOWN,
                                marker, "No response received", testExchange, candidates.size() == 1)));
                return results;
            }
            String body = testExchange.response().bodyToString();
            markers.forEach((candidate, marker) -> {
                boolean reflected = body.contains(marker);
                results.put(candidate, new ReflectionResult(
                        reflected ? ReflectionStatus.REFLECTED : ReflectionStatus.NOT_REFLECTED,
                        reflected ? analyzer.analyze(body, marker) : ReflectionContext.UNKNOWN,
                        marker, analyzer.evidence(body, marker), testExchange, candidates.size() == 1));
            });
        } catch (RuntimeException exception) {
            markers.forEach((candidate, marker) -> results.put(candidate,
                    new ReflectionResult(ReflectionStatus.ERROR, ReflectionContext.UNKNOWN,
                            marker, exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                            null, candidates.size() == 1)));
        }
        return results;
    }
}

