package parammatrix.analysis;

import org.junit.jupiter.api.Test;
import parammatrix.model.ReflectionContext;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionContextAnalyzerTest {
    private final ReflectionContextAnalyzer analyzer = new ReflectionContextAnalyzer();

    @Test void findsHtmlText() {
        assertThat(analyzer.analyze("<p>NARO_x</p>", "NARO_x"))
                .isEqualTo(ReflectionContext.HTML_TEXT);
    }

    @Test void findsDoubleQuotedAttribute() {
        assertThat(analyzer.analyze("<input value=\"NARO_x\">", "NARO_x"))
                .isEqualTo(ReflectionContext.HTML_ATTRIBUTE_DOUBLE_QUOTED);
    }

    @Test void findsJavaScriptString() {
        assertThat(analyzer.analyze("<script>let x='NARO_x';</script>", "NARO_x"))
                .isEqualTo(ReflectionContext.JAVASCRIPT_SINGLE_QUOTED_STRING);
    }
}

