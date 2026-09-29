package burp;

import burp.api.montoya.MontoyaApi;
import parammatrix.analysis.ReflectionContextAnalyzer;
import parammatrix.analysis.DatabaseErrorSignatureAnalyzer;
import parammatrix.config.DatabaseStressConfig;
import parammatrix.config.ExtensionConfig;
import parammatrix.config.SstiConfig;
import parammatrix.core.AutoAnalyzer;
import parammatrix.core.ExtensionController;
import parammatrix.core.ParameterAnalyzerContextMenu;
import parammatrix.discovery.ContentTypeClassifier;
import parammatrix.discovery.HeuristicJavaScriptParser;
import parammatrix.discovery.HtmlParameterExtractor;
import parammatrix.discovery.ParameterDiscoveryEngine;
import parammatrix.http.ExtensionRequestRegistry;
import parammatrix.http.MarkerGenerator;
import parammatrix.http.RequestMutator;
import parammatrix.http.RequestSender;
import parammatrix.http.SimpleJsonRequestInjector;
import parammatrix.scan.HistoryScanService;
import parammatrix.storage.ResultRepository;
import parammatrix.storage.SstiResultRepository;
import parammatrix.storage.DatabaseResultRepository;
import parammatrix.testing.ReflectionTestEngine;
import parammatrix.testing.TestCoordinator;
import parammatrix.testing.ssti.DefaultSstiTestEngine;
import parammatrix.testing.ssti.FreeMarkerPayloadProvider;
import parammatrix.testing.ssti.GenericPayloadProvider;
import parammatrix.testing.ssti.Jinja2PayloadProvider;
import parammatrix.testing.ssti.SmartyPayloadProvider;
import parammatrix.testing.ssti.SstiCoordinator;
import parammatrix.testing.ssti.ThymeleafPayloadProvider;
import parammatrix.testing.ssti.TwigPayloadProvider;
import parammatrix.testing.ssti.VelocityPayloadProvider;
import parammatrix.testing.database.DatabaseErrorTestEngine;
import parammatrix.testing.database.DatabaseTestCoordinator;
import parammatrix.testing.database.SafeSyntaxPayloadProvider;
import parammatrix.ui.MainTab;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class BurpExtension implements burp.api.montoya.BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("ParamMatrix");

        ExtensionConfig config = new ExtensionConfig();
        SstiConfig sstiConfig = new SstiConfig();
        DatabaseStressConfig databaseConfig = new DatabaseStressConfig();
        ResultRepository repository = new ResultRepository();
        SstiResultRepository sstiResults = new SstiResultRepository();
        DatabaseResultRepository databaseResults = new DatabaseResultRepository();
        ExtensionRequestRegistry generatedRequests = new ExtensionRequestRegistry();
        RequestMutator requestMutator = new RequestMutator(new SimpleJsonRequestInjector());
        RequestSender requestSender = new RequestSender(api, generatedRequests);
        ParameterDiscoveryEngine discovery = new ParameterDiscoveryEngine(
                new HtmlParameterExtractor(), new HeuristicJavaScriptParser(),
                new ContentTypeClassifier(), config);
        ReflectionTestEngine reflection = new ReflectionTestEngine(
                new MarkerGenerator(), requestMutator, requestSender, new ReflectionContextAnalyzer());
        TestCoordinator testing = new TestCoordinator(reflection, config, repository);
        SstiCoordinator sstiCoordinator = new SstiCoordinator(
                new DefaultSstiTestEngine(requestMutator, requestSender),
                List.of(new GenericPayloadProvider(), new Jinja2PayloadProvider(),
                        new TwigPayloadProvider(), new FreeMarkerPayloadProvider(),
                        new VelocityPayloadProvider(), new ThymeleafPayloadProvider(),
                        new SmartyPayloadProvider()), sstiResults, config);
        DatabaseTestCoordinator databaseCoordinator = new DatabaseTestCoordinator(
                new DatabaseErrorTestEngine(requestMutator, requestSender,
                        new DatabaseErrorSignatureAnalyzer()),
                new SafeSyntaxPayloadProvider(), databaseResults, config);
        ExtensionController controller = new ExtensionController(api, config, discovery,
                testing, repository, generatedRequests, new HistoryScanService(api, config),
                sstiCoordinator, sstiConfig, databaseCoordinator, databaseConfig);

        MainTab mainTab = createUi(api, repository, sstiResults, databaseResults, config,
                sstiConfig, databaseConfig, controller);
        api.userInterface().registerSuiteTab("ParamMatrix", mainTab);
        api.userInterface().registerContextMenuItemsProvider(
                new ParameterAnalyzerContextMenu(controller));
        api.proxy().registerResponseHandler(new AutoAnalyzer(controller));
        api.extension().registerUnloadingHandler(controller::close);
        api.logging().logToOutput("ParamMatrix loaded. Auto Analysis is disabled by default.");
    }

    private MainTab createUi(MontoyaApi api, ResultRepository repository,
                             SstiResultRepository sstiResults,
                             DatabaseResultRepository databaseResults, ExtensionConfig config,
                             SstiConfig sstiConfig, DatabaseStressConfig databaseConfig,
                             ExtensionController controller) {
        if (SwingUtilities.isEventDispatchThread()) {
            return new MainTab(api, repository, sstiResults, databaseResults, config,
                    sstiConfig, databaseConfig, controller);
        }
        AtomicReference<MainTab> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(
                    new MainTab(api, repository, sstiResults, databaseResults, config,
                            sstiConfig, databaseConfig, controller)));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating ParamMatrix UI", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Failed to create ParamMatrix UI", exception.getCause());
        }
        return result.get();
    }
}
