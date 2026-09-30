package parammatrix.testing.database;

import burp.api.montoya.http.message.HttpRequestResponse;
import org.junit.jupiter.api.Test;
import parammatrix.config.ExtensionConfig;
import parammatrix.http.PayloadEncodingMode;
import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterCandidate;
import parammatrix.storage.DatabaseResultRepository;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseTestCoordinatorTest {
    @Test
    void repeatsPositiveResultAndMarksItVerified() {
        DatabaseErrorTestEngine engine = mock(DatabaseErrorTestEngine.class);
        DatabaseStressPayload payload = new DatabaseStressPayload("Single quote", "'");
        DatabaseStressPayloadProvider provider = () -> List.of(payload);
        ParameterCandidate candidate = mock(ParameterCandidate.class);
        HttpRequestResponse base = mock(HttpRequestResponse.class);
        when(candidate.name()).thenReturn("id");
        when(candidate.originalExchange()).thenReturn(base);
        DatabaseTestResult positive = new DatabaseTestResult("id",
                DatabaseTestStatus.DB_ERROR_DETECTED, DatabaseType.MYSQL,
                payload.name(), payload.value(), "%27", "Auto", "SQL syntax",
                DiscoveryConfidence.MEDIUM,
                false, (short) 200, (short) 500, 100, "evidence", base, null);
        when(engine.execute(base, candidate, payload, EnumSet.of(DatabaseType.MYSQL),
                PayloadEncodingMode.AUTO))
                .thenReturn(positive);
        ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.inScopeOnly.set(false);
        DatabaseResultRepository repository = new DatabaseResultRepository();
        DatabaseTestCoordinator coordinator = new DatabaseTestCoordinator(engine, provider,
                repository, extensionConfig);

        List<DatabaseTestResult> results = coordinator.testPage(List.of(candidate),
                new DatabaseRunOptions(EnumSet.of(DatabaseType.MYSQL), false,
                        true, 2, 0, PayloadEncodingMode.AUTO));

        assertThat(results).singleElement().satisfies(result -> assertThat(result.verified()).isTrue());
        verify(engine, times(2)).execute(base, candidate, payload,
                EnumSet.of(DatabaseType.MYSQL), PayloadEncodingMode.AUTO);
        assertThat(repository.all()).hasSize(1);
    }
}
