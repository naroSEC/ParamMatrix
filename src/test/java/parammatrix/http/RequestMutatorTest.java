package parammatrix.http;

import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestMutatorTest {
    private final RequestMutator mutator = new RequestMutator(new SimpleJsonRequestInjector());

    @Test
    void autoEncodesExistingUrlParameterExactlyOnce() {
        HttpRequest original = request("GET", "", "id=10&keep=x%20y", "/view", "");
        HttpRequest changed = mock(HttpRequest.class);
        ParsedHttpParameter existing = parameter("id", HttpParameterType.URL);
        when(original.parameters()).thenReturn(List.of(existing));
        when(original.withPath("/view?id=1%27&keep=x%20y")).thenReturn(changed);

        PayloadMutation result = mutator.injectPayload(original, "id", "1'",
                PayloadEncodingMode.AUTO);

        assertThat(result.request()).isSameAs(changed);
        assertThat(result.wireValue()).isEqualTo("1%27");
        assertThat(result.encodingDescription()).contains("once").contains("URL query");
        verify(original).withPath("/view?id=1%27&keep=x%20y");
    }

    @Test
    void autoEncodesNewSstiUrlParameter() {
        HttpRequest original = request("GET", "", "", "/search", "");
        HttpRequest changed = mock(HttpRequest.class);
        when(original.parameters()).thenReturn(List.of());
        when(original.withPath("/search?template=%7B%7B7*7%7D%7D")).thenReturn(changed);

        PayloadMutation result = mutator.injectPayload(original, "template", "{{7*7}}",
                PayloadEncodingMode.AUTO);

        assertThat(result.request()).isSameAs(changed);
        assertThat(result.logicalValue()).isEqualTo("{{7*7}}");
        assertThat(result.wireValue()).isEqualTo("%7B%7B7*7%7D%7D");
    }

    @Test
    void autoEncodesFormBodyWithoutChangingExistingFields() {
        HttpRequest original = request("POST", "application/x-www-form-urlencoded",
                "", "/search", "csrf=a%2Bb");
        HttpRequest changed = mock(HttpRequest.class);
        when(original.parameters()).thenReturn(List.of());
        when(original.withBody("csrf=a%2Bb&keyword=%22%29")).thenReturn(changed);

        PayloadMutation result = mutator.injectPayload(original, "keyword", "\")",
                PayloadEncodingMode.AUTO);

        assertThat(result.request()).isSameAs(changed);
        assertThat(result.wireValue()).isEqualTo("%22%29");
    }

    @Test
    void rawModeKeepsLiteralUrlValue() {
        HttpRequest original = request("GET", "", "id=10", "/view", "");
        HttpRequest changed = mock(HttpRequest.class);
        ParsedHttpParameter existing = parameter("id", HttpParameterType.URL);
        when(original.parameters()).thenReturn(List.of(existing));
        when(original.withPath("/view?id=1'")).thenReturn(changed);

        PayloadMutation result = mutator.injectPayload(original, "id", "1'",
                PayloadEncodingMode.RAW);

        assertThat(result.request()).isSameAs(changed);
        assertThat(result.wireValue()).isEqualTo("1'");
        assertThat(result.encodingDescription()).contains("Raw value");
    }

    @Test
    void jsonUsesJsonEscapingInsteadOfUrlEncoding() {
        HttpRequest original = request("POST", "application/json", "", "/api", "{}");
        HttpRequest changed = mock(HttpRequest.class);
        when(original.withBody("{\"value\":\"a\\\"b\\\\c\"}")).thenReturn(changed);

        PayloadMutation result = mutator.injectPayload(original, "value", "a\"b\\c",
                PayloadEncodingMode.AUTO);

        assertThat(result.request()).isSameAs(changed);
        assertThat(result.wireValue()).isEqualTo("a\\\"b\\\\c");
        assertThat(result.encodingDescription()).isEqualTo("JSON string escaping");
    }

    private HttpRequest request(String method, String contentType, String query,
                                String pathWithoutQuery, String body) {
        HttpRequest request = mock(HttpRequest.class);
        when(request.method()).thenReturn(method);
        when(request.headerValue("Content-Type")).thenReturn(contentType);
        when(request.query()).thenReturn(query);
        when(request.pathWithoutQuery()).thenReturn(pathWithoutQuery);
        when(request.bodyToString()).thenReturn(body);
        return request;
    }

    private ParsedHttpParameter parameter(String name, HttpParameterType type) {
        ParsedHttpParameter parameter = mock(ParsedHttpParameter.class);
        when(parameter.name()).thenReturn(name);
        when(parameter.type()).thenReturn(type);
        return parameter;
    }
}
