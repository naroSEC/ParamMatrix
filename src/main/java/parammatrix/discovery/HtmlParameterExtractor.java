package parammatrix.discovery;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import parammatrix.config.ExtensionConfig;
import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterSource;

import java.util.ArrayList;
import java.util.List;

public final class HtmlParameterExtractor {
    private final UrlParameterExtractor urlExtractor = new UrlParameterExtractor();

    public Extraction extract(String html, String baseUrl, ExtensionConfig config) {
        Document document = Jsoup.parse(html, baseUrl);
        List<DiscoveredParameter> parameters = new ArrayList<>();
        if (config.htmlFormFields.get()) {
            collectNamed(document.select("input[name]"), ParameterSource.HTML_INPUT, parameters);
            collectNamed(document.select("select[name]"), ParameterSource.HTML_SELECT, parameters);
            collectNamed(document.select("textarea[name]"), ParameterSource.HTML_TEXTAREA, parameters);
            collectNamed(document.select("button[name]"), ParameterSource.HTML_BUTTON, parameters);
        }
        if (config.htmlUrls.get()) {
            collectUrls(document.select("a[href]"), "href", parameters);
            collectUrls(document.select("form[action]"), "action", parameters);
            collectUrls(document.select("iframe[src], script[src], img[src], link[href]"),
                    "src", parameters);
            collectUrls(document.select("[data-url], [data-href], [formaction]"),
                    "data-url", parameters);
        }
        List<String> scripts = config.inlineJavaScript.get()
                ? document.select("script:not([src])").stream().map(Element::data).toList()
                : List.of();
        return new Extraction(parameters, scripts);
    }

    private void collectNamed(Elements elements, ParameterSource source,
                              List<DiscoveredParameter> output) {
        for (Element element : elements) {
            String name = element.attr("name").trim();
            if (UrlParameterExtractor.isReasonableName(name)) {
                output.add(new DiscoveredParameter(name, source,
                        element.outerHtml(), "", DiscoveryConfidence.HIGH));
            }
        }
    }

    private void collectUrls(Elements elements, String preferredAttribute,
                             List<DiscoveredParameter> output) {
        for (Element element : elements) {
            String attribute = element.hasAttr(preferredAttribute) ? preferredAttribute
                    : element.hasAttr("href") ? "href"
                    : element.hasAttr("src") ? "src"
                    : element.hasAttr("action") ? "action"
                    : element.hasAttr("formaction") ? "formaction"
                    : element.hasAttr("data-href") ? "data-href" : "data-url";
            String value = element.attr(attribute);
            output.addAll(urlExtractor.extract(value, ParameterSource.HTML_URL,
                    element.tagName() + "[" + attribute + "]=" + value));
        }
    }

    public record Extraction(List<DiscoveredParameter> parameters, List<String> inlineScripts) {}
}

