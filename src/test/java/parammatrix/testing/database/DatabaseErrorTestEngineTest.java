package parammatrix.testing.database;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.Test;
import parammatrix.analysis.DatabaseErrorSignatureAnalyzer;
import parammatrix.http.PayloadEncodingMode;
import parammatrix.http.PayloadMutation;
import parammatrix.http.RequestMutator;
import parammatrix.http.RequestSender;
import parammatrix.model.ParameterCandidate;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseErrorTestEngineTest {
    @Test
    void detectsNewDatabaseErrorSignature() {
        RequestMutator mutator = mock(RequestMutator.class);
        RequestSender sender = mock(RequestSender.class);
        HttpRequestResponse base = exchange((short) 200, "normal page");
        HttpRequestResponse tested = exchange((short) 500,
                "You have an error in your SQL syntax near quote");
        HttpRequest original = mock(HttpRequest.class);
        HttpRequest changed = mock(HttpRequest.class);
        when(base.request()).thenReturn(original);
        when(mutator.injectPayload(original, "id", "'", PayloadEncodingMode.AUTO))
                .thenReturn(new PayloadMutation(changed, "'", "%27",
                        "UTF-8 form percent encoding (once, URL query)"));
        when(sender.send(changed)).thenReturn(tested);
        ParameterCandidate candidate = mock(ParameterCandidate.class);
        when(candidate.name()).thenReturn("id");

        DatabaseTestResult result = new DatabaseErrorTestEngine(mutator, sender,
                new DatabaseErrorSignatureAnalyzer()).execute(base, candidate,
                new DatabaseStressPayload("Single quote", "'"),
                EnumSet.allOf(DatabaseType.class), PayloadEncodingMode.AUTO);

        assertThat(result.status()).isEqualTo(DatabaseTestStatus.DB_ERROR_DETECTED);
        assertThat(result.suspectedDatabase()).isEqualTo(DatabaseType.MYSQL);
        assertThat(result.errorSignature()).containsIgnoringCase("SQL syntax");
        assertThat(result.wirePayload()).isEqualTo("%27");
    }

    @Test
    void classifiesServerFailureWithoutSignatureAsBehaviorChange() {
        RequestMutator mutator = mock(RequestMutator.class);
        RequestSender sender = mock(RequestSender.class);
        HttpRequestResponse base = exchange((short) 200, "normal page");
        HttpRequestResponse tested = exchange((short) 500, "internal error");
        HttpRequest original = mock(HttpRequest.class);
        HttpRequest changed = mock(HttpRequest.class);
        when(base.request()).thenReturn(original);
        when(mutator.injectPayload(original, "id", "'", PayloadEncodingMode.AUTO))
                .thenReturn(new PayloadMutation(changed, "'", "%27",
                        "UTF-8 form percent encoding (once, URL query)"));
        when(sender.send(changed)).thenReturn(tested);
        ParameterCandidate candidate = mock(ParameterCandidate.class);
        when(candidate.name()).thenReturn("id");

        DatabaseTestResult result = new DatabaseErrorTestEngine(mutator, sender,
                new DatabaseErrorSignatureAnalyzer()).execute(base, candidate,
                new DatabaseStressPayload("Single quote", "'"),
                EnumSet.allOf(DatabaseType.class), PayloadEncodingMode.AUTO);

        assertThat(result.status()).isEqualTo(DatabaseTestStatus.BEHAVIOR_CHANGED);
        assertThat(result.confidence()).isEqualTo(parammatrix.model.DiscoveryConfidence.LOW);
    }

    private HttpRequestResponse exchange(short status, String body) {
        HttpRequestResponse exchange = mock(HttpRequestResponse.class);
        HttpResponse response = mock(HttpResponse.class);
        when(exchange.hasResponse()).thenReturn(true);
        when(exchange.response()).thenReturn(response);
        when(response.statusCode()).thenReturn(status);
        when(response.bodyToString()).thenReturn(body);
        return exchange;
    }
}

