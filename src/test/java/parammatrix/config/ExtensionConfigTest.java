package parammatrix.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionConfigTest {
    @Test
    void usesFiveConcurrentActiveTestWorkersByDefault() {
        assertThat(new ExtensionConfig().concurrentActiveTests).hasValue(5);
    }
}
