package parammatrix.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import parammatrix.testing.ssti.SstiTestResult;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

public final class SstiResultDetailPanel extends JPanel {
    private final HttpRequestEditor originalRequest;
    private final HttpResponseEditor originalResponse;
    private final HttpRequestEditor testRequest;
    private final HttpResponseEditor testResponse;
    private final JTextArea evidence = new JTextArea();

    public SstiResultDetailPanel(MontoyaApi api) {
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
        tabs.addTab("Detection Evidence", new JScrollPane(evidence));
        add(tabs, BorderLayout.CENTER);
    }

    public void showResult(SstiTestResult result) {
        originalRequest.setRequest(result.originalExchange().request());
        if (result.originalExchange().hasResponse()) {
            originalResponse.setResponse(result.originalExchange().response());
        }
        if (result.testExchange() != null) {
            testRequest.setRequest(result.testExchange().request());
            if (result.testExchange().hasResponse()) {
                testResponse.setResponse(result.testExchange().response());
            }
        } else {
            testRequest.setRequest(HttpRequest.httpRequest());
            testResponse.setResponse(HttpResponse.httpResponse());
        }
        evidence.setText("Status: " + result.status()
                + "\nTemplate engine: " + result.templateEngine()
                + "\nDetection method: " + result.detectionMethod()
                + "\nConfidence: " + result.confidence()
                + "\n\nPayload\n-------\n" + result.payload()
                + "\n\nExpected result\n---------------\n" + result.expectedResult()
                + "\n\nActual result\n-------------\n" + result.actualResult()
                + "\n\nEvidence\n--------\n" + result.evidence());
        evidence.setCaretPosition(0);
    }
}
