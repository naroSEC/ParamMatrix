package parammatrix.core;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction;

public final class AutoAnalyzer implements ProxyResponseHandler {
    private final ExtensionController controller;

    public AutoAnalyzer(ExtensionController controller) {
        this.controller = controller;
    }

    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse response) {
        controller.submitAutomatic(HttpRequestResponse.httpRequestResponse(response.request(), response));
        return ProxyResponseReceivedAction.continueWith(response);
    }

    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse response) {
        return ProxyResponseToBeSentAction.continueWith(response);
    }
}

