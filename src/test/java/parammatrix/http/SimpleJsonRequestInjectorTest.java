package parammatrix.http;

import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimpleJsonRequestInjectorTest {
    @Test
    void appendsTopLevelStringFieldsWithoutReformattingExistingFields() {
        HttpRequest request = mock(HttpRequest.class);
        HttpRequest changed = mock(HttpRequest.class);
        when(request.bodyToString()).thenReturn("{\"existing\":42}");
        when(request.withBody(org.mockito.ArgumentMatchers.anyString())).thenReturn(changed);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("keyword", "NARO_\"quoted");

        HttpRequest result = new SimpleJsonRequestInjector().inject(request, values);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(request).withBody(body.capture());
        assertThat(body.getValue()).isEqualTo("{\"existing\":42,\"keyword\":\"NARO_\\\"quoted\"}");
        assertThat(result).isSameAs(changed);
    }
}
