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
        boolean refreshCookiesFromJar,
        List<String> excludedPathRules) {

    public ScanOptions {
        excludedPathRules = excludedPathRules == null ? List.of() : List.copyOf(excludedPathRules);
    }
}
