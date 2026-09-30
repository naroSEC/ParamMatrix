package parammatrix.scan;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import parammatrix.config.ExtensionConfig;
import parammatrix.model.PageIdentity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.ZonedDateTime;

public final class HistoryScanService {
    private final MontoyaApi api;
    private final ExtensionConfig config;

    public HistoryScanService(MontoyaApi api, ExtensionConfig config) {
        this.api = api;
        this.config = config;
    }

    public ScanBatch collect(ScanOptions options) {
        List<HttpRequestResponse> source = new ArrayList<>();
        int withoutResponse = 0;

        if (options.proxyHistory()) {
            for (ProxyHttpRequestResponse item : api.proxy().history()) {
                if (!item.hasResponse() || item.response() == null) {
                    withoutResponse++;
                    continue;
                }
                source.add(HttpRequestResponse.httpRequestResponse(item.finalRequest(), item.response()));
            }
        }
        if (options.siteMap()) {
            for (HttpRequestResponse item : api.siteMap().requestResponses()) {
                if (!item.hasResponse() || item.response() == null) {
                    withoutResponse++;
                    continue;
                }
                source.add(item);
            }
        }

        PathExclusionMatcher exclusions = new PathExclusionMatcher(options.excludedPathRules());
        Map<PageIdentity, HttpRequestResponse> eligible = new LinkedHashMap<>();
        int excludedByMethod = 0;
        int excludedByPath = 0;
        int duplicates = 0;
        for (HttpRequestResponse item : source) {
            String method = item.request().method().toUpperCase(Locale.ROOT);
            if ((method.equals("GET") && !options.get())
                    || (method.equals("POST") && !options.post())
                    || (!method.equals("GET") && !method.equals("POST"))) {
                excludedByMethod++;
                continue;
            }
            if (exclusions.excludes(item.request().pathWithoutQuery())) {
                excludedByPath++;
                continue;
            }
            PageIdentity identity = PageIdentity.from(item.request(),
                    config.identityIgnoresParameterValues.get());
            if (eligible.putIfAbsent(identity, item) != null) duplicates++;
        }
        List<HttpRequestResponse> exchanges = List.copyOf(eligible.values());
        int cookiesRefreshed = 0;
        if (options.refreshCookiesFromJar() && !exchanges.isEmpty()) {
            List<Cookie> cookies = api.http().cookieJar().cookies();
            ScanCookieRefresher refresher = new ScanCookieRefresher();
            List<HttpRequestResponse> refreshed = new ArrayList<>(exchanges.size());
            ZonedDateTime now = ZonedDateTime.now();
            for (HttpRequestResponse exchange : exchanges) {
                ScanCookieRefresher.RefreshResult result = refresher.refresh(exchange, cookies, now);
                if (result.refreshed()) {
                    refreshed.add(HttpRequestResponse.httpRequestResponse(
                            result.request(), exchange.response(), exchange.annotations()));
                    cookiesRefreshed++;
                } else {
                    refreshed.add(exchange);
                }
            }
            exchanges = List.copyOf(refreshed);
        }
        ScanSummary summary = new ScanSummary(source.size() + withoutResponse, exchanges.size(),
                excludedByMethod, excludedByPath, duplicates, withoutResponse, cookiesRefreshed);
        return new ScanBatch(exchanges, summary);
    }
}
