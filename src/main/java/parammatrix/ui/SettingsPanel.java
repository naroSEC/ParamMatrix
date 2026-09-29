package parammatrix.ui;

import parammatrix.config.ExtensionConfig;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

public final class SettingsPanel extends JPanel {
    public SettingsPanel(ExtensionConfig config) {
        super(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(18, 20, 18, 20));
        add(header(), BorderLayout.NORTH);
        add(content(config), BorderLayout.CENTER);
    }

    private JPanel header() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 7f));
        JLabel description = new JLabel(
                "Configure discovery, safety limits, and reflection behavior for ParamMatrix.");
        description.setBorder(new EmptyBorder(4, 0, 0, 0));
        panel.add(title);
        panel.add(description);
        return panel;
    }

    private JPanel content(ExtensionConfig config) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel cards = new JPanel(new GridLayout(2, 2, 12, 12));
        cards.add(general(config));
        cards.add(limits(config));
        cards.add(contentTypes(config));
        cards.add(discovery(config));
        content.add(cards);
        content.add(Box.createVerticalStrut(14));
        content.add(reflection(config));
        return content;
    }

    private JPanel general(ExtensionConfig config) {
        JPanel panel = card("General & safety",
                "Control automatic processing, target scope, and request identity.");
        panel.add(check("Enable Auto Analysis", config.autoAnalysis.get(), config.autoAnalysis::set));
        panel.add(check("Analyze only in-scope targets", config.inScopeOnly.get(),
                config.inScopeOnly::set));
        panel.add(check("Ignore extension-generated requests", config.ignoreGeneratedRequests.get(),
                config.ignoreGeneratedRequests::set));
        panel.add(check("Identity ignores parameter values", config.identityIgnoresParameterValues.get(),
                config.identityIgnoresParameterValues::set));
        return panel;
    }

    private JPanel limits(ExtensionConfig config) {
        JPanel panel = card("Resource limits",
                "Bound parsing work and the shared active-test worker pool.");
        panel.add(spinnerRow("Maximum response size", "bytes",
                config.maximumResponseBytes.get(), 1024, 100_000_000,
                config.maximumResponseBytes::set));
        panel.add(spinnerRow("Maximum candidates", "per page",
                config.maximumCandidatesPerPage.get(), 1, 10_000,
                config.maximumCandidatesPerPage::set));
        panel.add(spinnerRow("Concurrent active tests", "workers",
                config.concurrentActiveTests.get(), 1, 20,
                config.concurrentActiveTests::set));
        JLabel reload = new JLabel("Worker-count changes apply after extension reload.");
        reload.setBorder(new EmptyBorder(5, 2, 0, 0));
        panel.add(reload);
        return panel;
    }

    private JPanel contentTypes(ExtensionConfig config) {
        JPanel panel = card("Response classification",
                "Choose which response formats are eligible for parameter discovery.");
        panel.add(check("Analyze text/html", config.analyzeHtml.get(), config.analyzeHtml::set));
        panel.add(check("Analyze application/xhtml+xml", config.analyzeXhtml.get(),
                config.analyzeXhtml::set));
        panel.add(check("Enable HTML Auto Detection", config.htmlAutoDetection.get(),
                config.htmlAutoDetection::set));
        panel.add(check("Exclude JSON responses from discovery", config.ignoreJsonResponses.get(),
                config.ignoreJsonResponses::set));
        return panel;
    }

    private JPanel discovery(ExtensionConfig config) {
        JPanel panel = card("Parameter discovery",
                "Select the HTML and JavaScript sources used to create candidates.");
        panel.add(check("HTML form fields", config.htmlFormFields.get(), config.htmlFormFields::set));
        panel.add(check("HTML URLs", config.htmlUrls.get(), config.htmlUrls::set));
        panel.add(check("Inline JavaScript", config.inlineJavaScript.get(),
                config.inlineJavaScript::set));
        panel.add(check("JavaScript HTTP sink analysis", config.javaScriptSinkAnalysis.get(),
                config.javaScriptSinkAnalysis::set));
        panel.add(check("Include LOW-confidence candidates", config.includeLowConfidence.get(),
                config.includeLowConfidence::set));
        return panel;
    }

    private JPanel reflection(ExtensionConfig config) {
        JPanel panel = new JPanel(new BorderLayout(18, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Reflection testing"),
                new EmptyBorder(10, 12, 12, 12)));
        JPanel workflow = new JPanel();
        workflow.setLayout(new BoxLayout(workflow, BoxLayout.Y_AXIS));
        workflow.add(sectionLabel("Workflow"));
        workflow.add(check("Enable Reflection Test", config.reflectionEnabled.get(),
                config.reflectionEnabled::set));
        workflow.add(new JLabel("Used by Auto Analysis and history scans."));

        JPanel modes = new JPanel();
        modes.setLayout(new BoxLayout(modes, BoxLayout.Y_AXIS));
        modes.add(sectionLabel("Request mode"));
        ButtonGroup group = new ButtonGroup();
        modes.add(radio("Batch Only", ExtensionConfig.ReflectionMode.BATCH_ONLY, config, group));
        modes.add(radio("Individual Only", ExtensionConfig.ReflectionMode.INDIVIDUAL_ONLY,
                config, group));
        modes.add(radio("Batch + Individual Verification",
                ExtensionConfig.ReflectionMode.BATCH_AND_VERIFY, config, group));

        JPanel pacing = new JPanel();
        pacing.setLayout(new BoxLayout(pacing, BoxLayout.Y_AXIS));
        pacing.add(sectionLabel("Pacing & budget"));
        pacing.add(spinnerRow("Request delay", "ms", config.requestDelayMillis.get(),
                0, 60_000, config.requestDelayMillis::set));
        pacing.add(spinnerRow("Maximum requests", "per page",
                config.maximumRequestsPerPage.get(), 1, 1_000,
                config.maximumRequestsPerPage::set));

        JPanel columns = new JPanel(new GridLayout(1, 3, 18, 0));
        columns.add(workflow);
        columns.add(modes);
        columns.add(pacing);
        panel.add(columns, BorderLayout.CENTER);
        return panel;
    }

    private JPanel card(String title, String description) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title), new EmptyBorder(8, 10, 10, 10)));
        JLabel detail = new JLabel(description);
        detail.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(detail);
        return panel;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setBorder(new EmptyBorder(0, 0, 5, 0));
        return label;
    }

    private JCheckBox check(String label, boolean selected,
                            java.util.function.Consumer<Boolean> setter) {
        JCheckBox checkBox = new JCheckBox(label, selected);
        checkBox.addActionListener(ignored -> setter.accept(checkBox.isSelected()));
        return checkBox;
    }

    private JRadioButton radio(String label, ExtensionConfig.ReflectionMode value,
                               ExtensionConfig config, ButtonGroup group) {
        JRadioButton button = new JRadioButton(label, config.reflectionMode == value);
        button.addActionListener(ignored -> config.reflectionMode = value);
        group.add(button);
        return button;
    }

    private JPanel spinnerRow(String label, String unit, int current, int minimum, int maximum,
                              java.util.function.IntConsumer setter) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBorder(new EmptyBorder(2, 2, 2, 2));
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(current, minimum, maximum, 1));
        spinner.setPreferredSize(new Dimension(120, spinner.getPreferredSize().height));
        spinner.addChangeListener(ignored -> setter.accept((Integer) spinner.getValue()));
        JPanel value = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        value.add(spinner);
        value.add(new JLabel(unit));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
