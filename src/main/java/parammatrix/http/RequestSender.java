package parammatrix.http;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

public final class RequestSender {
    private final MontoyaApi api;
    private final ExtensionRequestRegistry registry;

    public RequestSender(MontoyaApi api, ExtensionRequestRegistry registry) {
        this.api = api;
        this.registry = registry;
    }

    public HttpRequestResponse send(HttpRequest request) {
        registry.register(request);
        return api.http().sendRequest(request);
    }
}

