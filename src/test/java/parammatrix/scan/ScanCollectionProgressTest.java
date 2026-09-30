package parammatrix.scan;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScanCollectionProgressTest {
    @Test
    void zeroTotalRepresentsAnIndeterminateBurpLoadingStage() {
        ScanCollectionProgress progress = new ScanCollectionProgress(
                "Loading Proxy History from Burp", 0, 0);

        assertThat(progress.indeterminate()).isTrue();
        assertThat(progress.completed()).isZero();
        assertThat(progress.total()).isZero();
    }

    @Test
    void knownTotalRepresentsMeasurableProgress() {
        ScanCollectionProgress progress = new ScanCollectionProgress(
                "Filtering", 50, 200);

        assertThat(progress.indeterminate()).isFalse();
        assertThat(progress.completed()).isEqualTo(50);
        assertThat(progress.total()).isEqualTo(200);
    }
}
