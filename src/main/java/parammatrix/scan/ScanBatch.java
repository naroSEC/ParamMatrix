package parammatrix.scan;

import burp.api.montoya.http.message.HttpRequestResponse;

import java.util.List;

public record ScanBatch(List<HttpRequestResponse> exchanges, ScanSummary summary) {
    public ScanBatch {
        exchanges = List.copyOf(exchanges);
    }
}

