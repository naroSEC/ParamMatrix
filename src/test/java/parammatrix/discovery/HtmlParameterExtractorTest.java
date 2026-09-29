package parammatrix.discovery;

import org.junit.jupiter.api.Test;
import parammatrix.config.ExtensionConfig;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlParameterExtractorTest {
    @Test
    void extractsNamedFieldsAndUrlQueries() {
        String html = """
                <form action="/search?mode=full"><input name="keyword">
                <select name="category"></select></form>
                <a href="/board/view?idx=10&type=notice">view</a>
                """;
        var result = new HtmlParameterExtractor().extract(html,
                "https://example.test/main", new ExtensionConfig());

        assertThat(result.parameters().stream().map(DiscoveredParameter::name))
                .containsExactlyInAnyOrder("keyword", "category", "mode", "idx", "type");
    }
}

