package parammatrix.core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.config.ExtensionConfig;
import parammatrix.config.SstiConfig;
import parammatrix.config.DatabaseStressConfig;
import parammatrix.discovery.ParameterDiscoveryEngine;
import parammatrix.http.ExtensionRequestRegistry;
import parammatrix.model.PageIdentity;
import parammatrix.model.ParameterCandidate;
import parammatrix.scan.HistoryScanService;
import parammatrix.scan.ScanBatch;
import parammatrix.scan.ScanOptions;
import parammatrix.scan.ScanProgressListener;
import parammatrix.scan.ScanSummary;
import parammatrix.storage.ResultRepository;
import parammatrix.testing.TestCoordinator;
import parammatrix.testing.ssti.SstiCoordinator;
import parammatrix.testing.ssti.SstiProgressListener;
import parammatrix.testing.ssti.SstiRunOptions;
import parammatrix.testing.database.DatabaseProgressListener;
import parammatrix.testing.database.DatabaseRunOptions;
import parammatrix.testing.database.DatabaseTestCoordinator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ExtensionController implements AutoCloseable {
    public enum Action { EXTRACT, TEST, EXTRACT_AND_TEST }

    private final MontoyaApi api;
    private final ExtensionConfig config;
    private final ParameterDiscoveryEngine discovery;
    private final TestCoordinator testing;
    private final ResultRepository repository;
    private final ExtensionRequestRegistry generatedRequests;
    private final HistoryScanService historyScanService;
    private final SstiCoordinator sstiCoordinator;
    private final SstiConfig sstiConfig;
    private final DatabaseTestCoordinator databaseCoordinator;
    private final DatabaseStressConfig databaseConfig;
    private final ActiveTaskQueue queue;
    private final Set<PageIdentity> autoAnalyzed = ConcurrentHashMap.newKeySet();
    private final AtomicReference<HistoryScanRun> activeHistoryScan = new AtomicReference<>();

    public ExtensionController(MontoyaApi api, ExtensionConfig config,
                               ParameterDiscoveryEngine discovery, TestCoordinator testing,
                               ResultRepository repository,
                               ExtensionRequestRegistry generatedRequests,
                               HistoryScanService historyScanService,
                               SstiCoordinator sstiCoordinator,
                               SstiConfig sstiConfig,
                               DatabaseTestCoordinator databaseCoordinator,
                               DatabaseStressConfig databaseConfig) {
        this.api = api;
        this.config = config;
        this.discovery = discovery;
        this.testing = testing;
        this.repository = repository;
        this.generatedRequests = generatedRequests;
        this.historyScanService = historyScanService;
        this.sstiCoordinator = sstiCoordinator;
        this.sstiConfig = sstiConfig;
        this.databaseCoordinator = databaseCoordinator;
        this.databaseConfig = databaseConfig;
        this.queue = new ActiveTaskQueue(config.concurrentActiveTests.get());
    }

    public void submitManual(HttpRequestResponse exchange, Action action) {
        queue.submit(() -> execute(exchange, action));
    }

    public void submitAutomatic(HttpRequestResponse exchange) {
        if (!config.autoAnalysis.get()) return;
        if (config.inScopeOnly.get() && !exchange.request().isInScope()) return;
        if (config.ignoreGeneratedRequests.get() && generatedRequests.isGenerated(exchange.request())) return;
        PageIdentity identity = PageIdentity.from(exchange.request(), config.identityIgnoresParameterValues.get());
        if (!autoAnalyzed.add(identity)) return;
        queue.submit(() -> execute(exchange, config.reflectionEnabled.get()
                ? Action.EXTRACT_AND_TEST : Action.EXTRACT));
    }

    public void submitHistoryScan(ScanOptions options, ScanProgressListener listener) {
        HistoryScanRun run = new HistoryScanRun(listener);
        if (!activeHistoryScan.compareAndSet(null, run)) {
            listener.scanFailed("A history scan is already running");
            return;
        }
        queue.submit(() -> {
            try {
                if (run.cancelled()) return;
                listener.collectionStarted();
                ScanBatch batch = historyScanService.collect(options, listener, run::cancelled);
                if (run.cancelled()) return;
                listener.scanStarted(batch.summary());
                int total = batch.exchanges().size();
                if (total == 0) {
                    run.finish(batch.summary());
                    return;
                }
                AtomicInteger completed = new AtomicInteger();
                AtomicInteger started = new AtomicInteger();
                Action action = options.runReflectionTests()
                        ? Action.EXTRACT_AND_TEST : Action.EXTRACT;
                for (HttpRequestResponse exchange : batch.exchanges()) {
                    queue.submit(() -> {
                        if (run.cancelled()) return;
                        String description = exchange.request().method() + " "
                                + exchange.request().url();
                        listener.itemStarted(started.incrementAndGet(), total, description);
                        listener.itemStage(options.runReflectionTests()
                                ? "Discovery + reflection testing" : "Parameter discovery",
                                description);
                        List<ParameterCandidate> candidates = execute(exchange, action);
                        if (!run.cancelled() && options.runSstiTests()) {
                            listener.itemStage("SSTI testing", description);
                            sstiCoordinator.testPage(candidates, sstiConfig.snapshot());
                        }
                        if (!run.cancelled() && options.runDatabaseTests()) {
                            listener.itemStage("Database error testing", description);
                            databaseCoordinator.testPage(candidates, databaseConfig.snapshot());
                        }
                        if (run.cancelled()) return;
                        int done = completed.incrementAndGet();
                        listener.itemCompleted(done, total);
                        if (done == total) run.finish(batch.summary());
                    }, run::cancel);
                }
            } catch (OutOfMemoryError error) {
                api.logging().logToError(
                        "ParamMatrix history scan stopped due to Java heap exhaustion");
                run.fail("History scan stopped: Java heap exhausted while Burp loaded a response. "
                        + "Reduce Maximum pages or Maximum response size and retry.");
            } catch (RuntimeException exception) {
                api.logging().logToError("ParamMatrix history scan failed", exception);
                run.fail(exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }, run::cancel);
    }

    public boolean cancelHistoryScan() {
        HistoryScanRun run = activeHistoryScan.get();
        return run != null && run.cancel();
    }

    public void submitSstiTest(SstiRunOptions options, SstiProgressListener listener) {
        List<ParameterCandidate> candidates = repository.all().stream()
                .filter(candidate -> !options.onlyReflectedParameters()
                        || candidate.reflectionResult().status()
                        == parammatrix.model.ReflectionStatus.REFLECTED)
                .toList();
        Map<PageIdentity, List<ParameterCandidate>> pages = new LinkedHashMap<>();
        for (ParameterCandidate candidate : candidates) {
            pages.computeIfAbsent(candidate.pageIdentity(), ignored -> new ArrayList<>()).add(candidate);
        }
        listener.started(pages.size(), candidates.size());
        if (pages.isEmpty()) {
            listener.finished(0);
            return;
        }
        AtomicInteger completedPages = new AtomicInteger();
        AtomicInteger resultCount = new AtomicInteger();
        pages.values().forEach(page -> queue.submit(() -> {
            try {
                resultCount.addAndGet(sstiCoordinator.testPage(page, options).size());
                int done = completedPages.incrementAndGet();
                listener.pageCompleted(done, pages.size());
                if (done == pages.size()) listener.finished(resultCount.get());
            } catch (RuntimeException exception) {
                api.logging().logToError("ParamMatrix SSTI test failed", exception);
                listener.failed(exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }));
    }

    public void submitDatabaseTest(DatabaseRunOptions options,
                                   DatabaseProgressListener listener) {
        List<ParameterCandidate> candidates = repository.all().stream()
                .filter(candidate -> !options.onlyReflectedParameters()
                        || candidate.reflectionResult().status()
                        == parammatrix.model.ReflectionStatus.REFLECTED)
                .toList();
        Map<PageIdentity, List<ParameterCandidate>> pages = new LinkedHashMap<>();
        for (ParameterCandidate candidate : candidates) {
            pages.computeIfAbsent(candidate.pageIdentity(), ignored -> new ArrayList<>()).add(candidate);
        }
        listener.started(pages.size(), candidates.size());
        if (pages.isEmpty()) {
            listener.finished(0);
            return;
        }
        AtomicInteger completedPages = new AtomicInteger();
        AtomicInteger resultCount = new AtomicInteger();
        pages.values().forEach(page -> queue.submit(() -> {
            try {
                resultCount.addAndGet(databaseCoordinator.testPage(page, options).size());
                int done = completedPages.incrementAndGet();
                listener.pageCompleted(done, pages.size());
                if (done == pages.size()) listener.finished(resultCount.get());
            } catch (RuntimeException exception) {
                api.logging().logToError("ParamMatrix database stress test failed", exception);
                listener.failed(exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }));
    }

    private List<ParameterCandidate> execute(HttpRequestResponse exchange, Action action) {
        try {
            List<ParameterCandidate> candidates;
            if (action == Action.TEST) {
                PageIdentity identity = PageIdentity.from(exchange.request(),
                        config.identityIgnoresParameterValues.get());
                candidates = repository.all().stream()
                        .filter(candidate -> candidate.pageIdentity().equals(identity)).toList();
            } else {
                candidates = repository.saveAll(discovery.discover(exchange));
            }
            if (action != Action.EXTRACT) testing.testReflections(candidates);
            return candidates;
        } catch (RuntimeException exception) {
            api.logging().logToError("ParamMatrix task failed", exception);
            return List.of();
        }
    }

    public ActiveTaskQueue queue() { return queue; }
    @Override public void close() { queue.close(); }

    private final class HistoryScanRun {
        private final ScanProgressListener listener;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean terminal = new AtomicBoolean(false);

        private HistoryScanRun(ScanProgressListener listener) {
            this.listener = listener;
        }

        private boolean cancelled() {
            return cancelled.get();
        }

        private boolean cancel() {
            if (terminal.get() || !cancelled.compareAndSet(false, true)) return false;
            if (!terminal.compareAndSet(false, true)) return false;
            listener.scanCancelled();
            activeHistoryScan.compareAndSet(this, null);
            return true;
        }

        private void finish(ScanSummary summary) {
            if (cancelled.get() || !terminal.compareAndSet(false, true)) return;
            activeHistoryScan.compareAndSet(this, null);
            listener.scanFinished(summary);
        }

        private void fail(String message) {
            if (cancelled.get() || !terminal.compareAndSet(false, true)) return;
            activeHistoryScan.compareAndSet(this, null);
            listener.scanFailed(message);
        }
    }
}
