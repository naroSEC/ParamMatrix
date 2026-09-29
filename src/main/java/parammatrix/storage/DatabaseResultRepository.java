package parammatrix.storage;

import parammatrix.testing.database.DatabaseTestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DatabaseResultRepository {
    private final List<DatabaseTestResult> results = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public synchronized void save(DatabaseTestResult result) {
        results.add(result);
        listeners.forEach(Runnable::run);
    }
    public synchronized List<DatabaseTestResult> all() { return List.copyOf(results); }
    public synchronized void clear() {
        results.clear();
        listeners.forEach(Runnable::run);
    }
    public void addListener(Runnable listener) { listeners.add(listener); }
}

