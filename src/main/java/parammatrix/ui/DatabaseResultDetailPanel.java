package parammatrix.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import parammatrix.testing.database.DatabaseTestResult;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

public final class DatabaseResultDetailPanel extends JPanel {
    private final HttpRequestEditor originalRequest;
    private final HttpResponseEditor originalResponse;
    private final HttpRequestEditor testRequest;
    private final HttpResponseEditor testResponse;
    private final JTextArea evidence = new JTextArea();

    public DatabaseResultDetailPanel(MontoyaApi api) {
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
        tabs.addTab("Error Evidence", new JScrollPane(evidence));
        add(tabs, BorderLayout.CENTER);
    }

    public void showResult(DatabaseTestResult result) {
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
                + "\nSuspected database: " + result.suspectedDatabase()
                + "\nConfidence: " + result.confidence()
                + "\nVerified: " + result.verified()
                + "\nHTTP status: " + result.originalStatus() + " -> " + result.testStatus()
                + "\nResponse length delta: " + result.responseLengthDelta()
                + "\n\nPayload\n-------\n" + result.payloadName() + ": " + result.payload()
                + "\n\nError signature\n---------------\n" + result.errorSignature()
                + "\n\nEvidence\n--------\n" + result.evidence());
        evidence.setCaretPosition(0);
    }
}

