package parammatrix.discovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicJavaScriptParserTest {
    private final HeuristicJavaScriptParser parser = new HeuristicJavaScriptParser();

    @Test
    void extractsOnlyValuesUsedByHttpSinks() {
        String script = """
                let count = 0;
                let button = document.querySelector('button');
                fetch('/api?id=' + userId);
                $.ajax({url:'/search', data:{keyword: keyword, searchType: type}});
                """;

        assertThat(parser.extract(script, "https://example.test/")
                .stream().map(DiscoveredParameter::name))
                .contains("id", "keyword", "searchType")
                .doesNotContain("count", "button", "result", "userId");
    }

    @Test
    void extractsFormDataAndUrlSearchParams() {
        String script = """
                const form = new FormData(); form.append('upload', file);
                const query = new URLSearchParams({page: 1, sort: order});
                query.set('filter', value);
                """;
        assertThat(parser.extract(script, "https://example.test/")
                .stream().map(DiscoveredParameter::name))
                .contains("upload", "page", "sort", "filter");
    }
}

