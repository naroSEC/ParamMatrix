package parammatrix.ui;

import parammatrix.config.ExtensionConfig;
import parammatrix.core.ActiveTaskQueue;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public final class SettingsPanel extends JPanel {
    public SettingsPanel(ExtensionConfig config, ActiveTaskQueue queue) {
        super(new BorderLayout());
        JPanel columns = new JPanel(new GridLayout(1, 3, 8, 8));
        columns.add(general(config));
        columns.add(contentAndDiscovery(config));
        columns.add(reflection(config));
        add(columns, BorderLayout.NORTH);
        add(queueControls(queue), BorderLayout.SOUTH);
    }

    private JPanel general(ExtensionConfig config) {
        JPanel panel = section("General / Safety");
        panel.add(check("Enable Auto Analysis", config.autoAnalysis.get(), config.autoAnalysis::set));
        panel.add(check("Analyze only in-scope targets", config.inScopeOnly.get(), config.inScopeOnly::set));
        panel.add(check("Ignore extension-generated requests", config.ignoreGeneratedRequests.get(),
                config.ignoreGeneratedRequests::set));
        panel.add(check("Identity ignores parameter values", config.identityIgnoresParameterValues.get(),
                config.identityIgnoresParameterValues::set));
        panel.add(spinner("Maximum response size (bytes)", config.maximumResponseBytes.get(),
                1024, 100_000_000, config.maximumResponseBytes::set));
        panel.add(spinner("Maximum candidates/page", config.maximumCandidatesPerPage.get(),
                1, 10_000, config.maximumCandidatesPerPage::set));
        panel.add(spinner("Concurrent active tests", config.concurrentActiveTests.get(),
                1, 20, config.concurrentActiveTests::set));
        panel.add(new JLabel("Concurrency changes apply after extension reload."));
        return panel;
    }

    private JPanel contentAndDiscovery(ExtensionConfig config) {
        JPanel panel = section("Content Type / Discovery");
        panel.add(check("Analyze text/html", config.analyzeHtml.get(), config.analyzeHtml::set));
        panel.add(check("Analyze application/xhtml+xml", config.analyzeXhtml.get(), config.analyzeXhtml::set));
        panel.add(check("Enable HTML Auto Detection", config.htmlAutoDetection.get(),
                config.htmlAutoDetection::set));
        panel.add(check("Ignore JSON Response", config.ignoreJsonResponses.get(),
                config.ignoreJsonResponses::set));
        panel.add(Box.createVerticalStrut(8));
        panel.add(check("HTML Form Fields", config.htmlFormFields.get(), config.htmlFormFields::set));
        panel.add(check("HTML URLs", config.htmlUrls.get(), config.htmlUrls::set));
        panel.add(check("Inline JavaScript", config.inlineJavaScript.get(), config.inlineJavaScript::set));
        panel.add(check("JavaScript HTTP Sink Analysis", config.javaScriptSinkAnalysis.get(),
                config.javaScriptSinkAnalysis::set));
        panel.add(check("Include LOW confidence", config.includeLowConfidence.get(),
                config.includeLowConfidence::set));
        return panel;
    }

    private JPanel reflection(ExtensionConfig config) {
        JPanel panel = section("Reflection");
        panel.add(check("Enable Reflection Test", config.reflectionEnabled.get(),
                config.reflectionEnabled::set));
        ButtonGroup group = new ButtonGroup();
        panel.add(radio("Batch Only", ExtensionConfig.ReflectionMode.BATCH_ONLY, config, group));
        panel.add(radio("Individual Only", ExtensionConfig.ReflectionMode.INDIVIDUAL_ONLY, config, group));
        panel.add(radio("Batch + Individual Verification",
                ExtensionConfig.ReflectionMode.BATCH_AND_VERIFY, config, group));
        panel.add(spinner("Request delay (ms)", config.requestDelayMillis.get(),
                0, 60_000, config.requestDelayMillis::set));
        panel.add(spinner("Maximum requests/page", config.maximumRequestsPerPage.get(),
                1, 1_000, config.maximumRequestsPerPage::set));
        return panel;
    }

    private JPanel queueControls(ActiveTaskQueue queue) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel status = new JLabel();
        JButton pause = new JButton("Pause");
        pause.addActionListener(event -> {
            if (queue.isPaused()) { queue.resume(); pause.setText("Pause"); }
            else { queue.pause(); pause.setText("Resume"); }
        });
        JButton clear = new JButton("Stop / Clear Pending");
        clear.addActionListener(event -> queue.clearPending());
        panel.add(pause);
        panel.add(clear);
        panel.add(status);
        Timer timer = new Timer(500, event -> status.setText(
                "Active: " + queue.activeCount() + " | Queued: " + queue.queuedCount()
                        + (queue.isPaused() ? " | PAUSED" : "")));
        timer.start();
        return panel;
    }

    private JPanel section(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private JCheckBox check(String label, boolean selected, java.util.function.Consumer<Boolean> setter) {
        JCheckBox checkBox = new JCheckBox(label, selected);
        checkBox.addActionListener(event -> setter.accept(checkBox.isSelected()));
        return checkBox;
    }

    private JRadioButton radio(String label, ExtensionConfig.ReflectionMode value,
                               ExtensionConfig config, ButtonGroup group) {
        JRadioButton button = new JRadioButton(label, config.reflectionMode == value);
        button.addActionListener(event -> config.reflectionMode = value);
        group.add(button);
        return button;
    }

    private JPanel spinner(String label, int current, int minimum, int maximum,
                           java.util.function.IntConsumer setter) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(current, minimum, maximum, 1));
        spinner.addChangeListener(event -> setter.accept((Integer) spinner.getValue()));
        row.add(new JLabel(label));
        row.add(spinner);
        return row;
    }
}

