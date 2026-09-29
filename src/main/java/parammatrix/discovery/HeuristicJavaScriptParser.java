package parammatrix.discovery;

import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative sink-oriented parser. It deliberately ignores declarations and only examines
 * values that flow into HTTP/navigation APIs. The interface permits swapping in an AST parser.
 */
public final class HeuristicJavaScriptParser implements JavaScriptParser {
    private static final Pattern CALL = Pattern.compile(
            "(?is)(fetch|axios\\.(?:get|post|put|patch|delete)|\\$\\.(?:ajax|get|post)|window\\.open)\\s*\\((.{1,4000}?)\\)\\s*;?");
    private static final Pattern LOCATION = Pattern.compile(
            "(?is)(?:window\\.)?(?:document\\.)?location(?:\\.href)?\\s*=\\s*([^;]{1,2000})");
    private static final Pattern STRING = Pattern.compile("(['\"])(.*?)\\1", Pattern.DOTALL);
    private static final Pattern OBJECT = Pattern.compile(
            "(?s)(?:data|body|params)\\s*:\\s*\\{(.{1,3000}?)\\}");
    private static final Pattern DIRECT_OBJECT = Pattern.compile(
            "(?s)(?:axios\\.(?:post|put|patch)|\\$\\.(?:post))\\s*\\([^,]+,\\s*\\{(.{1,3000}?)\\}");
    private static final Pattern OBJECT_KEY = Pattern.compile(
            "(?:^|,)\\s*(?:['\"]([^'\"]+)['\"]|([A-Za-z_$][\\w$.-]*))\\s*:");
    private static final Pattern APPEND = Pattern.compile(
            "(?is)(?:FormData|URLSearchParams|\\w+)?.{0,80}?\\.(append|set)\\s*\\(\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern CONSTRUCTOR_OBJECT = Pattern.compile(
            "(?is)new\\s+URLSearchParams\\s*\\(\\s*\\{(.{1,3000}?)\\}\\s*\\)");

    private final UrlParameterExtractor urlExtractor = new UrlParameterExtractor();

    @Override
    public List<DiscoveredParameter> extract(String script, String documentUrl) {
        List<DiscoveredParameter> results = new ArrayList<>();
        if (script == null || script.isBlank()) return results;

        Matcher callMatcher = CALL.matcher(script);
        while (callMatcher.find()) {
            String sink = callMatcher.group(1).toLowerCase();
            String args = callMatcher.group(2);
            ParameterSource source = sourceFor(sink);
            extractStringQueries(args, source, sink, results);
            extractObjectKeys(args, source == ParameterSource.JAVASCRIPT_FETCH
                    ? ParameterSource.JAVASCRIPT_OBJECT : source, sink, results);
        }

        Matcher direct = DIRECT_OBJECT.matcher(script);
        while (direct.find()) {
            addObjectKeys(direct.group(1), ParameterSource.JAVASCRIPT_OBJECT,
                    "HTTP request object", results);
        }

        Matcher location = LOCATION.matcher(script);
        while (location.find()) {
            extractStringQueries(location.group(1), ParameterSource.JAVASCRIPT_LOCATION,
                    "location navigation", results);
        }

        Matcher append = APPEND.matcher(script);
        while (append.find()) {
            String prefix = script.substring(Math.max(0, append.start() - 100), append.start());
            ParameterSource source = prefix.contains("FormData")
                    ? ParameterSource.JAVASCRIPT_FORMDATA
                    : ParameterSource.JAVASCRIPT_URLSEARCHPARAMS;
            add(append.group(2), source, append.group(1) + "()", "", DiscoveryConfidence.HIGH, results);
        }

        Matcher constructor = CONSTRUCTOR_OBJECT.matcher(script);
        while (constructor.find()) {
            addObjectKeys(constructor.group(1), ParameterSource.JAVASCRIPT_URLSEARCHPARAMS,
                    "URLSearchParams object", results);
        }
        return deduplicate(results);
    }

    private void extractStringQueries(String expression, ParameterSource source, String detail,
                                      List<DiscoveredParameter> results) {
        Matcher strings = STRING.matcher(expression);
        while (strings.find()) {
            results.addAll(urlExtractor.extract(strings.group(2), source, detail));
        }
        Matcher explicitQuery = Pattern.compile("[?&]([A-Za-z_][\\w.\\[\\]-]*)\\s*=").matcher(expression);
        while (explicitQuery.find()) {
            add(explicitQuery.group(1), source, detail + " query construction", "",
                    DiscoveryConfidence.HIGH, results);
        }
    }

    private void extractObjectKeys(String args, ParameterSource source, String detail,
                                   List<DiscoveredParameter> results) {
        Matcher object = OBJECT.matcher(args);
        while (object.find()) addObjectKeys(object.group(1), source, detail + " data/body", results);
    }

    private void addObjectKeys(String body, ParameterSource source, String detail,
                               List<DiscoveredParameter> results) {
        Matcher key = OBJECT_KEY.matcher(body);
        while (key.find()) {
            String name = key.group(1) != null ? key.group(1) : key.group(2);
            add(name, source, detail, "", DiscoveryConfidence.MEDIUM, results);
        }
    }

    private void add(String name, ParameterSource source, String detail, String endpoint,
                     DiscoveryConfidence confidence, List<DiscoveredParameter> results) {
        if (UrlParameterExtractor.isReasonableName(name)) {
            results.add(new DiscoveredParameter(name, source, detail, endpoint, confidence));
        }
    }

    private static ParameterSource sourceFor(String sink) {
        if (sink.equals("fetch")) return ParameterSource.JAVASCRIPT_FETCH;
        if (sink.contains("ajax") || sink.startsWith("$.")) return ParameterSource.JAVASCRIPT_AJAX;
        if (sink.equals("window.open")) return ParameterSource.JAVASCRIPT_LOCATION;
        return ParameterSource.JAVASCRIPT_AJAX;
    }

    private static List<DiscoveredParameter> deduplicate(List<DiscoveredParameter> input) {
        Set<String> seen = new LinkedHashSet<>();
        List<DiscoveredParameter> output = new ArrayList<>();
        for (DiscoveredParameter item : input) {
            String key = item.name() + "|" + item.source() + "|" + item.detail();
            if (seen.add(key)) output.add(item);
        }
        return output;
    }
}

