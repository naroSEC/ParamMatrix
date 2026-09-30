package parammatrix.scan;

public record ScanSummary(
        int sourceRecords,
        int eligiblePages,
        int excludedByMethod,
        int excludedByPath,
        int duplicates,
        int withoutResponse,
        int cookieHeadersUpdated) {
}

