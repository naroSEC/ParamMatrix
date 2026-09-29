package parammatrix.model;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

public record PageIdentity(
        String protocol,
        String host,
        int port,
        String method,
        String path,
        String parameterStructure) {

    public PageIdentity {
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        parameterStructure = parameterStructure == null ? "" : parameterStructure;
    }

    public static PageIdentity from(HttpRequest request, boolean ignoreParameterValues) {
        HttpService service = request.httpService();
        String structure = request.parameters().stream()
                .sorted(Comparator.comparing(ParsedHttpParameter::name)
                        .thenComparing(p -> p.type().name()))
                .map(p -> p.type().name() + ":" + p.name()
                        + (ignoreParameterValues ? "" : "=" + p.value()))
                .collect(Collectors.joining("&"));
        return new PageIdentity(
                service.secure() ? "https" : "http",
                service.host().toLowerCase(),
                service.port(),
                request.method().toUpperCase(),
                request.pathWithoutQuery(),
                structure);
    }

    public String displayUrl() {
        int defaultPort = protocol.equals("https") ? 443 : 80;
        return protocol + "://" + host + (port == defaultPort ? "" : ":" + port) + path;
    }
}

