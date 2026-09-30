package parammatrix.ui;

import parammatrix.config.SstiConfig;
import parammatrix.core.ExtensionController;
import parammatrix.http.PayloadEncodingMode;
import parammatrix.testing.ssti.SstiEngine;
import parammatrix.testing.ssti.SstiPayloadPatternCatalog;
import parammatrix.testing.ssti.SstiProgressListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SstiSettingsPanel extends JPanel {
    private static final Map<SstiEngine, String> DESCRIPTIONS = descriptions();
    private final Map<SstiEngine, JCheckBox> selections = new EnumMap<>(SstiEngine.class);
    private final ExtensionController controller;
    private final SstiConfig config;
    private final JLabel selectionStatus = new JLabel();
    private final JLabel runStatus = new JLabel("Ready");
    private final JProgressBar progress = new JProgressBar();
    private final JButton start = new JButton("Start SSTI test");

    public SstiSettingsPanel(ExtensionController controller, SstiConfig config) {
        super(new BorderLayout(0, 16));
        this.controller = controller;
        this.config = config;
        setBorder(new EmptyBorder(18, 20, 18, 20));
        add(header(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        updateSelectionStatus();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("SSTI Test");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 7f));
        JLabel subtitle = new JLabel(
                "Run non-destructive arithmetic evaluation probes against discovered parameters.");
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        copy.add(title);
        copy.add(subtitle);
        JPanel actions = new JPanel();
        JButton selectAll = new JButton("Select all");
        JButton clear = new JButton("Clear");
        selectAll.addActionListener(ignored -> selectAll(true));
        clear.addActionListener(ignored -> selectAll(false));
        actions.add(selectAll);
        actions.add(clear);
        panel.add(copy, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    private JPanel content() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        for (SstiEngine engine : SstiEngine.values()) grid.add(engineCard(engine));
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setBorder(BorderFactory.createDashedBorder(null));
        placeholder.add(new JLabel("Additional providers can implement SstiPayloadProvider",
                SwingConstants.CENTER), BorderLayout.CENTER);
        grid.add(placeholder);
        content.add(grid);
        content.add(Box.createVerticalStrut(14));
        content.add(policyPanel());
        content.add(Box.createVerticalStrut(14));
        content.add(payloadPreview());
        return content;
    }

    private JPanel payloadPreview() {
        List<Object[]> rows = new java.util.ArrayList<>();
        for (SstiEngine engine : SstiEngine.values()) {
            for (String pattern : SstiPayloadPatternCatalog.patternsFor(engine)) {
                rows.add(new Object[] {displayName(engine),
                        SstiPayloadPatternCatalog.displayPattern(pattern),
                        "Arithmetic evaluation"});
            }
        }
        return PayloadPreviewPanel.create("Logical payloads",
                "Values shown here are encoded according to Payload transport before sending.",
                new String[] {"Engine", "Payload pattern", "Detection"},
                rows.toArray(Object[][]::new), 1);
    }

    private JPanel engineCard(SstiEngine engine) {
        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(), new EmptyBorder(12, 12, 12, 12)));
        JCheckBox selected = new JCheckBox(displayName(engine), config.isSelected(engine));
        selected.setFont(selected.getFont().deriveFont(Font.BOLD,
                selected.getFont().getSize2D() + 1f));
        selected.addActionListener(ignored -> {
            config.setSelected(engine, selected.isSelected());
            updateSelectionStatus();
        });
        selections.put(engine, selected);
        JTextArea description = new JTextArea(DESCRIPTIONS.get(engine));
        description.setEditable(false);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        description.setRows(2);
        card.add(selected, BorderLayout.NORTH);
        card.add(description, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(330, 92));
        return card;
    }

    private JPanel policyPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Test policy"), new EmptyBorder(8, 10, 10, 10)));
        JPanel scope = new JPanel();
        scope.setLayout(new BoxLayout(scope, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        JRadioButton all = new JRadioButton("Test all discovered parameters",
                !config.onlyReflectedParameters.get());
        JRadioButton reflected = new JRadioButton("Test only reflected parameters",
                config.onlyReflectedParameters.get());
        all.addActionListener(ignored -> config.onlyReflectedParameters.set(false));
        reflected.addActionListener(ignored -> config.onlyReflectedParameters.set(true));
        group.add(all);
        group.add(reflected);
        scope.add(all);
        scope.add(reflected);

        JPanel limits = new JPanel();
        limits.setLayout(new BoxLayout(limits, BoxLayout.Y_AXIS));
        limits.add(new JLabel("Safety limits"));
        limits.add(spinnerRow("Maximum requests per page", config.maximumRequestsPerPage.get(),
                1, 500, config.maximumRequestsPerPage::set));
        limits.add(spinnerRow("Request delay (ms)", config.requestDelayMillis.get(),
                0, 60_000, config.requestDelayMillis::set));

        JPanel method = new JPanel();
        method.setLayout(new BoxLayout(method, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Detection method");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        method.add(title);
        method.add(new JLabel("Randomized arithmetic evaluation"));
        method.add(new JLabel("Exact marker + evaluated-result matching"));
        method.add(Box.createVerticalStrut(6));
        method.add(new JLabel("Payload transport"));
        JComboBox<PayloadEncodingMode> encoding = new JComboBox<>(PayloadEncodingMode.values());
        encoding.setSelectedItem(config.payloadEncodingMode());
        encoding.addActionListener(ignored -> config.setPayloadEncodingMode(
                (PayloadEncodingMode) encoding.getSelectedItem()));
        method.add(encoding);
        method.add(new JLabel("Auto encodes URL and form values once"));
        panel.add(scope, BorderLayout.WEST);
        panel.add(limits, BorderLayout.CENTER);
        panel.add(method, BorderLayout.EAST);
        return panel;
    }

    private JPanel spinnerRow(String label, int value, int minimum, int maximum,
                              java.util.function.IntConsumer setter) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, minimum, maximum, 1));
        spinner.addChangeListener(ignored -> setter.accept((Integer) spinner.getValue()));
        row.add(new JLabel(label));
        row.add(spinner);
        return row;
    }

    private JPanel footer() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));
        JLabel notice = new JLabel(
                "Only non-destructive arithmetic payloads are sent. Burp scope and queue controls apply.");
        notice.setBorder(new EmptyBorder(5, 8, 8, 8));
        panel.add(notice);
        JPanel action = new JPanel(new BorderLayout(12, 0));
        start.setPreferredSize(new Dimension(180, 38));
        start.addActionListener(ignored -> startTest());
        progress.setStringPainted(true);
        progress.setString("Ready");
        action.add(start, BorderLayout.WEST);
        action.add(progress, BorderLayout.CENTER);
        panel.add(action);
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.add(runStatus, BorderLayout.WEST);
        statusRow.add(selectionStatus, BorderLayout.EAST);
        panel.add(statusRow);
        return panel;
    }

    private void startTest() {
        if (config.selectedEngines().isEmpty()) {
            runStatus.setText("Select at least one template engine.");
            return;
        }
        start.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setString("Preparing...");
        controller.submitSstiTest(config.snapshot(), new UiProgress());
    }

    private void selectAll(boolean selected) {
        config.selectAll(selected);
        selections.forEach((engine, checkBox) -> checkBox.setSelected(selected));
        updateSelectionStatus();
    }

    private void updateSelectionStatus() {
        long count = config.selectedEngines().size();
        selectionStatus.setText(count + " engine" + (count == 1 ? "" : "s") + " selected");
    }

    private void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run();
        else SwingUtilities.invokeLater(action);
    }

    private final class UiProgress implements SstiProgressListener {
        @Override public void started(int pages, int parameters) {
            onEdt(() -> {
                progress.setIndeterminate(false);
                progress.setMinimum(0);
                progress.setMaximum(Math.max(1, pages));
                progress.setValue(0);
                progress.setString("0 / " + pages + " pages");
                runStatus.setText(parameters + " eligible parameters across " + pages + " pages");
            });
        }
        @Override public void pageCompleted(int completed, int total) {
            onEdt(() -> {
                progress.setValue(completed);
                progress.setString(completed + " / " + total + " pages");
            });
        }
        @Override public void finished(int resultCount) {
            onEdt(() -> {
                progress.setIndeterminate(false);
                progress.setValue(progress.getMaximum());
                progress.setString("Complete");
                runStatus.setText(resultCount + " SSTI test results recorded");
                start.setEnabled(true);
            });
        }
        @Override public void failed(String message) {
            onEdt(() -> {
                progress.setIndeterminate(false);
                progress.setString("Failed");
                runStatus.setText(message);
                start.setEnabled(true);
            });
        }
    }

    private static String displayName(SstiEngine engine) {
        return switch (engine) {
            case GENERIC -> "Generic";
            case JINJA2 -> "Jinja2";
            case TWIG -> "Twig";
            case FREEMARKER -> "FreeMarker";
            case VELOCITY -> "Velocity";
            case THYMELEAF -> "Thymeleaf";
            case SMARTY -> "Smarty";
        };
    }

    private static Map<SstiEngine, String> descriptions() {
        Map<SstiEngine, String> values = new EnumMap<>(SstiEngine.class);
        values.put(SstiEngine.GENERIC, "Common curly-brace and dollar-expression evaluation probes.");
        values.put(SstiEngine.JINJA2, "Python/Jinja arithmetic expression behavior.");
        values.put(SstiEngine.TWIG, "PHP/Twig arithmetic expression behavior.");
        values.put(SstiEngine.FREEMARKER, "Java FreeMarker dollar-expression evaluation.");
        values.put(SstiEngine.VELOCITY, "Apache Velocity safe variable assignment and evaluation.");
        values.put(SstiEngine.THYMELEAF, "Thymeleaf inline standard-expression evaluation.");
        values.put(SstiEngine.SMARTY, "PHP Smarty arithmetic delimiter evaluation.");
        return values;
    }
}
