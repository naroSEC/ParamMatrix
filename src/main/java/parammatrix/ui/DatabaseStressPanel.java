package parammatrix.ui;

import parammatrix.config.DatabaseStressConfig;
import parammatrix.core.ExtensionController;
import parammatrix.testing.database.DatabaseProgressListener;
import parammatrix.testing.database.DatabaseType;

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
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;

public final class DatabaseStressPanel extends JPanel {
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
        JPanel databases = new JPanel(new GridLayout(0, 4, 10, 10));
        databases.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Error signatures"),
                new EmptyBorder(10, 10, 10, 10)));
        for (DatabaseType database : DatabaseType.values()) {
            JCheckBox box = new JCheckBox(display(database), config.isSelected(database));
            box.setFont(box.getFont().deriveFont(Font.BOLD));
            box.addActionListener(ignored -> {
                config.setSelected(database, box.isSelected());
                updateSelectionStatus();
            });
            selections.put(database, box);
            databases.add(box);
        }
        panel.add(databases);
        panel.add(Box.createVerticalStrut(14));
        panel.add(policyPanel());
        return panel;
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

    private String display(DatabaseType database) {
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
}
