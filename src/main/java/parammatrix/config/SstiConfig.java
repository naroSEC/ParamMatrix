package parammatrix.config;

import parammatrix.http.PayloadEncodingMode;
import parammatrix.testing.ssti.SstiEngine;
import parammatrix.testing.ssti.SstiRunOptions;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class SstiConfig {
    private final Set<SstiEngine> selectedEngines = EnumSet.of(SstiEngine.GENERIC);
    public final AtomicBoolean onlyReflectedParameters = new AtomicBoolean(false);
    public final AtomicInteger maximumRequestsPerPage = new AtomicInteger(30);
    public final AtomicInteger requestDelayMillis = new AtomicInteger(200);
    private PayloadEncodingMode payloadEncodingMode = PayloadEncodingMode.AUTO;

    public synchronized void setSelected(SstiEngine engine, boolean selected) {
        if (selected) selectedEngines.add(engine);
        else selectedEngines.remove(engine);
    }

    public synchronized void selectAll(boolean selected) {
        selectedEngines.clear();
        if (selected) selectedEngines.addAll(EnumSet.allOf(SstiEngine.class));
    }

    public synchronized boolean isSelected(SstiEngine engine) {
        return selectedEngines.contains(engine);
    }

    public synchronized Set<SstiEngine> selectedEngines() {
        return Set.copyOf(selectedEngines);
    }

    public synchronized PayloadEncodingMode payloadEncodingMode() {
        return payloadEncodingMode;
    }

    public synchronized void setPayloadEncodingMode(PayloadEncodingMode mode) {
        payloadEncodingMode = mode;
    }

    public synchronized SstiRunOptions snapshot() {
        return new SstiRunOptions(selectedEngines(), onlyReflectedParameters.get(),
                maximumRequestsPerPage.get(), requestDelayMillis.get(), payloadEncodingMode);
    }
}

