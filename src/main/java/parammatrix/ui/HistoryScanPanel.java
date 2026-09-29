package parammatrix.ui;

import parammatrix.core.ExtensionController;
import parammatrix.config.SstiConfig;
import parammatrix.config.DatabaseStressConfig;
import parammatrix.scan.ScanOptions;
import parammatrix.scan.ScanProgressListener;
import parammatrix.scan.ScanSummary;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

public final class HistoryScanPanel extends JPanel {
    private final ExtensionController controller;
    private final SstiConfig sstiConfig;
    private final DatabaseStressConfig databaseConfig;
    private final JCheckBox proxyHistory = new JCheckBox("Proxy History", true);
    private final JCheckBox siteMap = new JCheckBox("Site Map / Crawl", true);
    private final JToggleButton get = new JToggleButton("GET", true);
    private final JToggleButton post = new JToggleButton("POST", true);
    private final JRadioButton discoverOnly = new JRadioButton("Discover only");
    private final JRadioButton discoverAndTest = new JRadioButton("Discover + reflection test", true);
    private final JCheckBox includeSsti = new JCheckBox("Include SSTI testing", false);
    private final JCheckBox includeDatabase = new JCheckBox("Include DB error testing", false);
    private final JTextArea exclusions = new JTextArea(8, 42);
    private final JButton start = new JButton("Start history scan");
    private final JProgressBar progress = new JProgressBar();
    private final JLabel status = new JLabel("Ready");
    private final JLabel summary = new JLabel(" ");

