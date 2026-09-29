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
    private final java.util.Set<QueueTask> pauseWaiting = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        submit(task, () -> { });
    }

    public void submit(Runnable task, Runnable onDiscard) {
        executor.execute(new QueueTask(task, onDiscard));
    }

    public void pause() { paused.set(true); }
    public void resume() {
        synchronized (pauseMonitor) {
            paused.set(false);
            pauseMonitor.notifyAll();
        }
    }
    public boolean isPaused() { return paused.get(); }
    public int queuedCount() { return executor.getQueue().size() + pauseWaiting.size(); }
    public int activeCount() {
        return Math.max(0, executor.getActiveCount() - pauseWaiting.size());
    }
    public int clearPending() {
        java.util.List<Runnable> discarded = new java.util.ArrayList<>();
        executor.getQueue().drainTo(discarded);
        int count = 0;
        for (Runnable task : discarded) {
            if (task instanceof QueueTask queued && queued.discard()) count++;
        }
        for (QueueTask waiting : java.util.List.copyOf(pauseWaiting)) {
            if (waiting.discard()) count++;
        }
        return count;
    }

    private void awaitIfPaused(QueueTask task) {
        synchronized (pauseMonitor) {
            while (paused.get() && !task.discarded.get()) {
                try { pauseMonitor.wait(); }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private final class QueueTask implements Runnable {
        private final Runnable action;
        private final Runnable onDiscard;
        private final AtomicBoolean discarded = new AtomicBoolean(false);

        private QueueTask(Runnable action, Runnable onDiscard) {
            this.action = java.util.Objects.requireNonNull(action, "action");
            this.onDiscard = java.util.Objects.requireNonNull(onDiscard, "onDiscard");
        }

        @Override public void run() {
            if (paused.get()) {
                pauseWaiting.add(this);
                try {
                    awaitIfPaused(this);
                } finally {
                    pauseWaiting.remove(this);
                }
            }
            if (!discarded.get() && !Thread.currentThread().isInterrupted()) action.run();
        }

        private boolean discard() {
            if (!discarded.compareAndSet(false, true)) return false;
            onDiscard.run();
            synchronized (pauseMonitor) {
                pauseMonitor.notifyAll();
            }
            return true;
        }
    }

    @Override public void close() {
        resume();
        executor.shutdownNow();
    }
}

