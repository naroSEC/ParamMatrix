package parammatrix.config;

import parammatrix.testing.database.DatabaseRunOptions;
import parammatrix.testing.database.DatabaseType;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class DatabaseStressConfig {
    private final Set<DatabaseType> databases = EnumSet.allOf(DatabaseType.class);
    public final AtomicBoolean onlyReflectedParameters = new AtomicBoolean(false);
    public final AtomicBoolean verifyPositiveResults = new AtomicBoolean(true);
    public final AtomicInteger maximumRequestsPerPage = new AtomicInteger(20);
    public final AtomicInteger requestDelayMillis = new AtomicInteger(200);

    public synchronized void setSelected(DatabaseType database, boolean selected) {
        if (selected) databases.add(database); else databases.remove(database);
    }
    public synchronized void selectAll(boolean selected) {
        databases.clear();
        if (selected) databases.addAll(EnumSet.allOf(DatabaseType.class));
    }
    public synchronized boolean isSelected(DatabaseType database) { return databases.contains(database); }
    public synchronized Set<DatabaseType> selectedDatabases() { return Set.copyOf(databases); }
    public synchronized DatabaseRunOptions snapshot() {
        return new DatabaseRunOptions(selectedDatabases(), onlyReflectedParameters.get(),
                verifyPositiveResults.get(), maximumRequestsPerPage.get(), requestDelayMillis.get());
    }
}
