package parammatrix.ui;

import burp.api.montoya.MontoyaApi;
import parammatrix.config.ExtensionConfig;
import parammatrix.config.SstiConfig;
import parammatrix.config.DatabaseStressConfig;
import parammatrix.core.ExtensionController;
import parammatrix.model.ParameterCandidate;
import parammatrix.storage.ResultRepository;
import parammatrix.storage.SstiResultRepository;
import parammatrix.storage.DatabaseResultRepository;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;

public final class MainTab extends JPanel {
    private final ResultTableModel model = new ResultTableModel();
    private final JTable table = new JTable(model);

    public MainTab(MontoyaApi api, ResultRepository repository,
                   SstiResultRepository sstiResults, DatabaseResultRepository databaseResults,
                   ExtensionConfig config, SstiConfig sstiConfig,
                   DatabaseStressConfig databaseConfig, ExtensionController controller) {
        super(new BorderLayout());
        ResultDetailPanel details = new ResultDetailPanel(api);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(new TableRowSorter<>(model));
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || table.getSelectedRow() < 0) return;
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            ParameterCandidate candidate = model.row(row);
            details.showCandidate(candidate);
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), details);
        split.setResizeWeight(0.48);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Discovery & Reflection", split);
        tabs.addTab("SSTI Results", new SstiResultsPanel(api, sstiResults));
        tabs.addTab("DB Error Results", new DatabaseResultsPanel(api, databaseResults));
        tabs.addTab("Scan", new HistoryScanPanel(controller, sstiConfig, databaseConfig));
        tabs.addTab("SSTI Test", new JScrollPane(new SstiSettingsPanel(controller, sstiConfig)));
        tabs.addTab("DB Stress Test", new JScrollPane(
                new DatabaseStressPanel(controller, databaseConfig)));
        tabs.addTab("Settings", new JScrollPane(new SettingsPanel(config, controller.queue())));
        add(tabs, BorderLayout.CENTER);

        repository.addListener(() -> SwingUtilities.invokeLater(() -> {
            model.setRows(repository.all());
            if (table.getRowCount() > 0 && table.getSelectedRow() < 0) {
                table.setRowSelectionInterval(0, 0);
            }
        }));
        api.userInterface().applyThemeToComponent(this);
    }
}
