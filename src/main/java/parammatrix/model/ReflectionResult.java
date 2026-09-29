package parammatrix.model;

import burp.api.montoya.http.message.HttpRequestResponse;

public record ReflectionResult(
        ReflectionStatus status,
        ReflectionContext context,
        String marker,
        String evidence,
        HttpRequestResponse exchange,
        boolean individuallyVerified) {

    public static ReflectionResult notTested() {
        return new ReflectionResult(ReflectionStatus.NOT_TESTED, ReflectionContext.UNKNOWN,
                "", "", null, false);
    }

    public static ReflectionResult skipped(String reason) {
        return new ReflectionResult(ReflectionStatus.SKIPPED, ReflectionContext.UNKNOWN,
                "", reason, null, false);
    }
}

