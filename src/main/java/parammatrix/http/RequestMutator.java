package parammatrix.http;

import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RequestMutator {
    private static final Set<HttpParameterType> MUTABLE_EXISTING_TYPES = Set.of(
            HttpParameterType.URL, HttpParameterType.BODY, HttpParameterType.MULTIPART_ATTRIBUTE);
    private final JsonRequestInjector jsonInjector;

    public RequestMutator(JsonRequestInjector jsonInjector) {
        this.jsonInjector = jsonInjector;
    }

    public HttpRequest inject(HttpRequest original, Map<String, String> values) {
        String contentType = value(original.headerValue("Content-Type")).toLowerCase(Locale.ROOT);
        if (contentType.contains("application/json")) {
            return jsonInjector.inject(original, values);
        }

        HttpParameterType defaultType = parameterType(original, contentType);
        List<HttpParameter> additions = new ArrayList<>();
        List<HttpParameter> updates = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            ParsedHttpParameter existing = original.parameters().stream()
                    .filter(parameter -> parameter.name().equals(entry.getKey()))
                    .filter(parameter -> MUTABLE_EXISTING_TYPES.contains(parameter.type()))
                    .findFirst().orElse(null);
            HttpParameterType type = existing == null ? defaultType : existing.type();
            HttpParameter parameter = HttpParameter.parameter(entry.getKey(), entry.getValue(), type);
            if (existing == null) additions.add(parameter); else updates.add(parameter);
        }
        HttpRequest mutated = updates.isEmpty() ? original : original.withUpdatedParameters(updates);
        return additions.isEmpty() ? mutated : mutated.withAddedParameters(additions);
    }

    private static HttpParameterType parameterType(HttpRequest request, String contentType) {
        String method = request.method().toUpperCase(Locale.ROOT);
        if (method.equals("GET") || method.equals("HEAD") || method.equals("DELETE")) {
            return HttpParameterType.URL;
        }
        if (contentType.contains("multipart/form-data")) return HttpParameterType.MULTIPART_ATTRIBUTE;
        if (contentType.contains("application/x-www-form-urlencoded")) return HttpParameterType.BODY;
        return HttpParameterType.URL;
    }

    private static String value(String value) { return value == null ? "" : value; }
}
