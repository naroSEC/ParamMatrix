package parammatrix.ui;

import parammatrix.testing.ssti.SstiTestResult;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public final class SstiResultTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Method", "URL", "Parameter", "Engine", "Result", "Detection",
            "Confidence", "Payload"
    };
    private List<SstiTestResult> rows = List.of();

    public void setRows(List<SstiTestResult> rows) {
        this.rows = List.copyOf(rows);
        fireTableDataChanged();
    }

    public SstiTestResult row(int index) { return rows.get(index); }
    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SstiTestResult result = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> result.originalExchange().request().method();
            case 1 -> result.originalExchange().request().url();
            case 2 -> result.parameter();
            case 3 -> result.templateEngine();
            case 4 -> result.status();
            case 5 -> result.detectionMethod();
            case 6 -> result.confidence();
            case 7 -> result.payload();
            default -> "";
        };
    }
}

