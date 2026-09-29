package parammatrix.http;

import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.Map;

public interface JsonRequestInjector {
    HttpRequest inject(HttpRequest request, Map<String, String> values);
}

