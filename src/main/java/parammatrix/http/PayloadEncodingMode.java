package parammatrix.http;

public enum PayloadEncodingMode {
    AUTO("Auto (recommended)"),
    RAW("Raw");

    private final String displayName;

    PayloadEncodingMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
