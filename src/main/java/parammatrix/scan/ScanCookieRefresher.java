package parammatrix.scan;

import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ScanCookieRefresher {
    RefreshResult refresh(HttpRequestResponse exchange, List<Cookie> cookies,
                          ZonedDateTime now) {
        HttpRequest request = exchange.request();
        String host = request.httpService().host().toLowerCase(Locale.ROOT);
        String path = normalizePath(request.pathWithoutQuery());
        Map<String, String> matching = new LinkedHashMap<>();
        cookies.stream()
                .filter(cookie -> usable(cookie, host, path, now))
                .sorted(Comparator.comparingInt((Cookie cookie) ->
                        normalizeCookiePath(cookie.path()).length()).reversed()
                        .thenComparing(Cookie::name))
                .forEach(cookie -> matching.putIfAbsent(cookie.name(), cookie.value()));
        if (matching.isEmpty()) return new RefreshResult(request, false);

        String headerValue = matching.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("; "));
        HttpRequest refreshed = request.hasHeader("Cookie")
                ? request.withUpdatedHeader("Cookie", headerValue)
                : request.withAddedHeader("Cookie", headerValue);
        return new RefreshResult(refreshed, true);
    }

    RefreshResult replaceWithCustom(HttpRequestResponse exchange, String targetHost,
                                    String customCookieHeader) {
        HttpRequest request = exchange.request();
        if (!request.httpService().host().equalsIgnoreCase(targetHost)) {
            return new RefreshResult(request, false);
        }
        String headerValue = normalizeCustomHeader(customCookieHeader);
        if (headerValue.isBlank()) return new RefreshResult(request, false);
        HttpRequest refreshed = request.hasHeader("Cookie")
                ? request.withUpdatedHeader("Cookie", headerValue)
                : request.withAddedHeader("Cookie", headerValue);
        return new RefreshResult(refreshed, true);
    }

    private boolean usable(Cookie cookie, String host, String requestPath, ZonedDateTime now) {
        if (cookie.name() == null || cookie.name().isBlank() || cookie.value() == null) return false;
        if (cookie.expiration().isPresent() && !cookie.expiration().get().isAfter(now)) return false;
        return domainMatches(host, cookie.domain())
                && pathMatches(requestPath, cookie.path());
    }

    private boolean domainMatches(String host, String cookieDomain) {
        if (cookieDomain == null || cookieDomain.isBlank()) return false;
        String domain = cookieDomain.toLowerCase(Locale.ROOT);
        if (domain.startsWith(".")) {
            String parent = domain.substring(1);
            return host.equals(parent) || host.endsWith("." + parent);
        }
        return host.equals(domain);
    }

    private boolean pathMatches(String requestPath, String cookiePath) {
        String expected = normalizeCookiePath(cookiePath);
        if (expected.equals("/")) return true;
        if (!requestPath.startsWith(expected)) return false;
        return requestPath.length() == expected.length()
                || expected.endsWith("/")
                || requestPath.charAt(expected.length()) == '/';
    }

    private String normalizePath(String path) {
        return path == null || path.isBlank() ? "/" : path;
    }

    private String normalizeCookiePath(String path) {
        return path == null || path.isBlank() || !path.startsWith("/") ? "/" : path;
    }

    private String normalizeCustomHeader(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.regionMatches(true, 0, "Cookie:", 0, "Cookie:".length())) {
            return trimmed.substring("Cookie:".length()).trim();
        }
        return trimmed;
    }

    record RefreshResult(HttpRequest request, boolean refreshed) {
    }
}
