package parammatrix.core;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ActiveTaskQueue implements AutoCloseable {
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final Object pauseMonitor = new Object();
    private final ThreadPoolExecutor executor;

    public ActiveTaskQueue(int concurrency) {
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "param-matrix-worker-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        executor = new ThreadPoolExecutor(concurrency, concurrency, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), factory);
    }

    public void submit(Runnable task) {
        executor.execute(() -> {
            awaitIfPaused();
            if (!Thread.currentThread().isInterrupted()) task.run();
        });
    }

    public void pause() { paused.set(true); }
    public void resume() {
        synchronized (pauseMonitor) {
            paused.set(false);
            pauseMonitor.notifyAll();
        }
    }
    public boolean isPaused() { return paused.get(); }
    public int queuedCount() { return executor.getQueue().size(); }
    public int activeCount() { return executor.getActiveCount(); }
    public void clearPending() { executor.getQueue().clear(); }

    private void awaitIfPaused() {
        synchronized (pauseMonitor) {
            while (paused.get()) {
                try { pauseMonitor.wait(); }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override public void close() {
        resume();
        executor.shutdownNow();
    }
}

