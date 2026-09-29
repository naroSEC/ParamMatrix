package parammatrix.ui;

import parammatrix.config.DatabaseStressConfig;
import parammatrix.core.ExtensionController;
import parammatrix.testing.database.DatabaseProgressListener;
import parammatrix.testing.database.DatabaseStressPayload;
import parammatrix.testing.database.DatabaseType;
import parammatrix.testing.database.SafeSyntaxPayloadProvider;

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

public final class DatabaseStressPanel extends JPanel {
    private static final Map<DatabaseType, String> DESCRIPTIONS = descriptions();
    private final ExtensionController controller;
    private final DatabaseStressConfig config;
    private final Map<DatabaseType, JCheckBox> selections = new EnumMap<>(DatabaseType.class);
    private final JLabel selectionStatus = new JLabel();
    private final JLabel runStatus = new JLabel("Ready");
    private final JProgressBar progress = new JProgressBar();
    private final JButton start = new JButton("Start DB error test");

    public DatabaseStressPanel(ExtensionController controller, DatabaseStressConfig config) {
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
        JLabel title = new JLabel("Database Error Stress Test");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 7f));
        JLabel subtitle = new JLabel(
                "Apply non-destructive syntax stress strings and compare database error behavior.");
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JPanel databases = new JPanel(new GridLayout(0, 2, 12, 12));
        for (DatabaseType database : DatabaseType.values()) {
            databases.add(databaseCard(database));
        }
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setBorder(BorderFactory.createDashedBorder(null));
        placeholder.add(new JLabel("Additional database signature families can be added here",
                SwingConstants.CENTER), BorderLayout.CENTER);
        databases.add(placeholder);
        panel.add(databases);
        panel.add(Box.createVerticalStrut(14));
        panel.add(policyPanel());
        panel.add(Box.createVerticalStrut(14));
        panel.add(payloadPreview());
        return panel;
    }

    private JPanel payloadPreview() {
        List<DatabaseStressPayload> payloads = new SafeSyntaxPayloadProvider().payloads();
        Object[][] rows = payloads.stream()
                .map(payload -> new Object[] {payload.name(), visible(payload.value()),
                        "Syntax boundary"})
                .toArray(Object[][]::new);
        return PayloadPreviewPanel.create("Payloads sent",
                "These exact short strings are tested; no SQL statements or delay payloads are used.",
                new String[] {"Name", "Payload", "Purpose"}, rows, 1);
    }

    private static String visible(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private JPanel databaseCard(DatabaseType database) {
        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(), new EmptyBorder(12, 12, 12, 12)));
        JCheckBox selected = new JCheckBox(display(database), config.isSelected(database));
        selected.setFont(selected.getFont().deriveFont(Font.BOLD,
                selected.getFont().getSize2D() + 1f));
        selected.addActionListener(ignored -> {
            config.setSelected(database, selected.isSelected());
            updateSelectionStatus();
        });
        selections.put(database, selected);
        JTextArea description = new JTextArea(DESCRIPTIONS.get(database));
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
        JPanel panel = new JPanel(new GridLayout(1, 3, 14, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Test policy"),
                new EmptyBorder(10, 10, 10, 10)));
        JPanel targets = new JPanel();
        targets.setLayout(new BoxLayout(targets, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        JRadioButton all = new JRadioButton("Test all discovered parameters",
                !config.onlyReflectedParameters.get());
        JRadioButton reflected = new JRadioButton("Test only reflected parameters",
                config.onlyReflectedParameters.get());
        all.addActionListener(ignored -> config.onlyReflectedParameters.set(false));
        reflected.addActionListener(ignored -> config.onlyReflectedParameters.set(true));
        group.add(all);
        group.add(reflected);
        targets.add(new JLabel("Targets"));
        targets.add(all);
        targets.add(reflected);

        JPanel verification = new JPanel();
        verification.setLayout(new BoxLayout(verification, BoxLayout.Y_AXIS));
        verification.add(new JLabel("Confirmation"));
        JCheckBox verify = new JCheckBox("Repeat recognized DB errors",
                config.verifyPositiveResults.get());
        verify.addActionListener(ignored -> config.verifyPositiveResults.set(verify.isSelected()));
        verification.add(verify);
        verification.add(new JLabel("Literal quotes and delimiters only"));

        JPanel limits = new JPanel();
        limits.setLayout(new BoxLayout(limits, BoxLayout.Y_AXIS));
        limits.add(new JLabel("Safety limits"));
        limits.add(spinnerRow("Requests/page", config.maximumRequestsPerPage.get(),
                1, 500, config.maximumRequestsPerPage::set));
        limits.add(spinnerRow("Delay (ms)", config.requestDelayMillis.get(),
                0, 60_000, config.requestDelayMillis::set));
        panel.add(targets);
        panel.add(verification);
        panel.add(limits);
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
                "No SQL statements, time delays, data access, or destructive payloads are used.");
        notice.setBorder(new EmptyBorder(5, 8, 8, 8));
        panel.add(notice);
        JPanel action = new JPanel(new BorderLayout(12, 0));
        start.setPreferredSize(new Dimension(190, 38));
        start.addActionListener(ignored -> startTest());
        progress.setStringPainted(true);
        progress.setString("Ready");
        action.add(start, BorderLayout.WEST);
        action.add(progress, BorderLayout.CENTER);
        panel.add(action);
        JPanel status = new JPanel(new BorderLayout());
        status.add(runStatus, BorderLayout.WEST);
        status.add(selectionStatus, BorderLayout.EAST);
        panel.add(status);
        return panel;
    }

    private void startTest() {
        if (config.selectedDatabases().isEmpty()) {
            runStatus.setText("Select at least one error signature family.");
            return;
        }
        start.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setString("Preparing...");
        controller.submitDatabaseTest(config.snapshot(), new UiProgress());
    }

    private void selectAll(boolean selected) {
        config.selectAll(selected);
        selections.values().forEach(box -> box.setSelected(selected));
        updateSelectionStatus();
    }

    private void updateSelectionStatus() {
        int count = config.selectedDatabases().size();
        selectionStatus.setText(count + " signature famil" + (count == 1 ? "y" : "ies") + " selected");
    }

    private void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run();
        else SwingUtilities.invokeLater(action);
    }

    private final class UiProgress implements DatabaseProgressListener {
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
                runStatus.setText(resultCount + " database stress results recorded");
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

    private static String display(DatabaseType database) {
        return switch (database) {
            case GENERIC -> "Generic SQL/JDBC";
            case MYSQL -> "MySQL / MariaDB";
            case POSTGRESQL -> "PostgreSQL";
            case MSSQL -> "Microsoft SQL Server";
            case ORACLE -> "Oracle";
            case SQLITE -> "SQLite";
            case DB2 -> "IBM DB2";
        };
    }

    private static Map<DatabaseType, String> descriptions() {
        Map<DatabaseType, String> values = new EnumMap<>(DatabaseType.class);
        values.put(DatabaseType.GENERIC,
                "Common JDBC, ODBC, SQLSTATE, and ORM error patterns across database stacks.");
        values.put(DatabaseType.MYSQL,
                "MySQL and MariaDB syntax, driver, and query-processing error signatures.");
        values.put(DatabaseType.POSTGRESQL,
                "PostgreSQL parser, type, and server error messages exposed in responses.");
        values.put(DatabaseType.MSSQL,
                "Microsoft SQL Server and SQL Server driver error signatures.");
        values.put(DatabaseType.ORACLE,
                "Oracle ORA errors and common Oracle JDBC exception patterns.");
        values.put(DatabaseType.SQLITE,
                "SQLite parser, query, and embedded database error messages.");
        values.put(DatabaseType.DB2,
                "IBM Db2 SQLCODE, SQLSTATE, and driver-specific error signatures.");
        return values;
    }
}
