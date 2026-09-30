package parammatrix.http;

import burp.api.montoya.http.message.requests.HttpRequest;

public record PayloadMutation(
        HttpRequest request,
        String logicalValue,
        String wireValue,
        String encodingDescription) {
}
