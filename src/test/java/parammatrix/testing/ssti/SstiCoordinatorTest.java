package parammatrix.testing.ssti;

import burp.api.montoya.http.message.HttpRequestResponse;
import org.junit.jupiter.api.Test;
import parammatrix.config.ExtensionConfig;
import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterCandidate;
import parammatrix.storage.SstiResultRepository;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SstiCoordinatorTest {
    @Test
    void enforcesIndependentPerPageRequestBudget() {
        SstiTestEngine engine = mock(SstiTestEngine.class);
        ParameterCandidate candidate = mock(ParameterCandidate.class);
        HttpRequestResponse base = mock(HttpRequestResponse.class);
        when(candidate.name()).thenReturn("name");
        when(candidate.originalExchange()).thenReturn(base);
        SstiPayload first = new SstiPayload(SstiEngine.GENERIC, "a", "A", "Arithmetic Evaluation");
        SstiPayload second = new SstiPayload(SstiEngine.GENERIC, "b", "B", "Arithmetic Evaluation");
        SstiPayloadProvider provider = new SstiPayloadProvider() {
            @Override public SstiEngine engine() { return SstiEngine.GENERIC; }
            @Override public List<SstiPayload> payloads() { return List.of(first, second); }
        };
        SstiTestResult executed = new SstiTestResult("name", SstiEngine.GENERIC,
                SstiTestStatus.NOT_DETECTED, "a", "A", "Not observed",
                "Arithmetic Evaluation", DiscoveryConfidence.LOW, "none", base, null);
        when(engine.execute(base, candidate, first)).thenReturn(executed);
        ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.inScopeOnly.set(false);
        SstiResultRepository repository = new SstiResultRepository();
        SstiCoordinator coordinator = new SstiCoordinator(engine, List.of(provider), repository,
                extensionConfig);

        List<SstiTestResult> results = coordinator.testPage(List.of(candidate),
                new SstiRunOptions(Set.of(SstiEngine.GENERIC), false, 1, 0));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(SstiTestResult::status)
                .containsExactly(SstiTestStatus.NOT_DETECTED, SstiTestStatus.SKIPPED);
        verify(engine, times(1)).execute(any(), any(), any());
        assertThat(repository.all()).hasSize(2);
    }
}
