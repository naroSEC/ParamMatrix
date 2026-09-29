package parammatrix.scan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PathExclusionMatcherTest {
    @Test
    void plainRuleExcludesPathAndDescendants() {
        PathExclusionMatcher matcher = new PathExclusionMatcher(List.of("/logout", "/admin"));
        assertThat(matcher.excludes("/logout")).isTrue();
        assertThat(matcher.excludes("/admin/users")).isTrue();
        assertThat(matcher.excludes("/administrator")).isFalse();
    }

    @Test
    void supportsGlobAndRegexRules() {
        PathExclusionMatcher matcher = new PathExclusionMatcher(
                List.of("/static/*", "regex:^/api/v[0-9]+/health$"));
        assertThat(matcher.excludes("/static/app.js?cache=1")).isTrue();
        assertThat(matcher.excludes("/api/v2/health")).isTrue();
        assertThat(matcher.excludes("/api/v2/users")).isFalse();
    }
}
