package parammatrix.testing.ssti;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SstiPayloadProviderTest {
    @Test
    void everyProviderCreatesSafeArithmeticPayloadWithUniqueExpectedToken() {
        List<SstiPayloadProvider> providers = List.of(
                new GenericPayloadProvider(), new Jinja2PayloadProvider(),
                new TwigPayloadProvider(), new FreeMarkerPayloadProvider(),
                new VelocityPayloadProvider(), new ThymeleafPayloadProvider(),
                new SmartyPayloadProvider());

        for (SstiPayloadProvider provider : providers) {
            assertThat(provider.payloads()).isNotEmpty().allSatisfy(payload -> {
                assertThat(payload.engine()).isEqualTo(provider.engine());
                assertThat(payload.payload()).contains("PMX_");
                assertThat(payload.expectedResult()).startsWith("PMX_").endsWith("_END");
                assertThat(payload.payload()).isNotEqualTo(payload.expectedResult());
                assertThat(payload.detectionMethod()).isEqualTo("Arithmetic Evaluation");
            });
        }
    }

    @Test
    void repeatedPayloadGenerationUsesDifferentMarkers() {
        SstiPayloadProvider provider = new Jinja2PayloadProvider();
        assertThat(provider.payloads().getFirst().expectedResult())
                .isNotEqualTo(provider.payloads().getFirst().expectedResult());
    }

    @Test
    void previewCatalogMatchesProviderPayloadCounts() {
        List<SstiPayloadProvider> providers = List.of(
                new GenericPayloadProvider(), new Jinja2PayloadProvider(),
                new TwigPayloadProvider(), new FreeMarkerPayloadProvider(),
                new VelocityPayloadProvider(), new ThymeleafPayloadProvider(),
                new SmartyPayloadProvider());

        for (SstiPayloadProvider provider : providers) {
            assertThat(SstiPayloadPatternCatalog.patternsFor(provider.engine()))
                    .hasSameSizeAs(provider.payloads());
        }
    }
}

