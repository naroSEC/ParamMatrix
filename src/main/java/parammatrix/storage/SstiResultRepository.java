package parammatrix.storage;

import parammatrix.testing.ssti.SstiTestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SstiResultRepository {
    private final List<SstiTestResult> results = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public synchronized void save(SstiTestResult result) {
        results.add(result);
        notifyListeners();
    }

    public synchronized void saveAll(List<SstiTestResult> items) {
        results.addAll(items);
        notifyListeners();
    }

    public synchronized List<SstiTestResult> all() {
        return List.copyOf(results);
    }

    public synchronized void clear() {
        results.clear();
        notifyListeners();
    }

    public void addListener(Runnable listener) { listeners.add(listener); }
    private void notifyListeners() { listeners.forEach(Runnable::run); }
}

