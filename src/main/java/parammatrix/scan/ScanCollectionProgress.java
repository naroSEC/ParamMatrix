package parammatrix.scan;

public record ScanCollectionProgress(
        String stage,
        int completed,
        int total) {

    public ScanCollectionProgress {
        stage = stage == null ? "Collecting traffic" : stage;
        completed = Math.max(0, completed);
        total = Math.max(0, total);
    }

    public boolean indeterminate() {
        return total == 0;
    }
}
