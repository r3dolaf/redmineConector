package redmineconnector.service;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.awt.Component;

import redmineconnector.model.Task;
import redmineconnector.util.I18n;
import redmineconnector.ui.dialogs.ReportsDialog;

/**
 * Manages export operations (CSV) and reporting dialogs.
 */
public class ExportManager {

    private final Component parentView;
    private final Consumer<String> logger;

    public ExportManager(Component parentView, Consumer<String> logger) {
        this.parentView = parentView;
        this.logger = logger;
    }

    public void exportToCsv(List<Task> tasks) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(I18n.get("controller.csv.filename")));
        if (fc.showSaveDialog(parentView) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                pw.println(I18n.get("controller.csv.header"));
                for (Task t : tasks)
                    pw.println(t.id + ",\"" + (t.subject != null ? t.subject.replace("\"", "\"\"") : "") + "\","
                            + t.status + "," + t.priority + "," + t.assignedTo);
                log(I18n.format("controller.log.csv_saved", fc.getSelectedFile().getAbsolutePath()));
            } catch (Exception e) {
                log(I18n.format("controller.error.export", e.getMessage()));
            }
        }
    }

    public void openReportsDialog(String title, DataService service, String projectId, List<Task> tasks) {
        log(I18n.get("controller.log.reports"));
        ReportsDialog dialog = new ReportsDialog(SwingUtilities.getWindowAncestor(parentView), title, service,
                projectId, tasks);
        dialog.setVisible(true);
    }

    private void log(String m) {
        if (logger != null)
            logger.accept(m);
    }
}
