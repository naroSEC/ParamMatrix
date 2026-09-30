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
        return collect(options, new ScanProgressListener() { });
    }

    public ScanBatch collect(ScanOptions options, ScanProgressListener listener) {
        List<HttpRequestResponse> source = new ArrayList<>();
        int withoutResponse = 0;

        if (options.proxyHistory()) {
            listener.collectionProgress(new ScanCollectionProgress(
                    "Loading Proxy History from Burp", 0, 0));
            List<ProxyHttpRequestResponse> history = api.proxy().history();
            reportCollection(listener, "Reading Proxy History", 0, history.size());
            int read = 0;
            for (ProxyHttpRequestResponse item : history) {
                if (!item.hasResponse() || item.response() == null) {
                    withoutResponse++;
                } else {
                    source.add(HttpRequestResponse.httpRequestResponse(
                            item.finalRequest(), item.response()));
                }
                reportCollection(listener, "Reading Proxy History", ++read, history.size());
            }
        }
        if (options.siteMap()) {
            listener.collectionProgress(new ScanCollectionProgress(
                    "Loading Site Map / Crawl records from Burp", 0, 0));
            List<HttpRequestResponse> siteMap = api.siteMap().requestResponses();
            reportCollection(listener, "Reading Site Map / Crawl", 0, siteMap.size());
            int read = 0;
            for (HttpRequestResponse item : siteMap) {
                if (!item.hasResponse() || item.response() == null) {
                    withoutResponse++;
                } else {
                    source.add(item);
                }
                reportCollection(listener, "Reading Site Map / Crawl", ++read, siteMap.size());
            }
        }

        PathExclusionMatcher exclusions = new PathExclusionMatcher(options.excludedPathRules());
        Map<PageIdentity, HttpRequestResponse> eligible = new LinkedHashMap<>();
        int excludedByMethod = 0;
        int excludedByPath = 0;
        int duplicates = 0;
        reportCollection(listener, "Filtering methods, paths, and duplicates", 0, source.size());
        int filtered = 0;
        for (HttpRequestResponse item : source) {
            String method = item.request().method().toUpperCase(Locale.ROOT);
            if ((method.equals("GET") && !options.get())
                    || (method.equals("POST") && !options.post())
                    || (!method.equals("GET") && !method.equals("POST"))) {
                excludedByMethod++;
                reportCollection(listener, "Filtering methods, paths, and duplicates",
                        ++filtered, source.size());
                continue;
            }
            if (exclusions.excludes(item.request().pathWithoutQuery())) {
                excludedByPath++;
                reportCollection(listener, "Filtering methods, paths, and duplicates",
                        ++filtered, source.size());
                continue;
            }
            PageIdentity identity = PageIdentity.from(item.request(),
                    config.identityIgnoresParameterValues.get());
            if (eligible.putIfAbsent(identity, item) != null) duplicates++;
            reportCollection(listener, "Filtering methods, paths, and duplicates",
                    ++filtered, source.size());
        }
        List<HttpRequestResponse> exchanges = List.copyOf(eligible.values());
        int cookieHeadersUpdated = 0;
        if (options.cookieMode() != ScanCookieMode.KEEP_RECORDED && !exchanges.isEmpty()) {
            List<Cookie> cookies = options.cookieMode() == ScanCookieMode.BURP_COOKIE_JAR
                    ? api.http().cookieJar().cookies() : List.of();
            ScanCookieRefresher refresher = new ScanCookieRefresher();
            List<HttpRequestResponse> refreshed = new ArrayList<>(exchanges.size());
            ZonedDateTime now = ZonedDateTime.now();
            String cookieStage = options.cookieMode() == ScanCookieMode.BURP_COOKIE_JAR
                    ? "Refreshing cookies from Burp Cookie Jar"
                    : "Applying custom Cookie header";
            reportCollection(listener, cookieStage, 0, exchanges.size());
            int processed = 0;
            for (HttpRequestResponse exchange : exchanges) {
                ScanCookieRefresher.RefreshResult result = switch (options.cookieMode()) {
                    case BURP_COOKIE_JAR -> refresher.refresh(exchange, cookies, now);
                    case CUSTOM_HEADER -> refresher.replaceWithCustom(exchange,
                            options.customCookieHost(), options.customCookieHeader());
                    case KEEP_RECORDED -> throw new IllegalStateException("Unexpected cookie mode");
                };
                if (result.refreshed()) {
                    refreshed.add(HttpRequestResponse.httpRequestResponse(
                            result.request(), exchange.response(), exchange.annotations()));
                    cookieHeadersUpdated++;
                } else {
                    refreshed.add(exchange);
                }
                reportCollection(listener, cookieStage, ++processed, exchanges.size());
            }
            exchanges = List.copyOf(refreshed);
        }
        ScanSummary summary = new ScanSummary(source.size() + withoutResponse, exchanges.size(),
                excludedByMethod, excludedByPath, duplicates, withoutResponse,
                cookieHeadersUpdated);
        return new ScanBatch(exchanges, summary);
    }

    private void reportCollection(ScanProgressListener listener, String stage,
                                  int completed, int total) {
        if (total == 0) {
            listener.collectionProgress(new ScanCollectionProgress(stage + " (no records)", 1, 1));
            return;
        }
        if (completed == 0 || completed == 1 || completed == total || completed % 50 == 0) {
            listener.collectionProgress(new ScanCollectionProgress(stage, completed, total));
        }
    }
}
