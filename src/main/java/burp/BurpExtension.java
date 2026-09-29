package burp;

import burp.api.montoya.MontoyaApi;
import parammatrix.analysis.ReflectionContextAnalyzer;
import parammatrix.config.ExtensionConfig;
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
import parammatrix.storage.ResultRepository;
import parammatrix.testing.ReflectionTestEngine;
import parammatrix.testing.TestCoordinator;
import parammatrix.ui.MainTab;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public final class BurpExtension implements burp.api.montoya.BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("ParamMatrix - Parameter Analyzer");

        ExtensionConfig config = new ExtensionConfig();
        ResultRepository repository = new ResultRepository();
        ExtensionRequestRegistry generatedRequests = new ExtensionRequestRegistry();
        ParameterDiscoveryEngine discovery = new ParameterDiscoveryEngine(
                new HtmlParameterExtractor(), new HeuristicJavaScriptParser(),
                new ContentTypeClassifier(), config);
        ReflectionTestEngine reflection = new ReflectionTestEngine(
                new MarkerGenerator(), new RequestMutator(new SimpleJsonRequestInjector()),
                new RequestSender(api, generatedRequests), new ReflectionContextAnalyzer());
        TestCoordinator testing = new TestCoordinator(reflection, config, repository);
        ExtensionController controller = new ExtensionController(api, config, discovery,
                testing, repository, generatedRequests);

        MainTab mainTab = createUi(api, repository, config, controller);
        api.userInterface().registerSuiteTab("Parameter Analyzer", mainTab);
        api.userInterface().registerContextMenuItemsProvider(
                new ParameterAnalyzerContextMenu(controller));
        api.proxy().registerResponseHandler(new AutoAnalyzer(controller));
        api.extension().registerUnloadingHandler(controller::close);
        api.logging().logToOutput("ParamMatrix loaded. Auto Analysis is disabled by default.");
    }

    private MainTab createUi(MontoyaApi api, ResultRepository repository,
                             ExtensionConfig config, ExtensionController controller) {
        if (SwingUtilities.isEventDispatchThread()) {
            return new MainTab(api, repository, config, controller.queue());
        }
        AtomicReference<MainTab> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(
                    new MainTab(api, repository, config, controller.queue())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating ParamMatrix UI", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Failed to create ParamMatrix UI", exception.getCause());
        }
        return result.get();
    }
}

