package parammatrix.scan;

import burp.api.montoya.MontoyaApi;
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
                        "", "", List.of("/static/*")),
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
                .contains("Loading Site Map / Crawl records from Burp",
                        "Reading Site Map / Crawl",
                        "Filtering methods, paths, and duplicates");
        assertThat(progress).anySatisfy(value -> {
            assertThat(value.stage()).isEqualTo("Filtering methods, paths, and duplicates");
            assertThat(value.completed()).isEqualTo(4);
            assertThat(value.total()).isEqualTo(4);
        });
    }

    private HttpRequestResponse exchange(String method, String path) {
        HttpRequestResponse exchange = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);
        HttpResponse response = mock(HttpResponse.class);
        HttpService service = mock(HttpService.class);
        when(exchange.hasResponse()).thenReturn(true);
        when(exchange.request()).thenReturn(request);
        when(exchange.response()).thenReturn(response);
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
