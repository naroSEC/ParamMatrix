package parammatrix.core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import parammatrix.config.ExtensionConfig;
import parammatrix.discovery.ParameterDiscoveryEngine;
import parammatrix.http.ExtensionRequestRegistry;
import parammatrix.model.PageIdentity;
import parammatrix.model.ParameterCandidate;
import parammatrix.storage.ResultRepository;
import parammatrix.testing.TestCoordinator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtensionController implements AutoCloseable {
    public enum Action { EXTRACT, TEST, EXTRACT_AND_TEST }

    private final MontoyaApi api;
    private final ExtensionConfig config;
    private final ParameterDiscoveryEngine discovery;
    private final TestCoordinator testing;
    private final ResultRepository repository;
    private final ExtensionRequestRegistry generatedRequests;
    private final ActiveTaskQueue queue;
    private final Set<PageIdentity> autoAnalyzed = ConcurrentHashMap.newKeySet();

    public ExtensionController(MontoyaApi api, ExtensionConfig config,
                               ParameterDiscoveryEngine discovery, TestCoordinator testing,
                               ResultRepository repository,
                               ExtensionRequestRegistry generatedRequests) {
        this.api = api;
        this.config = config;
        this.discovery = discovery;
        this.testing = testing;
        this.repository = repository;
        this.generatedRequests = generatedRequests;
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

    private void execute(HttpRequestResponse exchange, Action action) {
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
        } catch (RuntimeException exception) {
            api.logging().logToError("ParamMatrix task failed", exception);
        }
    }

    public ActiveTaskQueue queue() { return queue; }
    @Override public void close() { queue.close(); }
}
