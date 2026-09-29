package parammatrix.discovery;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.Test;
import parammatrix.config.ExtensionConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentTypeClassifierTest {
    private final ContentTypeClassifier classifier = new ContentTypeClassifier();

    @Test
    void acceptsHtmlContentType() {
        HttpResponse response = response("text/html; charset=UTF-8", "<html><body>ok</body></html>");
        assertThat(classifier.shouldAnalyze(response, new ExtensionConfig())).isTrue();
    }

    @Test
    void rejectsJsonEvenWhenAutoDetectionIsEnabled() {
        ExtensionConfig config = new ExtensionConfig();
        config.htmlAutoDetection.set(true);
        HttpResponse response = response("application/json",
                "{\"html\":\"<html><script>alert(1)</script></html>\"}");
        assertThat(classifier.shouldAnalyze(response, config)).isFalse();
    }

    private HttpResponse response(String contentType, String bodyText) {
        HttpResponse response = mock(HttpResponse.class);
        ByteArray body = mock(ByteArray.class);
        when(body.length()).thenReturn(bodyText.length());
        when(response.body()).thenReturn(body);
        when(response.bodyToString()).thenReturn(bodyText);
        when(response.headerValue("Content-Type")).thenReturn(contentType);
        return response;
    }
}
