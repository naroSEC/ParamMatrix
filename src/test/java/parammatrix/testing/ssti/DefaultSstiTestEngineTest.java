package parammatrix.testing.ssti;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.Test;
import parammatrix.http.RequestMutator;
import parammatrix.http.RequestSender;
import parammatrix.model.ParameterCandidate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultSstiTestEngineTest {
    @Test
    void detectsOnlyEvaluatedExpectedToken() {
        RequestMutator mutator = mock(RequestMutator.class);
        RequestSender sender = mock(RequestSender.class);
        HttpRequestResponse base = exchange("<p>baseline</p>");
        HttpRequest request = mock(HttpRequest.class);
        HttpRequest mutated = mock(HttpRequest.class);
        HttpRequestResponse evaluated = exchange("<p>PRE221POST</p>");
        when(base.request()).thenReturn(request);
        when(mutator.inject(request, Map.of("name", "PRE{{13*17}}POST"))).thenReturn(mutated);
        when(sender.send(mutated)).thenReturn(evaluated);
        ParameterCandidate candidate = mock(ParameterCandidate.class);
        when(candidate.name()).thenReturn("name");
        SstiPayload payload = new SstiPayload(SstiEngine.JINJA2, "PRE{{13*17}}POST",
                "PRE221POST", "Arithmetic Evaluation");

        SstiTestResult result = new DefaultSstiTestEngine(mutator, sender)
                .execute(base, candidate, payload);

        assertThat(result.status()).isEqualTo(SstiTestStatus.DETECTED);
        assertThat(result.actualResult()).isEqualTo("PRE221POST");
        assertThat(result.evidence()).contains("PRE221POST");
    }

    @Test
    void literalPayloadReflectionIsNotDetection() {
        RequestMutator mutator = mock(RequestMutator.class);
        RequestSender sender = mock(RequestSender.class);
        HttpRequestResponse base = exchange("baseline");
        HttpRequest request = mock(HttpRequest.class);
        HttpRequest mutated = mock(HttpRequest.class);
        HttpRequestResponse reflected = exchange("PRE{{13*17}}POST");
        when(base.request()).thenReturn(request);
        when(mutator.inject(request, Map.of("name", "PRE{{13*17}}POST"))).thenReturn(mutated);
        when(sender.send(mutated)).thenReturn(reflected);
        ParameterCandidate candidate = mock(ParameterCandidate.class);
        when(candidate.name()).thenReturn("name");
        SstiPayload payload = new SstiPayload(SstiEngine.JINJA2, "PRE{{13*17}}POST",
                "PRE221POST", "Arithmetic Evaluation");

        SstiTestResult result = new DefaultSstiTestEngine(mutator, sender)
                .execute(base, candidate, payload);

        assertThat(result.status()).isEqualTo(SstiTestStatus.NOT_DETECTED);
    }

    private HttpRequestResponse exchange(String bodyText) {
        HttpRequestResponse exchange = mock(HttpRequestResponse.class);
        HttpResponse response = mock(HttpResponse.class);
        when(exchange.hasResponse()).thenReturn(true);
        when(exchange.response()).thenReturn(response);
        when(response.bodyToString()).thenReturn(bodyText);
        return exchange;
    }
}
