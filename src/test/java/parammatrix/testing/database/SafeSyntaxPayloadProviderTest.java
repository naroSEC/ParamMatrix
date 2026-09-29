package parammatrix.testing.database;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafeSyntaxPayloadProviderTest {
    @Test
    void providesOnlyShortSyntaxStressStrings() {
        var payloads = new SafeSyntaxPayloadProvider().payloads();
        assertThat(payloads).hasSize(6).allSatisfy(payload -> {
            assertThat(payload.value()).hasSizeLessThanOrEqualTo(3);
            assertThat(payload.value().toLowerCase())
                    .doesNotContain("select", "insert", "update", "delete", "drop", "sleep", "waitfor");
        });
    }
}

