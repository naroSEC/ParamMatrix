package parammatrix.ui;

import parammatrix.model.ParameterCandidate;
import parammatrix.model.ReflectionStatus;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public final class ResultTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Method", "URL", "Parameter", "Source", "Existing", "Confidence",
            "Reflection", "Reflection Context", "Test Status"
    };
    private List<ParameterCandidate> rows = List.of();

    public void setRows(List<ParameterCandidate> rows) {
        this.rows = List.copyOf(rows);
        fireTableDataChanged();
    }

    public ParameterCandidate row(int modelRow) { return rows.get(modelRow); }
    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ParameterCandidate item = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> item.originalExchange().request().method();
            case 1 -> item.pageIdentity().displayUrl();
            case 2 -> item.name();
            case 3 -> item.sourceSummary();
            case 4 -> item.existing() ? "Yes" : "No";
            case 5 -> item.confidence();
            case 6 -> reflectionLabel(item.reflectionResult().status());
            case 7 -> item.reflectionResult().status() == ReflectionStatus.REFLECTED
                    ? item.reflectionResult().context() : "-";
            case 8 -> item.reflectionResult().status();
            default -> "";
        };
    }

    private String reflectionLabel(ReflectionStatus status) {
        return switch (status) {
            case REFLECTED -> "Yes";
            case NOT_REFLECTED -> "No";
            case NOT_TESTED -> "Not Tested";
            case ERROR -> "Error";
            case SKIPPED -> "Skipped";
        };
    }
}

