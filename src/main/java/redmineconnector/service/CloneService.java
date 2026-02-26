package redmineconnector.service;

import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import redmineconnector.model.Task;
import redmineconnector.ui.InstanceController;
import redmineconnector.ui.InstanceView;
import redmineconnector.util.I18n;
import redmineconnector.config.ConnectionConfig;
import redmineconnector.ui.NotificationService;

/**
 * Service responsible for cloning operations and checking task synchronization
 * across instances.
 */
public class CloneService {

    private final InstanceController controller;
    private final NotificationService notificationService;
    private final ConnectionConfig config;

    public CloneService(InstanceController controller, NotificationService notificationService,
            ConnectionConfig config) {
        this.controller = controller;
        this.notificationService = notificationService;
        this.config = config;
    }

    public boolean hasAllTwins(List<Task> tasks, List<InstanceController> peers) {
        if (tasks == null || tasks.isEmpty() || peers == null || peers.isEmpty())
            return false;
        for (Task t : tasks) {
            boolean foundAny = false;
            for (InstanceController peer : peers) {
                if (peer.findMatch(t, this.config) != null) {
                    foundAny = true;
                    break;
                }
            }
            if (!foundAny)
                return false;
        }
        return true;
    }

    public boolean hasMissingTwins(List<Task> tasks, List<InstanceController> peers) {
        if (tasks == null || tasks.isEmpty())
            return false;
        if (peers == null || peers.isEmpty())
            return true;
        for (Task t : tasks) {
            boolean foundAny = false;
            for (InstanceController peer : peers) {
                if (peer.findMatch(t, this.config) != null) {
                    foundAny = true;
                    break;
                }
            }
            if (!foundAny)
                return true;
        }
        return false;
    }

    public void requestClone(Task task, List<InstanceController> peers, DataService sourceService) {
        if (peers.isEmpty()) {
            notificationService.showWarning(I18n.get("controller.warn.no_peers", "No target clients available"));
            return;
        }

        InstanceController target = null;
        if (peers.size() == 1) {
            target = peers.get(0);
        } else {
            // Ask user to select target
            String[] names = peers.stream().map(InstanceController::getTitle).toArray(String[]::new);
            InstanceView view = controller.getView();
            String selected = (String) JOptionPane.showInputDialog(
                    SwingUtilities.getWindowAncestor(view),
                    I18n.get("controller.clone.select_target", "Select target client:"),
                    I18n.get("controller.clone.title", "Clone Task"),
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    names,
                    names[0]);
            if (selected == null)
                return;

            final String selectedTitle = selected;
            target = peers.stream().filter(p -> p.getTitle().equals(selectedTitle)).findFirst().orElse(null);
        }

        if (target != null) {
            target.promptClone(task, this.controller);
        }
    }
}
