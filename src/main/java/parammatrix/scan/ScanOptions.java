package parammatrix.scan;

import java.util.List;

public record ScanOptions(
        boolean proxyHistory,
        boolean siteMap,
        boolean get,
        boolean post,
        boolean runReflectionTests,
        boolean runSstiTests,
        boolean runDatabaseTests,
        ScanCookieMode cookieMode,
        String customCookieHost,
        String customCookieHeader,
        List<String> excludedPathRules) {

    public ScanOptions {
        cookieMode = cookieMode == null ? ScanCookieMode.KEEP_RECORDED : cookieMode;
        customCookieHost = customCookieHost == null ? "" : customCookieHost.trim();
        customCookieHeader = customCookieHeader == null ? "" : customCookieHeader.trim();
        if (customCookieHeader.indexOf('\r') >= 0 || customCookieHeader.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Custom Cookie header must be a single line");
        }
        if (cookieMode == ScanCookieMode.CUSTOM_HEADER
                && (customCookieHost.isBlank() || customCookieHeader.isBlank())) {
            throw new IllegalArgumentException(
                    "Custom Cookie mode requires a target host and Cookie value");
        }
        excludedPathRules = excludedPathRules == null ? List.of() : List.copyOf(excludedPathRules);
    }
}
