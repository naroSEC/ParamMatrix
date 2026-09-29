package parammatrix.storage;

import parammatrix.model.ParameterCandidate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ResultRepository {
    private final Map<String, ParameterCandidate> candidates = new LinkedHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public synchronized List<ParameterCandidate> saveAll(List<ParameterCandidate> items) {
        List<ParameterCandidate> canonical = new ArrayList<>();
        for (ParameterCandidate item : items) {
            String key = item.pageIdentity().toString() + "|" + item.name();
            ParameterCandidate current = candidates.get(key);
            if (current == null) {
                candidates.put(key, item);
                canonical.add(item);
            } else {
                item.evidence().forEach(current::addEvidence);
                canonical.add(current);
            }
        }
        notifyListeners();
        return canonical;
    }

    public synchronized List<ParameterCandidate> all() {
        return new ArrayList<>(candidates.values());
    }

    public void changed() { notifyListeners(); }
    public void addListener(Runnable listener) { listeners.add(listener); }

    private void notifyListeners() { listeners.forEach(Runnable::run); }
}