    public HistoryScanPanel(ExtensionController controller, SstiConfig sstiConfig,
                            DatabaseStressConfig databaseConfig) {
        super(new BorderLayout(0, 14));
        this.controller = controller;
        this.sstiConfig = sstiConfig;
        this.databaseConfig = databaseConfig;
        setBorder(new EmptyBorder(18, 20, 18, 20));
        add(header(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        start.addActionListener(ignored -> startScan());
    }

    private JPanel header() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("History Scan");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 7f));
        JLabel description = new JLabel(
                "Collect recorded traffic, filter it, then run page-scoped discovery and testing.");
        description.setBorder(new EmptyBorder(4, 0, 0, 0));
        panel.add(title);
        panel.add(description);
        return panel;
    }

    private JPanel content() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel filters = new JPanel(new GridLayout(1, 2, 12, 0));
        filters.add(sourceCard());
        filters.add(methodCard());
        content.add(filters);
        content.add(Box.createVerticalStrut(12));

        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0));
        lower.add(modeCard());
        lower.add(exclusionCard());
        content.add(lower);
        return content;
    }

    private JPanel sourceCard() {
        JPanel panel = card("Traffic sources",
                "Proxy History and Site Map entries are merged before scanning.");
        proxyHistory.setToolTipText("Messages captured in Burp Proxy history");
        siteMap.setToolTipText("Requests and responses stored in Target Site Map, including crawl data");
        panel.add(proxyHistory);
        panel.add(siteMap);
        return panel;
    }

    private JPanel methodCard() {
        JPanel panel = card("HTTP methods", "Choose which recorded requests are eligible.");
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        get.setPreferredSize(new Dimension(100, 34));
        post.setPreferredSize(new Dimension(100, 34));
        row.add(get);
        row.add(post);
        panel.add(row);
        panel.add(new JLabel("Other methods are skipped."));
        return panel;
    }

    private JPanel modeCard() {
        JPanel panel = card("Scan action", "Discovery always remains isolated to the page that produced it.");
        ButtonGroup group = new ButtonGroup();
        group.add(discoverOnly);
        group.add(discoverAndTest);
        panel.add(discoverOnly);
        panel.add(discoverAndTest);
        panel.add(includeSsti);
        panel.add(includeDatabase);
        panel.add(Box.createVerticalStrut(8));
        JLabel safety = new JLabel("Active tests respect scope, request budget, delay, and queue settings.");
        safety.setToolTipText("Configure these controls in the Settings tab");
        panel.add(safety);
        includeSsti.setToolTipText(
                "Uses the engines, target policy, delay, and request limit configured in SSTI Test");
        includeDatabase.setToolTipText(
                "Uses the signature families, target policy, delay, and request limit configured in DB Stress Test");
        return panel;
    }

    private JPanel exclusionCard() {
        JPanel panel = card("Excluded paths", "One rule per line. Plain paths, glob, and regex are supported.");
        exclusions.setFont(new Font(Font.MONOSPACED, Font.PLAIN, exclusions.getFont().getSize()));
        exclusions.setLineWrap(false);
        exclusions.setToolTipText("Examples: /logout, /static/*, regex:^/api/v[0-9]+/health$");
        JScrollPane scroll = new JScrollPane(exclusions);
        scroll.setPreferredSize(new Dimension(430, 155));
        panel.add(scroll);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        JButton common = new JButton("Add common static paths");
        common.addActionListener(ignored -> addCommonExclusions());
        actions.add(common);
        panel.add(actions);
        return panel;
    }

    private JPanel footer() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));
        JPanel actionRow = new JPanel(new BorderLayout(12, 0));
        start.setPreferredSize(new Dimension(180, 38));
        progress.setStringPainted(true);
        progress.setString("Ready");
        actionRow.add(start, BorderLayout.WEST);
        actionRow.add(progress, BorderLayout.CENTER);
        panel.add(actionRow);
        panel.add(Box.createVerticalStrut(7));
        JPanel text = new JPanel(new BorderLayout());
        text.add(status, BorderLayout.WEST);
        text.add(summary, BorderLayout.EAST);
        panel.add(text);
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

    private void startScan() {
        if (!proxyHistory.isSelected() && !siteMap.isSelected()) {
            showValidation("Select at least one traffic source.");
            return;
        }
        if (!get.isSelected() && !post.isSelected()) {
            showValidation("Select GET, POST, or both.");
            return;
        }
        List<String> rules = exclusions.getText().lines().toList();
        ScanOptions options = new ScanOptions(proxyHistory.isSelected(), siteMap.isSelected(),
                get.isSelected(), post.isSelected(), discoverAndTest.isSelected(),
                includeSsti.isSelected(), includeDatabase.isSelected(), rules);
        start.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setString("Collecting traffic...");
        status.setText("Reading Burp history");
        if (includeSsti.isSelected() && sstiConfig.selectedEngines().isEmpty()) {
            showValidation("Select at least one engine in SSTI Test.");
            start.setEnabled(true);
            return;
        }
        if (includeSsti.isSelected() && sstiConfig.onlyReflectedParameters.get()
                && discoverOnly.isSelected()) {
            showValidation("Reflected-only SSTI requires Discover + reflection test.");
            start.setEnabled(true);
            return;
        }
        if (includeDatabase.isSelected() && databaseConfig.selectedDatabases().isEmpty()) {
            showValidation("Select at least one signature family in DB Stress Test.");
            start.setEnabled(true);
            return;
        }
        if (includeDatabase.isSelected() && databaseConfig.onlyReflectedParameters.get()
                && discoverOnly.isSelected()) {
            showValidation("Reflected-only DB testing requires Discover + reflection test.");
            start.setEnabled(true);
            return;
        }
        summary.setText(" ");
        controller.submitHistoryScan(options, new UiProgress());
    }

    private void addCommonExclusions() {
        String common = "/static/*\n/assets/*\n/css/*\n/js/*\n/images/*\n/favicon.ico\n/logout";
        if (!exclusions.getText().isBlank() && !exclusions.getText().endsWith("\n")) {
            exclusions.append("\n");
        }
        exclusions.append(common);
    }

    private void showValidation(String message) {
        status.setText(message);
        progress.setIndeterminate(false);
        progress.setString("Check filters");
    }

    private void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run();
        else SwingUtilities.invokeLater(action);
    }

    private final class UiProgress implements ScanProgressListener {
        @Override public void collectionStarted() {
            onEdt(() -> status.setText("Collecting Proxy History and Site Map records..."));
        }

        @Override public void scanStarted(ScanSummary value) {
            onEdt(() -> {
                progress.setIndeterminate(false);
                progress.setMinimum(0);
                progress.setMaximum(Math.max(1, value.eligiblePages()));
                progress.setValue(0);
                progress.setString("0 / " + value.eligiblePages());
                status.setText("Scanning eligible pages");
                summary.setText(summaryText(value));
            });
        }

        @Override public void itemCompleted(int completed, int total) {
            onEdt(() -> {
                progress.setValue(completed);
                progress.setString(completed + " / " + total);
            });
        }

        @Override public void scanFinished(ScanSummary value) {
            onEdt(() -> {
                progress.setIndeterminate(false);
                progress.setValue(value.eligiblePages());
                progress.setString("Complete");
                status.setText("History scan complete");
                summary.setText(summaryText(value));
                start.setEnabled(true);
            });
        }

        @Override public void scanFailed(String message) {
            onEdt(() -> {
                progress.setIndeterminate(false);
                progress.setString("Failed");
                status.setText(message);
                start.setEnabled(true);
            });
        }

        private String summaryText(ScanSummary value) {
            return "Records: " + value.sourceRecords()
                    + "  |  Eligible: " + value.eligiblePages()
                    + "  |  Method: " + value.excludedByMethod()
                    + "  |  Path: " + value.excludedByPath()
                    + "  |  Duplicates: " + value.duplicates()
                    + "  |  No response: " + value.withoutResponse();
        }
    }
}
