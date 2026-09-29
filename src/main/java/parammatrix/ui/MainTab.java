package parammatrix.ui;

import burp.api.montoya.MontoyaApi;
import parammatrix.config.ExtensionConfig;
import parammatrix.core.ExtensionController;
import parammatrix.model.ParameterCandidate;
import parammatrix.storage.ResultRepository;

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
                   ExtensionConfig config, ExtensionController controller) {
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
        tabs.addTab("Results", split);
        tabs.addTab("Scan", new HistoryScanPanel(controller));
        tabs.addTab("Settings", new JScrollPane(new SettingsPanel(config, controller.queue())));
        tabs.addTab("SSTI", new JScrollPane(new SstiSettingsPanel()));
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
