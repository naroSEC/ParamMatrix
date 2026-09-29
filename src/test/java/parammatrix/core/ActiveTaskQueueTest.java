package parammatrix.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveTaskQueueTest {
    @Test
    void clearingWhilePausedDiscardsTasksAlreadyWaitingAtPauseGate()
            throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicInteger discarded = new AtomicInteger();

        try (ActiveTaskQueue queue = new ActiveTaskQueue(1)) {
            queue.pause();
            queue.submit(() -> executed.set(true), discarded::incrementAndGet);
            for (int attempt = 0; attempt < 50 && queue.queuedCount() == 0; attempt++) {
                Thread.sleep(10);
            }

            assertThat(queue.queuedCount()).isEqualTo(1);
            assertThat(queue.activeCount()).isZero();
            assertThat(queue.clearPending()).isEqualTo(1);
            queue.resume();
            Thread.sleep(50);
            assertThat(discarded).hasValue(1);
            assertThat(executed).isFalse();
        }
    }

    @Test
    void clearingPendingTasksInvokesDiscardCallbackWithoutInterruptingActiveTask()
            throws InterruptedException {
        CountDownLatch activeStarted = new CountDownLatch(1);
        CountDownLatch releaseActive = new CountDownLatch(1);
        AtomicBoolean activeCompleted = new AtomicBoolean(false);
        AtomicBoolean pendingExecuted = new AtomicBoolean(false);
        AtomicInteger discarded = new AtomicInteger();

        try (ActiveTaskQueue queue = new ActiveTaskQueue(1)) {
            queue.submit(() -> {
                activeStarted.countDown();
                try {
                    releaseActive.await();
                    activeCompleted.set(true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(activeStarted.await(2, TimeUnit.SECONDS)).isTrue();
            queue.submit(() -> pendingExecuted.set(true), discarded::incrementAndGet);

            assertThat(queue.clearPending()).isEqualTo(1);
            assertThat(discarded).hasValue(1);
            releaseActive.countDown();
            for (int attempt = 0; attempt < 20 && !activeCompleted.get(); attempt++) {
                Thread.sleep(10);
            }
            assertThat(activeCompleted).isTrue();
            assertThat(pendingExecuted).isFalse();
        }
    }
}
