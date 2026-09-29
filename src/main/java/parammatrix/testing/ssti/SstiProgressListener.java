package parammatrix.testing.ssti;

public interface SstiProgressListener {
    default void started(int pages, int parameters) {}
    default void pageCompleted(int completed, int total) {}
    default void finished(int resultCount) {}
    default void failed(String message) {}
}
