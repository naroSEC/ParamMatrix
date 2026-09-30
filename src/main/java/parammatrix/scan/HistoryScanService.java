package parammatrix.scan;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import parammatrix.config.ExtensionConfig;
import parammatrix.model.PageIdentity;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class HistoryScanService {
    private final MontoyaApi api;
    private final ExtensionConfig config;

    public HistoryScanService(MontoyaApi api, ExtensionConfig config) {
        this.api = api;
        this.config = config;
    }

    public ScanBatch collect(ScanOptions options) {
        return collect(options, new ScanProgressListener() { }, () -> false);
    }

    public ScanBatch collect(ScanOptions options, ScanProgressListener listener) {
        return collect(options, listener, () -> false);
    }

    public ScanBatch collect(ScanOptions options, ScanProgressListener listener,
                             BooleanSupplier cancelled) {
        PathExclusionMatcher exclusions = new PathExclusionMatcher(options.excludedPathRules());
        Map<PageIdentity, HttpRequestResponse> eligible = new LinkedHashMap<>();
        Counters counters = new Counters();
        CookieContext cookies = cookieContext(options);

        if (options.proxyHistory() && !cancelled.getAsBoolean()) {
            collectProxy(options, listener, cancelled, exclusions, eligible, counters, cookies);
        }
        if (options.siteMap() && !cancelled.getAsBoolean()
                && eligible.size() < options.maximumPages()) {
            collectSiteMap(options, listener, cancelled, exclusions, eligible, counters, cookies);
        } else if (options.siteMap() && eligible.size() >= options.maximumPages()) {
            counters.skippedByPageLimit++;
            listener.collectionProgress(new ScanCollectionProgress(
                    "Maximum page limit reached; Site Map collection skipped", 1, 1));
        }

        List<HttpRequestResponse> exchanges = List.copyOf(eligible.values());
        ScanSummary summary = new ScanSummary(counters.sourceRecords, exchanges.size(),
                counters.excludedByMethod, counters.excludedByPath, counters.duplicates,
                counters.withoutResponse, counters.cookieHeadersUpdated,
                counters.oversizedResponses, counters.skippedByPageLimit);
        return new ScanBatch(exchanges, summary);
    }

    private void collectProxy(ScanOptions options, ScanProgressListener listener,
                              BooleanSupplier cancelled, PathExclusionMatcher exclusions,
                              Map<PageIdentity, HttpRequestResponse> eligible,
                              Counters counters, CookieContext cookies) {
        listener.collectionProgress(new ScanCollectionProgress(
                "Loading Proxy History index from Burp", 0, 0));
        List<ProxyHttpRequestResponse> history = api.proxy().history();
        counters.sourceRecords += history.size();
        String stage = "Filtering Proxy History and loading eligible responses";
        reportCollection(listener, stage, 0, history.size());
        int processed = 0;
        for (ProxyHttpRequestResponse item : history) {
            if (cancelled.getAsBoolean()) break;
            if (eligible.size() >= options.maximumPages()) {
                counters.skippedByPageLimit += history.size() - processed;
                reportCollection(listener, stage, history.size(), history.size());
                break;
            }
            processProxyItem(item, options, exclusions, eligible, counters, cookies);
            reportCollection(listener, stage, ++processed, history.size());
        }
    }

    private void processProxyItem(ProxyHttpRequestResponse item, ScanOptions options,
                                  PathExclusionMatcher exclusions,
                                  Map<PageIdentity, HttpRequestResponse> eligible,
                                  Counters counters, CookieContext cookies) {
        HttpRequest request = item.finalRequest();
        String method = request.method().toUpperCase(Locale.ROOT);
        if (!methodAllowed(method, options)) {
            counters.excludedByMethod++;
            return;
        }
        if (exclusions.excludes(request.pathWithoutQuery())) {
            counters.excludedByPath++;
            return;
        }

        PageIdentity identity = PageIdentity.from(request,
                config.identityIgnoresParameterValues.get());
        if (eligible.containsKey(identity)) {
            counters.duplicates++;
            return;
        }

        HttpResponse response = item.response();
        if (response == null) {
            counters.withoutResponse++;
            return;
        }
        if (response.body().length() > config.maximumResponseBytes.get()) {
            counters.oversizedResponses++;
            return;
        }
        HttpRequest prepared = prepareCookies(request, cookies, counters);
        HttpRequestResponse exchange = HttpRequestResponse.httpRequestResponse(prepared, response)
                .copyToTempFile();
        eligible.put(identity, exchange);
    }

    private void collectSiteMap(ScanOptions options, ScanProgressListener listener,
                                BooleanSupplier cancelled, PathExclusionMatcher exclusions,
                                Map<PageIdentity, HttpRequestResponse> eligible,
                                Counters counters, CookieContext cookies) {
        listener.collectionProgress(new ScanCollectionProgress(
                "Loading Site Map / Crawl index from Burp", 0, 0));
        List<HttpRequestResponse> siteMap = api.siteMap().requestResponses();
        counters.sourceRecords += siteMap.size();
        String stage = "Filtering Site Map / Crawl and loading eligible responses";
        reportCollection(listener, stage, 0, siteMap.size());
        int processed = 0;
        for (HttpRequestResponse item : siteMap) {
            if (cancelled.getAsBoolean()) break;
            if (eligible.size() >= options.maximumPages()) {
                counters.skippedByPageLimit += siteMap.size() - processed;
                reportCollection(listener, stage, siteMap.size(), siteMap.size());
                break;
            }
            processSiteMapItem(item, options, exclusions, eligible, counters, cookies);
            reportCollection(listener, stage, ++processed, siteMap.size());
        }
    }

    private void processSiteMapItem(HttpRequestResponse item, ScanOptions options,
                                    PathExclusionMatcher exclusions,
                                    Map<PageIdentity, HttpRequestResponse> eligible,
                                    Counters counters, CookieContext cookies) {
        HttpRequest request = item.request();
        String method = request.method().toUpperCase(Locale.ROOT);
        if (!methodAllowed(method, options)) {
            counters.excludedByMethod++;
            return;
        }
        if (exclusions.excludes(request.pathWithoutQuery())) {
            counters.excludedByPath++;
            return;
        }
        PageIdentity identity = PageIdentity.from(request,
                config.identityIgnoresParameterValues.get());
        if (eligible.containsKey(identity)) {
            counters.duplicates++;
            return;
        }

        HttpResponse response = item.response();
        if (response == null) {
            counters.withoutResponse++;
            return;
        }
        if (response.body().length() > config.maximumResponseBytes.get()) {
            counters.oversizedResponses++;
            return;
        }
        HttpRequest prepared = prepareCookies(request, cookies, counters);
        HttpRequestResponse exchange = prepared == request
                ? item.copyToTempFile()
                : HttpRequestResponse.httpRequestResponse(
                        prepared, response, item.annotations()).copyToTempFile();
        eligible.put(identity, exchange);
    }

    private HttpRequest prepareCookies(HttpRequest request, CookieContext context,
                                       Counters counters) {
        ScanCookieRefresher.RefreshResult result = switch (context.mode) {
            case KEEP_RECORDED -> new ScanCookieRefresher.RefreshResult(request, false);
            case BURP_COOKIE_JAR -> context.refresher.refresh(request, context.cookies, context.now);
            case CUSTOM_HEADER -> context.refresher.replaceWithCustom(
                    request, context.customHost, context.customHeader);
        };
        if (result.refreshed()) counters.cookieHeadersUpdated++;
        return result.request();
    }

    private CookieContext cookieContext(ScanOptions options) {
        List<Cookie> cookies = options.cookieMode() == ScanCookieMode.BURP_COOKIE_JAR
                ? api.http().cookieJar().cookies() : List.of();
        return new CookieContext(options.cookieMode(), cookies,
                options.customCookieHost(), options.customCookieHeader(),
                new ScanCookieRefresher(), ZonedDateTime.now());
    }

    private boolean methodAllowed(String method, ScanOptions options) {
        return (method.equals("GET") && options.get())
                || (method.equals("POST") && options.post());
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

    private record CookieContext(ScanCookieMode mode, List<Cookie> cookies,
                                 String customHost, String customHeader,
                                 ScanCookieRefresher refresher, ZonedDateTime now) {
    }

    private static final class Counters {
        private int sourceRecords;
        private int excludedByMethod;
        private int excludedByPath;
        private int duplicates;
        private int withoutResponse;
        private int cookieHeadersUpdated;
        private int oversizedResponses;
        private int skippedByPageLimit;
    }
}
