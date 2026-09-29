package parammatrix.ui;

import parammatrix.testing.database.DatabaseTestResult;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public final class DatabaseResultTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Method", "URL", "Parameter", "Result", "Database", "Verified",
            "HTTP Status", "Length Delta", "Payload"
    };
    private List<DatabaseTestResult> rows = List.of();

    public void setRows(List<DatabaseTestResult> rows) {
        this.rows = List.copyOf(rows);
        fireTableDataChanged();
    }
    public DatabaseTestResult row(int index) { return rows.get(index); }
    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }
    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        DatabaseTestResult result = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> result.originalExchange().request().method();
            case 1 -> result.originalExchange().request().url();
            case 2 -> result.parameter();
            case 3 -> result.status();
            case 4 -> result.suspectedDatabase();
            case 5 -> result.verified() ? "Yes" : "No";
            case 6 -> result.originalStatus() + " → " + result.testStatus();
            case 7 -> result.responseLengthDelta();
            case 8 -> result.payloadName();
            default -> "";
        };
    }
}

