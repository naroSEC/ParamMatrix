package parammatrix.scan;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScanCookieRefresherTest {
    @Test
    void replacesStaleHeaderWithCurrentCookiesMatchingHostAndPath() {
        ZonedDateTime now = ZonedDateTime.now();
        HttpRequest request = request("example.test", "/account/profile", true);
        HttpRequest changed = mock(HttpRequest.class);
        when(request.withUpdatedHeader("Cookie", "scoped=two; session=fresh"))
                .thenReturn(changed);
        HttpRequestResponse exchange = exchange(request);

        ScanCookieRefresher.RefreshResult result = new ScanCookieRefresher().refresh(
                exchange,
                List.of(cookie("session", "fresh", "example.test", "/", Optional.empty()),
                        cookie("scoped", "two", "example.test", "/account", Optional.empty()),
                        cookie("other", "no", "other.test", "/", Optional.empty()),
                        cookie("expired", "no", "example.test", "/",
                                Optional.of(now.minusMinutes(1)))),
                now);

        assertThat(result.refreshed()).isTrue();
        assertThat(result.request()).isSameAs(changed);
        verify(request).withUpdatedHeader("Cookie", "scoped=two; session=fresh");
    }

    @Test
    void preservesOriginalExchangeWhenNoCookieMatches() {
        HttpRequest request = request("example.test", "/private", false);
        HttpRequestResponse exchange = exchange(request);

        ScanCookieRefresher.RefreshResult result = new ScanCookieRefresher().refresh(
                exchange,
                List.of(cookie("session", "foreign", "other.test", "/", Optional.empty())),
                ZonedDateTime.now());

        assertThat(result.refreshed()).isFalse();
        assertThat(result.request()).isSameAs(request);
    }

    @Test
    void dottedCookieDomainAlsoMatchesSubdomains() {
        HttpRequest request = request("app.example.test", "/", false);
        HttpRequest changed = mock(HttpRequest.class);
        when(request.withAddedHeader("Cookie", "session=fresh")).thenReturn(changed);

        ScanCookieRefresher.RefreshResult result = new ScanCookieRefresher().refresh(
                exchange(request),
                List.of(cookie("session", "fresh", ".example.test", "/", Optional.empty())),
                ZonedDateTime.now());

        assertThat(result.refreshed()).isTrue();
        verify(request).withAddedHeader("Cookie", "session=fresh");
    }

    @Test
    void appliesVisibleCustomCookieOnlyToTheExactTargetHost() {
        HttpRequest request = request("app.example.test", "/account", true);
        HttpRequest changed = mock(HttpRequest.class);
        when(request.withUpdatedHeader("Cookie", "session=manual; role=admin"))
                .thenReturn(changed);

        ScanCookieRefresher.RefreshResult result = new ScanCookieRefresher()
                .replaceWithCustom(exchange(request), "app.example.test",
                        "Cookie: session=manual; role=admin");

        assertThat(result.refreshed()).isTrue();
        assertThat(result.request()).isSameAs(changed);
        verify(request).withUpdatedHeader("Cookie", "session=manual; role=admin");
    }

    @Test
    void doesNotSendCustomCookieToAnotherHost() {
        HttpRequest request = request("other.example.test", "/", true);

        ScanCookieRefresher.RefreshResult result = new ScanCookieRefresher()
                .replaceWithCustom(exchange(request), "app.example.test", "session=manual");

        assertThat(result.refreshed()).isFalse();
        assertThat(result.request()).isSameAs(request);
    }

    private HttpRequest request(String host, String path, boolean hasCookie) {
        HttpRequest request = mock(HttpRequest.class);
        HttpService service = mock(HttpService.class);
        when(request.httpService()).thenReturn(service);
        when(request.pathWithoutQuery()).thenReturn(path);
        when(request.hasHeader("Cookie")).thenReturn(hasCookie);
        when(service.host()).thenReturn(host);
        return request;
    }

    private HttpRequestResponse exchange(HttpRequest request) {
        HttpRequestResponse exchange = mock(HttpRequestResponse.class);
        when(exchange.request()).thenReturn(request);
        when(exchange.response()).thenReturn(mock(HttpResponse.class));
        when(exchange.annotations()).thenReturn(mock(Annotations.class));
        return exchange;
    }

    private Cookie cookie(String name, String value, String domain, String path,
                          Optional<ZonedDateTime> expiration) {
        Cookie cookie = mock(Cookie.class);
        when(cookie.name()).thenReturn(name);
        when(cookie.value()).thenReturn(value);
        when(cookie.domain()).thenReturn(domain);
        when(cookie.path()).thenReturn(path);
        when(cookie.expiration()).thenReturn(expiration);
        return cookie;
    }
}
