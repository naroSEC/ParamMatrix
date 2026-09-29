package parammatrix.http;

import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.Map;

/** Adds top-level string fields to a simple JSON object without reformatting existing members. */
public final class SimpleJsonRequestInjector implements JsonRequestInjector {
    @Override
    public HttpRequest inject(HttpRequest request, Map<String, String> values) {
        String body = request.bodyToString().trim();
        if (!body.startsWith("{") || !body.endsWith("}")) {
            throw new IllegalArgumentException("JSON request body is not a top-level object");
        }
        StringBuilder addition = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (addition.length() > 0) addition.append(',');
            addition.append('"').append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue())).append('"');
        }
        String inside = body.substring(1, body.length() - 1).trim();
        String replacement = "{" + inside + (inside.isEmpty() ? "" : ",") + addition + "}";
        return request.withBody(replacement);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

