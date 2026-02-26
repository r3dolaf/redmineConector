package redmineconnector.ui.components;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import redmineconnector.model.Task;

public class TaskTableModel extends AbstractTableModel {
    String[] cols = { "ID", "Asunto", "Estado", "Prioridad", "Tipo", "Cat.", "Versión", "Asignado", "Horas", "%",
            "Fecha" };
    public List<Task> data = new ArrayList<>();
    private java.util.function.Predicate<Integer> pinChecker;

    public void setPinChecker(java.util.function.Predicate<Integer> pc) {
        this.pinChecker = pc;
    }

    public void setData(List<Task> d) {
        this.data = d;
        fireTableDataChanged();
    }

    public void updateTask(Task updated) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).id == updated.id) {
                data.set(i, updated);
                fireTableRowsUpdated(i, i);
                return;
            }
        }
        // If not found (e.g. newly created), we might want to add it,
        // but for now, let's just refresh if not found or do nothing.
    }

    public Task getTaskAt(int r) {
        if (r >= 0 && r < data.size()) {
            return data.get(r);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return cols.length;
    }

    @Override
    public String getColumnName(int c) {
        return cols[c];
    }

    @Override
    public Class<?> getColumnClass(int c) {
        switch (c) {
            case 0:
                return Integer.class;
            case 8:
                return Double.class;
            case 9:
                return Integer.class;
            case 10:
                return java.util.Date.class;
            case 11:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int r, int c) {
        Task t = data.get(r);
        switch (c) {
            case 0:
                return t.id;
            case 1:
                return t.subject;
            case 2:
                return t.status;
            case 3:
                return t.priority;
            case 4:
                return t.tracker;
            case 5:
                return t.category;
            case 6:
                return t.targetVersion;
            case 7:
                return t.assignedTo;
            case 8:
                return t.spentHours;
            case 9:
                return t.doneRatio;
            case 10:
                return t.createdOn;
            case 11:
                return pinChecker != null && pinChecker.test(t.id);
            default:
                return null;
        }
    }
}
