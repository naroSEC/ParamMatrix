package parammatrix.testing.database;

import java.util.Set;

public record DatabaseRunOptions(
        Set<DatabaseType> databases,
        boolean onlyReflectedParameters,
        boolean verifyPositiveResults,
        int maximumRequestsPerPage,
        int requestDelayMillis) {

    public DatabaseRunOptions {
        databases = Set.copyOf(databases);
        if (maximumRequestsPerPage < 1) throw new IllegalArgumentException("maximumRequestsPerPage");
        if (requestDelayMillis < 0) throw new IllegalArgumentException("requestDelayMillis");
    }
}

