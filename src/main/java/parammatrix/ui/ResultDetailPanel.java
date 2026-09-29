package parammatrix.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import parammatrix.model.DiscoveryEvidence;
import parammatrix.model.ParameterCandidate;

import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.util.stream.Collectors;

public final class ResultDetailPanel extends JPanel {
    private final HttpRequestEditor originalRequest;
    private final HttpResponseEditor originalResponse;
    private final HttpRequestEditor testRequest;
    private final HttpResponseEditor testResponse;
    private final JTextArea evidence = new JTextArea();

    public ResultDetailPanel(MontoyaApi api) {
        super(new BorderLayout());
        originalRequest = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        originalResponse = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        testRequest = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        testResponse = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        evidence.setEditable(false);
        evidence.setLineWrap(true);
        evidence.setWrapStyleWord(true);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Original Request", originalRequest.uiComponent());
        tabs.addTab("Test Request", testRequest.uiComponent());
        tabs.addTab("Original Response", originalResponse.uiComponent());
        tabs.addTab("Test Response", testResponse.uiComponent());
        tabs.addTab("Evidence", new JScrollPane(evidence));
        add(tabs, BorderLayout.CENTER);
    }

    public void showCandidate(ParameterCandidate candidate) {
        originalRequest.setRequest(candidate.originalExchange().request());
        if (candidate.originalExchange().hasResponse()) {
            originalResponse.setResponse(candidate.originalExchange().response());
        }
        if (candidate.reflectionResult().exchange() != null) {
            testRequest.setRequest(candidate.reflectionResult().exchange().request());
            if (candidate.reflectionResult().exchange().hasResponse()) {
                testResponse.setResponse(candidate.reflectionResult().exchange().response());
            }
        }
        String discovery = candidate.evidence().stream()
                .map(this::formatEvidence).collect(Collectors.joining("\n\n"));
        evidence.setText("Discovery evidence\n------------------\n" + discovery
                + "\n\nReflection evidence\n-------------------\n"
                + candidate.reflectionResult().evidence());
        evidence.setCaretPosition(0);
    }

    private String formatEvidence(DiscoveryEvidence item) {
        return "Source: " + item.source() + "\nConfidence: " + item.confidence()
                + "\nEndpoint: " + item.endpoint() + "\nDetail: " + item.detail();
    }
}

