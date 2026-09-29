package parammatrix.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

final class PayloadPreviewPanel {
    private PayloadPreviewPanel() {
    }

    static JPanel create(String title, String note, String[] columns, Object[][] rows,
                         int payloadColumn) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title), new EmptyBorder(6, 8, 8, 8)));

        JLabel description = new JLabel(note);
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(Math.max(table.getRowHeight(), 22));
        table.setFillsViewportHeight(true);
        if (payloadColumn >= 0 && payloadColumn < table.getColumnCount()) {
            table.getColumnModel().getColumn(payloadColumn).setCellRenderer(
                    new MonospaceTableCellRenderer());
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(640, 154));
        panel.add(description, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static final class MonospaceTableCellRenderer
            extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            java.awt.Component component = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            component.setFont(new Font(Font.MONOSPACED, Font.PLAIN, component.getFont().getSize()));
            return component;
        }
    }
}
