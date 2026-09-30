package parammatrix.scan;

public interface ScanProgressListener {
    default void collectionStarted() {}
    default void collectionProgress(ScanCollectionProgress progress) {}
    default void scanStarted(ScanSummary summary) {}
    default void itemStarted(int started, int total, String description) {}
    default void itemStage(String stage, String description) {}
    default void itemCompleted(int completed, int total) {}
    default void scanFinished(ScanSummary summary) {}
    default void scanCancelled() {}
    default void scanFailed(String message) {}
}

