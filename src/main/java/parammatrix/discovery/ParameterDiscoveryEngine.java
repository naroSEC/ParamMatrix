package parammatrix.discovery;

import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.config.ExtensionConfig;
import parammatrix.model.DiscoveryEvidence;
import parammatrix.model.PageIdentity;
import parammatrix.model.ParameterCandidate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ParameterDiscoveryEngine {
    private final HtmlParameterExtractor htmlExtractor;
    private final JavaScriptParser javaScriptParser;
    private final ContentTypeClassifier classifier;
    private final ExtensionConfig config;

    public ParameterDiscoveryEngine(HtmlParameterExtractor htmlExtractor,
                                    JavaScriptParser javaScriptParser,
                                    ContentTypeClassifier classifier,
                                    ExtensionConfig config) {
        this.htmlExtractor = htmlExtractor;
        this.javaScriptParser = javaScriptParser;
        this.classifier = classifier;
        this.config = config;
    }

    public List<ParameterCandidate> discover(HttpRequestResponse exchange) {
        if (!exchange.hasResponse() || !classifier.shouldAnalyze(exchange.response(), config)) {
            return List.of();
        }
        PageIdentity identity = PageIdentity.from(exchange.request(),
                config.identityIgnoresParameterValues.get());
        Set<String> existing = exchange.request().parameters().stream()
                .map(p -> p.name()).collect(Collectors.toSet());
        HtmlParameterExtractor.Extraction extraction = htmlExtractor.extract(
                exchange.response().bodyToString(), exchange.request().url(), config);
        List<DiscoveredParameter> found = new ArrayList<>(extraction.parameters());
        if (config.inlineJavaScript.get() && config.javaScriptSinkAnalysis.get()) {
            for (String script : extraction.inlineScripts()) {
                found.addAll(javaScriptParser.extract(script, exchange.request().url()));
            }
        }

        Map<String, ParameterCandidate> candidates = new LinkedHashMap<>();
        for (DiscoveredParameter item : found) {
            if (!config.includeLowConfidence.get()
                    && item.confidence() == parammatrix.model.DiscoveryConfidence.LOW) continue;
            if (candidates.size() >= config.maximumCandidatesPerPage.get()
                    && !candidates.containsKey(item.name())) break;
            ParameterCandidate candidate = candidates.computeIfAbsent(item.name(), name ->
                    new ParameterCandidate(name, identity, existing.contains(name), exchange));
            candidate.addEvidence(new DiscoveryEvidence(item.source(), item.detail(),
                    item.endpoint(), item.confidence()));
        }
        return List.copyOf(candidates.values());
    }
}
