package parammatrix.ui;

import burp.api.montoya.MontoyaApi;
import parammatrix.storage.SstiResultRepository;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

public final class SstiResultsPanel extends JPanel {
    private final SstiResultTableModel model = new SstiResultTableModel();
    private final JTable table = new JTable(model);

    public SstiResultsPanel(MontoyaApi api, SstiResultRepository repository) {
        super(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        SstiResultDetailPanel details = new SstiResultDetailPanel(api);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(model));
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || table.getSelectedRow() < 0) return;
            details.showResult(model.row(table.convertRowIndexToModel(table.getSelectedRow())));
        });
        JPanel toolbar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("SSTI Test Results");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 2f));
        JButton clear = new JButton("Clear results");
        clear.addActionListener(ignored -> repository.clear());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.add(clear);
        toolbar.add(title, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), details);
        split.setResizeWeight(0.48);
        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        repository.addListener(() -> SwingUtilities.invokeLater(() -> {
            model.setRows(repository.all());
            if (table.getRowCount() > 0 && table.getSelectedRow() < 0) {
                table.setRowSelectionInterval(0, 0);
            }
        }));
    }
}
