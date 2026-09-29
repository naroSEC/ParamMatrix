package parammatrix.model;

import burp.api.montoya.http.message.HttpRequestResponse;

public record TestResult(
        String module,
        String parameter,
        boolean positive,
        String evidence,
        HttpRequestResponse exchange) {
}

