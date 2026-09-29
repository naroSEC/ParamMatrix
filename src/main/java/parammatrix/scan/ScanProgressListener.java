package parammatrix.scan;

public interface ScanProgressListener {
    default void collectionStarted() {}
    default void scanStarted(ScanSummary summary) {}
    default void itemCompleted(int completed, int total) {}
    default void scanFinished(ScanSummary summary) {}
    default void scanCancelled() {}
    default void scanFailed(String message) {}
}

