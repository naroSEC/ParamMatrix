package parammatrix.discovery;

import burp.api.montoya.http.message.responses.HttpResponse;
import parammatrix.config.ExtensionConfig;

import java.util.Locale;

public final class ContentTypeClassifier {
    public boolean shouldAnalyze(HttpResponse response, ExtensionConfig config) {
        if (response == null || response.body().length() > config.maximumResponseBytes.get()) {
            return false;
        }
        String contentType = response.headerValue("Content-Type");
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String body = response.bodyToString().stripLeading();

        if (config.ignoreJsonResponses.get()
                && (normalized.contains("application/json")
                || normalized.contains("text/json")
                || looksLikeJson(body))) {
            return false;
        }
        if (config.analyzeHtml.get() && normalized.contains("text/html")) {
            return true;
        }
        if (config.analyzeXhtml.get() && normalized.contains("application/xhtml+xml")) {
            return true;
        }
        return config.htmlAutoDetection.get() && looksLikeHtml(body);
    }

    static boolean looksLikeJson(String body) {
        if (body.isEmpty()) return false;
        char first = body.charAt(0);
        if (first != '{' && first != '[') return false;
        int sampleLength = Math.min(body.length(), 4096);
        String sample = body.substring(0, sampleLength).stripTrailing();
        char last = sample.charAt(sample.length() - 1);
        return (first == '{' && (last == '}' || sample.contains("\":")))
                || (first == '[' && (last == ']' || sample.contains(",")));
    }

    static boolean looksLikeHtml(String body) {
        String sample = body.substring(0, Math.min(body.length(), 8192)).toLowerCase(Locale.ROOT);
        return sample.contains("<!doctype") || sample.contains("<html")
                || sample.contains("<head") || sample.contains("<body")
                || sample.contains("<script");
    }
}

