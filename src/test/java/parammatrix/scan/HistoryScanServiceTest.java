package parammatrix.scan;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.sitemap.SiteMap;
import org.junit.jupiter.api.Test;
import parammatrix.config.ExtensionConfig;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoryScanServiceTest {
    @Test
    void filtersMethodsPathsAndDuplicatePageIdentities() {
        MontoyaApi api = mock(MontoyaApi.class);
        SiteMap siteMap = mock(SiteMap.class);
        when(api.siteMap()).thenReturn(siteMap);
        List<HttpRequestResponse> records = List.of(
                exchange("GET", "/products"),
                exchange("GET", "/products"),
                exchange("POST", "/search"),
                exchange("GET", "/static/app.js"));
        when(siteMap.requestResponses()).thenReturn(records);

        HistoryScanService service = new HistoryScanService(api, new ExtensionConfig());
        List<ScanCollectionProgress> progress = new ArrayList<>();
        ScanBatch batch = service.collect(new ScanOptions(false, true, true, false,
                        true, false, false, ScanCookieMode.KEEP_RECORDED,
                        "", "", 500, List.of("/static/*")),
                new ScanProgressListener() {
                    @Override public void collectionProgress(ScanCollectionProgress value) {
                        progress.add(value);
                    }
                });

        assertThat(batch.exchanges()).hasSize(1);
        assertThat(batch.summary().sourceRecords()).isEqualTo(4);
        assertThat(batch.summary().excludedByMethod()).isEqualTo(1);
        assertThat(batch.summary().excludedByPath()).isEqualTo(1);
        assertThat(batch.summary().duplicates()).isEqualTo(1);
        assertThat(batch.summary().cookieHeadersUpdated()).isZero();
        assertThat(progress).extracting(ScanCollectionProgress::stage)
                .contains("Loading Site Map / Crawl index from Burp",
                        "Filtering Site Map / Crawl and loading eligible responses");
        assertThat(progress).anySatisfy(value -> {
            assertThat(value.stage()).isEqualTo(
                    "Filtering Site Map / Crawl and loading eligible responses");
            assertThat(value.completed()).isEqualTo(4);
            assertThat(value.total()).isEqualTo(4);
        });
        verify(records.getFirst(), never()).hasResponse();
    }

    @Test
    void pageLimitStopsBeforeAdditionalResponsesAreMaterialized() {
        MontoyaApi api = mock(MontoyaApi.class);
        SiteMap siteMap = mock(SiteMap.class);
        when(api.siteMap()).thenReturn(siteMap);
        HttpRequestResponse first = exchange("GET", "/one");
        HttpRequestResponse second = exchange("GET", "/two");
        when(siteMap.requestResponses()).thenReturn(List.of(first, second));

        HistoryScanService service = new HistoryScanService(api, new ExtensionConfig());
        ScanBatch batch = service.collect(new ScanOptions(false, true, true, false,
                true, false, false, ScanCookieMode.KEEP_RECORDED,
                "", "", 1, List.of()));

        assertThat(batch.exchanges()).hasSize(1);
        assertThat(batch.summary().skippedByPageLimit()).isEqualTo(1);
        verify(second, never()).response();
        verify(first, never()).hasResponse();
        verify(second, never()).hasResponse();
    }

    @Test
    void oversizedResponseIsDiscardedBeforeItIsRetained() {
        MontoyaApi api = mock(MontoyaApi.class);
        SiteMap siteMap = mock(SiteMap.class);
        when(api.siteMap()).thenReturn(siteMap);
        HttpRequestResponse oversized = exchange("GET", "/large", 3_000_000);
        when(siteMap.requestResponses()).thenReturn(List.of(oversized));

        HistoryScanService service = new HistoryScanService(api, new ExtensionConfig());
        ScanBatch batch = service.collect(new ScanOptions(false, true, true, false,
                true, false, false, ScanCookieMode.KEEP_RECORDED,
                "", "", 500, List.of()));

        assertThat(batch.exchanges()).isEmpty();
        assertThat(batch.summary().oversizedResponses()).isEqualTo(1);
        verify(oversized, never()).copyToTempFile();
        verify(oversized, never()).hasResponse();
    }

    @Test
    void cancellationStopsBeforeBurpSourceIsLoaded() {
        MontoyaApi api = mock(MontoyaApi.class);
        SiteMap siteMap = mock(SiteMap.class);
        when(api.siteMap()).thenReturn(siteMap);
        HistoryScanService service = new HistoryScanService(api, new ExtensionConfig());

        ScanBatch batch = service.collect(new ScanOptions(false, true, true, false,
                        true, false, false, ScanCookieMode.KEEP_RECORDED,
                        "", "", 500, List.of()),
                new ScanProgressListener() { }, () -> true);

        assertThat(batch.exchanges()).isEmpty();
        verify(siteMap, never()).requestResponses();
    }

    private HttpRequestResponse exchange(String method, String path) {
        return exchange(method, path, 100);
    }

    private HttpRequestResponse exchange(String method, String path, int bodyLength) {
        HttpRequestResponse exchange = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);
        ByteArray body = mock(ByteArray.class);
        HttpService service = mock(HttpService.class);
        when(exchange.hasResponse()).thenReturn(true);
        when(exchange.request()).thenReturn(request);
        when(exchange.response()).thenReturn(response);
        when(exchange.copyToTempFile()).thenReturn(exchange);
        when(response.body()).thenReturn(body);
        when(body.length()).thenReturn(bodyLength);
        when(request.method()).thenReturn(method);
        when(request.pathWithoutQuery()).thenReturn(path);
        when(request.parameters()).thenReturn(List.of());
        when(request.httpService()).thenReturn(service);
        when(service.secure()).thenReturn(true);
        when(service.host()).thenReturn("example.test");
        when(service.port()).thenReturn(443);
        return exchange;
    }
}
