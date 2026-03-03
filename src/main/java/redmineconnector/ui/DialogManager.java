package redmineconnector.ui;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import redmineconnector.util.I18n;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import redmineconnector.config.ConnectionConfig;
import redmineconnector.model.SimpleEntity;
import redmineconnector.model.Task;
import redmineconnector.service.DataService;
import redmineconnector.ui.dialogs.StatusColorDialog;
import redmineconnector.ui.dialogs.TaskFormDialog;
import redmineconnector.model.UploadToken;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

/**
 * Handles the creation and display of dialog windows for the application.
 * Extracted from InstanceController.
 */
public class DialogManager {
    private final InstanceController controller;
    private final InstanceView view;
    private final ConnectionConfig config;

    public DialogManager(InstanceController controller, InstanceView view, ConnectionConfig config) {
        this.controller = controller;
        this.view = view;
        this.config = config;
    }

    public void openCreateDialog(Task template) {
        openCreateDialog(template, null, 0);
    }

    public void openCreateDialog(Task template, DataService sourceService) {
        openCreateDialog(template, sourceService, 0);
    }

    public void openCreateDialog(Task template, DataService sourceService, int originalTaskId) {
        openCreateDialog(template, sourceService, originalTaskId, null);
    }

    public void openCreateDialog(Task template, DataService sourceService, int originalTaskId,
            java.util.function.Consumer<Integer> onSuccess) {
        // Create template if null (for new task creation)
        final Task finalTemplate = (template == null) ? new Task() : template;

        boolean isEdit = (finalTemplate.id > 0 && sourceService == null);

        // Set Default User for new tasks
        SimpleEntity currentUser = controller.getCurrentUser();

        if (finalTemplate.id == 0 && currentUser != null) {
            if (finalTemplate.assignedToId == 0) {
                finalTemplate.assignedToId = currentUser.id;
                finalTemplate.assignedTo = currentUser.name;
            }
        }

        // Set Default Status for new tasks (including cloned tasks)
        if (finalTemplate.id == 0 && finalTemplate.statusId == 0) {
            List<SimpleEntity> statuses = controller.getStatuses();
            if (statuses != null) {
                for (SimpleEntity status : statuses) {
                    if (status.name.equalsIgnoreCase("Nuevo") ||
                            status.name.equalsIgnoreCase("New") ||
                            status.name.equalsIgnoreCase("Nueva")) {
                        finalTemplate.statusId = status.id;
                        finalTemplate.status = status.name;
                        controller.log("Estado por defecto establecido a: " + status.name);
                        break;
                    }
                }
            }
        }

        List<SimpleEntity> parentCandidates = new ArrayList<>();
        List<Task> epics = controller.getEpicTasks();
        if (epics != null) {
            for (Task et : epics) {
                parentCandidates.add(new SimpleEntity(et.id, et.subject));
            }
        }

        // Accessing metadata lists via getters from controller
        TaskFormDialog dialog = new TaskFormDialog(SwingUtilities.getWindowAncestor(view),
                isEdit ? I18n.get("dialog.create.title.edit") : I18n.get("dialog.create.title.new"),
                controller.getUsers(),
                controller.getTrackers(),
                controller.getPriorities(),
                controller.getStatuses(),
                controller.getCategories(),
                controller.getVersions(),
                controller.getActivities(),
                parentCandidates,
                config.url,
                controller.getCurrentProject(),
                isEdit);

        // --- Heuristic Logic Start ---
        redmineconnector.service.HeuristicManager heuristicMgr = new redmineconnector.service.HeuristicManager();

        // 1. Load persisted known valid statuses
        Map<Integer, List<SimpleEntity>> heuristic = heuristicMgr.load();
        if (heuristic == null)
            heuristic = new HashMap<>();

        // 2. Augment with current tasks (Learning)
        List<Task> currentTasks = controller.getTasks();
        if (currentTasks != null) {
            Map<String, List<String>> trackerToStatusMap = new HashMap<>();

            for (Task t : currentTasks) {
                if (t.tracker != null && t.status != null) {
                    trackerToStatusMap.computeIfAbsent(t.tracker.trim(), k -> new ArrayList<>()).add(t.status.trim());
                }
            }

            boolean updatedAny = false;
            SimpleEntity tracker = null;
            for (SimpleEntity tr : controller.getTrackers()) {
                if (tr.id == finalTemplate.trackerId) {
                    tracker = tr;
                    break;
                }
            }
            if (tracker == null && controller.getTrackers().size() > 0)
                tracker = controller.getTrackers().get(0);

            List<SimpleEntity> allStatuses = controller.getStatuses();

            if (tracker != null && allStatuses != null) {
                // Get existing known statuses for this tracker ID
                List<SimpleEntity> knownList = heuristic.get(tracker.id);
                Set<Integer> knownIds = new HashSet<>();
                if (knownList != null) {
                    for (SimpleEntity s : knownList)
                        knownIds.add(s.id);
                } else {
                    knownList = new ArrayList<>();
                }

                Set<SimpleEntity> currentValid = new HashSet<>(knownList);

                // Add "New" variants if missing
                for (SimpleEntity s : allStatuses) {
                    String sn = s.name.trim();
                    if ((sn.equalsIgnoreCase("Nueva") || sn.equalsIgnoreCase("New") || sn.equalsIgnoreCase("Nuevo"))
                            && !knownIds.contains(s.id)) {
                        currentValid.add(s);
                        updatedAny = true;
                    }
                }

                // Add observed statuses from current tasks
                if (trackerToStatusMap.containsKey(tracker.name.trim())) {
                    List<String> seenStatuses = trackerToStatusMap.get(tracker.name.trim());
                    if (seenStatuses != null) {
                        for (String sName : seenStatuses) {
                            for (SimpleEntity s : allStatuses) {
                                if (s.name.trim().equalsIgnoreCase(sName) && !knownIds.contains(s.id)) {
                                    currentValid.add(s);
                                    updatedAny = true;
                                }
                            }
                        }
                    }
                }

                // Put back into map
                if (!currentValid.isEmpty()) {
                    List<SimpleEntity> finalValidList = new ArrayList<>(currentValid);
                    finalValidList.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
                    heuristic.put(tracker.id, finalValidList);
                }
            }

            // 3. Save if we learned something new
            if (updatedAny) {
                heuristicMgr.save(heuristic);
                controller.log("Heurística de estados actualizada y guardada.");
            }
        }

        dialog.setStatusHeuristic(heuristic);
        // --- Heuristic Logic End ---

        if (currentUser != null) {
            dialog.setDefaultTimeUser(currentUser);
        }

        dialog.fill(finalTemplate);
        if (!isEdit && sourceService != null && config.refPattern != null && originalTaskId > 0) {
            // Cloning logic
            dialog.setSourceDataService(sourceService);
            try {
                // dialog.setDescription(currentDesc + "\n\n" + ref);
            } catch (Exception ex) {
                controller.log("Error formatting reference: " + ex.getMessage());
            }
        }

        dialog.onUpload(file -> {
            try {
                byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
                String mime = java.nio.file.Files.probeContentType(file.toPath());
                if (mime == null)
                    mime = "application/octet-stream";
                controller.log("Subiendo archivo desde diálogo: " + file.getName());
                String token = controller.getService().uploadFile(data, mime);
                return new UploadToken(token, file.getName(), mime);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage());
            }
        });

        // Configurar DataService para descarga automática de imágenes
        dialog.setDataService(controller.getService());
        dialog.setAsyncDataService(controller.getAsyncService());

        dialog.onDownload(att -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File(att.filename));
            Object[] options = { I18n.get("task.form.attach.opt.download"), I18n.get("task.form.attach.opt.preview"),
                    I18n.get("task.form.attach.opt.browser") };
            int selection = JOptionPane.showOptionDialog(dialog,
                    I18n.format("task.form.attach.download.msg", att.filename),
                    I18n.get("task.form.attach.download.title"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (selection == 2) {
                if (att.contentUrl != null && !att.contentUrl.isEmpty()) {
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(att.contentUrl));
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(dialog, I18n.format("task.form.error.browser", e.getMessage()));
                    }
                }
                return;
            }

            if (selection == 0 || selection == 1) {
                boolean isPreview = (selection == 1);
                java.io.File destFile = null;
                if (isPreview) {
                    String tmpDir = System.getProperty("java.io.tmpdir");
                    destFile = new java.io.File(tmpDir, att.filename);
                } else {
                    if (fc.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                        destFile = fc.getSelectedFile();
                    } else {
                        return;
                    }
                }

                if (destFile != null) {
                    java.io.File finalDest = destFile;
                    view.setLoading(true);
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            byte[] data = controller.getService().downloadAttachment(att);
                            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(finalDest)) {
                                fos.write(data);
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            view.setLoading(false);
                            try {
                                get();
                                if (isPreview) {
                                    if (java.awt.Desktop.isDesktopSupported()) {
                                        java.awt.Desktop.getDesktop().open(finalDest);
                                    } else {
                                        JOptionPane.showMessageDialog(dialog,
                                                I18n.format("task.form.attach.downloaded",
                                                        finalDest.getAbsolutePath()));
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(dialog,
                                            I18n.format("task.form.attach.downloaded", finalDest.getAbsolutePath()));
                                }
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(dialog,
                                        I18n.format("task.form.error.download", e.getMessage()));
                            }
                        }
                    }.execute();
                }
            }
        });

        dialog.onSave(t -> {
            if (t.id == 0) {
                controller.performCreate(t, sourceService, onSuccess);
            } else {
                controller.performUpdate(t);
                // Forzar refresh para asegurar que la vista se actualiza
                SwingUtilities.invokeLater(() -> {
                    controller.log("Refrescando vista después de editar tarea #" + t.id);
                    // Pequeño delay para asegurar que el update en servidor se completó
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    controller.partialRefresh(t.id);
                });
            }
        });

        // Log Time Handler
        dialog.onLogTime((date, hours, userId, activityId, comment) -> {
            controller.onLogTime(finalTemplate.id, date, hours, userId, activityId, comment);
        });

        dialog.setVisible(true);
    }

    public void openBulkUpdateDialog() {
        int[] rows = view.table.getSelectedRows();
        if (rows.length < 2) {
            JOptionPane.showMessageDialog(view, I18n.get("dialog.bulk.msg.select"));
            return;
        }
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(view), I18n.get("dialog.bulk.title"),
                Dialog.ModalityType.MODELESS);
        d.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // UI Components for Bulk Update
        JCheckBox chkStatus = new JCheckBox(I18n.get("dialog.bulk.label.status"));
        JComboBox<SimpleEntity> cbStatus = new JComboBox<>();
        List<SimpleEntity> statuses = controller.getStatuses();
        if (statuses != null)
            statuses.forEach(cbStatus::addItem);
        cbStatus.setEnabled(false);

        JCheckBox chkPriority = new JCheckBox(I18n.get("dialog.bulk.label.priority"));
        JComboBox<SimpleEntity> cbPriority = new JComboBox<>();
        List<SimpleEntity> priorities = controller.getPriorities();
        if (priorities != null)
            priorities.forEach(cbPriority::addItem);
        cbPriority.setEnabled(false);

        JCheckBox chkAssign = new JCheckBox(I18n.get("dialog.bulk.label.assign"));
        JComboBox<SimpleEntity> cbAssign = new JComboBox<>();
        List<SimpleEntity> users = controller.getUsers();
        if (users != null)
            users.forEach(cbAssign::addItem);
        cbAssign.setEnabled(false);
        // Pre-select current user if available
        SimpleEntity currentUser = controller.getCurrentUser();
        if (currentUser != null) {
            for (int i = 0; i < cbAssign.getItemCount(); i++) {
                if (cbAssign.getItemAt(i).id == currentUser.id) {
                    cbAssign.setSelectedIndex(i);
                    break;
                }
            }
        }

        JCheckBox chkCategory = new JCheckBox(I18n.get("dialog.bulk.label.category"));
        JComboBox<SimpleEntity> cbCategory = new JComboBox<>();
        List<SimpleEntity> categories = controller.getCategories();
        if (categories != null)
            categories.forEach(cbCategory::addItem);
        cbCategory.setEnabled(false);

        JCheckBox chkVersion = new JCheckBox(I18n.get("dialog.bulk.label.version"));
        JComboBox<SimpleEntity> cbVersion = new JComboBox<>();
        cbVersion.addItem(null);
        List<SimpleEntity> versions = controller.getVersions();
        if (versions != null) {
            for (SimpleEntity v : versions)
                cbVersion.addItem(v);
        }
        cbVersion.setEnabled(false);

        JCheckBox chkDoneRatio = new JCheckBox(I18n.get("dialog.bulk.label.ratio"));
        JComboBox<String> cbDoneRatio = new JComboBox<>(
                new String[] { "0 %", "10 %", "20 %", "30 %", "40 %", "50 %", "60 %", "70 %", "80 %", "90 %",
                        "100 %" });
        cbDoneRatio.setEditable(true);
        cbDoneRatio.setEnabled(false);

        JTextArea txtComment = new JTextArea(3, 30);
        txtComment.setBorder(javax.swing.BorderFactory.createTitledBorder(I18n.get("dialog.bulk.label.comment")));

        // Listeners for checkboxes
        chkStatus.addActionListener(e -> {
            cbStatus.setEnabled(chkStatus.isSelected());
            if (chkStatus.isSelected()) {
                SimpleEntity s = (SimpleEntity) cbStatus.getSelectedItem();
                if (s != null && controller.isClosedStatus(s.name)) {
                    chkDoneRatio.setSelected(true);
                    cbDoneRatio.setEnabled(true);
                    cbDoneRatio.setSelectedItem("100 %");
                }
            }
        });
        chkPriority.addActionListener(e -> cbPriority.setEnabled(chkPriority.isSelected()));
        chkAssign.addActionListener(e -> cbAssign.setEnabled(chkAssign.isSelected()));
        chkCategory.addActionListener(e -> cbCategory.setEnabled(chkCategory.isSelected()));
        chkVersion.addActionListener(e -> cbVersion.setEnabled(chkVersion.isSelected()));
        chkDoneRatio.addActionListener(e -> cbDoneRatio.setEnabled(chkDoneRatio.isSelected()));

        cbStatus.addActionListener(e -> {
            if (chkStatus.isSelected()) {
                SimpleEntity s = (SimpleEntity) cbStatus.getSelectedItem();
                if (s != null && controller.isClosedStatus(s.name)) {
                    chkDoneRatio.setSelected(true);
                    cbDoneRatio.setEnabled(true);
                    cbDoneRatio.setSelectedItem("100 %");
                }
            }
        });

        // Add to Layout
        gbc.gridx = 0;
        form.add(chkStatus, gbc);
        gbc.gridx = 1;
        form.add(cbStatus, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        form.add(chkPriority, gbc);
        gbc.gridx = 1;
        form.add(cbPriority, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        form.add(chkAssign, gbc);
        gbc.gridx = 1;
        form.add(cbAssign, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        form.add(chkCategory, gbc);
        gbc.gridx = 1;
        form.add(cbCategory, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        form.add(chkVersion, gbc);
        gbc.gridx = 1;
        form.add(cbVersion, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        form.add(chkDoneRatio, gbc);
        gbc.gridx = 1;
        form.add(cbDoneRatio, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        form.add(new JScrollPane(txtComment), gbc);

        JButton btnApply = new JButton(I18n.get("dialog.bulk.btn.apply"));
        btnApply.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        btnApply.addActionListener(ev -> {
            if (JOptionPane.showConfirmDialog(d,
                    I18n.format("dialog.bulk.confirm.msg", rows.length), I18n.get("dialog.bulk.confirm.title"),
                    JOptionPane.YES_NO_OPTION) == 0) {
                d.dispose();
                Integer ratio = null;
                if (chkDoneRatio.isSelected()) {
                    String str = (String) cbDoneRatio.getSelectedItem();
                    if (str != null) {
                        try {
                            ratio = Integer.parseInt(str.replace("%", "").trim());
                        } catch (Exception ex) {
                        }
                    }
                }

                // Prepare updates object/map
                // Since performUpdate takes a Task, we might need a BulkUpdate logic in
                // controller
                // or iterate here.
                // The original code passed specific fields to performBulkUpdate.

                // Let's call controller.performBulkUpdate which we might need to expose.
                // For now, assume we collect the data and pass it back.

                SimpleEntity status = chkStatus.isSelected() ? (SimpleEntity) cbStatus.getSelectedItem() : null;
                SimpleEntity priority = chkPriority.isSelected() ? (SimpleEntity) cbPriority.getSelectedItem() : null;
                SimpleEntity assign = chkAssign.isSelected() ? (SimpleEntity) cbAssign.getSelectedItem() : null;
                SimpleEntity category = chkCategory.isSelected() ? (SimpleEntity) cbCategory.getSelectedItem() : null;
                SimpleEntity version = chkVersion.isSelected() ? (SimpleEntity) cbVersion.getSelectedItem() : null;
                String comment = txtComment.getText().trim();

                controller.performBulkUpdate(rows, status, priority, assign, category, version, ratio, comment);
            }
        });

        d.add(form, BorderLayout.CENTER);
        JPanel pnl = new JPanel();
        pnl.add(btnApply);
        d.add(pnl, BorderLayout.SOUTH);

        d.pack();
        d.setLocationRelativeTo(view);
        d.setVisible(true);
    }

    public void openColorConfigDialog() {
        controller.log("Abriendo diálogo de colores.");
        StatusColorDialog d = new StatusColorDialog((Frame) SwingUtilities.getWindowAncestor(view),
                controller.getConfigPrefix(), controller.getTitle(),
                controller.getStatuses());
        d.onSave(controller::reloadConfig);
        d.setVisible(true);
    }

    public void showVersionManager() {
        controller.openVersionManager();
    }

    public void showWikiManager() {
        controller.openWikiManager();
    }

    public void openConfigDialog() {
        controller.openConfigDialog();
    }

    public void openMultiCloseDialog() {
        int[] rows = view.table.getSelectedRows();
        if (rows.length < 1) {
            JOptionPane.showMessageDialog(view, I18n.get("dialog.multiclose.msg.select"));
            return;
        }

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(view),
                I18n.get("version.dialog.multiclose.title"),
                Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout());
        UIHelper.addEscapeListener(d);

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        pnl.add(new javax.swing.JLabel(I18n.get("version.label.target")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JComboBox<SimpleEntity> cbVersions = new JComboBox<>();
        cbVersions.addItem(new SimpleEntity(-1, I18n.get("version.combo.no_change")));
        cbVersions.addItem(new SimpleEntity(0, I18n.get("version.combo.empty")));
        if (controller.getVersions() != null) {
            controller.getVersions().forEach(cbVersions::addItem);
        }
        pnl.add(cbVersions, gbc);

        // Add status selector with tracker-based filtering
        gbc.gridy++;
        gbc.gridx = 0;

        gbc.gridx = 1;
        JComboBox<SimpleEntity> cbStatuses = new JComboBox<>();
        cbStatuses.addItem(new SimpleEntity(-1, I18n.get("dialog.multiclose.status.loading")));
        cbStatuses.setEnabled(false);

        // Get tracker and ID from first selected task to filter statuses
        Task firstTask = view.model.getTaskAt(view.table.convertRowIndexToModel(rows[0]));
        int trackerId = firstTask.trackerId;
        int firstIssueId = firstTask.id;

        controller.getAsyncService()
                .fetchAllowedStatusesAsync(controller.getConfig().projectId, trackerId, firstIssueId)
                .thenAccept(allowed -> SwingUtilities.invokeLater(() -> {
                    cbStatuses.removeAllItems();
                    if (allowed == null || allowed.isEmpty()) {
                        // Fallback to general statuses if fetch fails or returns empty
                        if (controller.getStatuses() != null) {
                            controller.getStatuses().forEach(cbStatuses::addItem);
                        }
                    } else {
                        allowed.forEach(cbStatuses::addItem);
                    }
                    cbStatuses.setEnabled(true);

                    // Try to pre-select a closed status
                    for (int i = 0; i < cbStatuses.getItemCount(); i++) {
                        SimpleEntity s = cbStatuses.getItemAt(i);
                        if (controller.isClosedStatus(s.name)) {
                            cbStatuses.setSelectedItem(s);
                            break;
                        }
                    }
                })).exceptionally(err -> {
                    SwingUtilities.invokeLater(() -> {
                        cbStatuses.removeAllItems();
                        if (controller.getStatuses() != null) {
                            controller.getStatuses().forEach(cbStatuses::addItem);
                        }
                        cbStatuses.setEnabled(true);
                    });
                    return null;
                });
        pnl.add(cbStatuses, gbc);

        // Add Assignment Selector
        gbc.gridy++;
        gbc.gridx = 0;
        pnl.add(new javax.swing.JLabel(I18n.get("task.form.label.assigned")), gbc);

        gbc.gridx = 1;
        JComboBox<SimpleEntity> cbAssignment = new JComboBox<>();
        // Option: No Change (-1)
        cbAssignment.addItem(new SimpleEntity(-1, I18n.get("dialog.assignment.no_change")));
        // Option: Nobody (0)
        cbAssignment.addItem(new SimpleEntity(0, I18n.get("dialog.assignment.nobody")));
        // Option: Author (-2)
        cbAssignment.addItem(new SimpleEntity(-2, I18n.get("dialog.assignment.author")));
        // Users list
        if (controller.getUsers() != null) {
            controller.getUsers().forEach(cbAssignment::addItem);
        }
        // Default to "Assign to Nobody" as per original legacy behavior, OR
        // "Unchanged"?
        // Legacy behavior was forceful unassign. To keep it close but flexible, let's
        // select "Nobody" by default?
        // Or "Unchanged"?
        // User request: "añadir opcion para asignar a nadie o al usuario creador"
        // Implicitly, maybe they want control.
        // Let's default to "Nobody" to match previous behavior (forcing 0), so user
        // doesn't accidentally keep assignments they wanted to clear.
        cbAssignment.setSelectedIndex(1); // "Nadie"

        pnl.add(cbAssignment, gbc);

        JButton btnClose = new JButton(I18n.get("version.btn.save"));
        btnClose.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(d,
                    I18n.format("version.msg.multiclose_confirm", rows.length),
                    I18n.get("version.msg.confirm.title"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                d.dispose();
                SimpleEntity v = (SimpleEntity) cbVersions.getSelectedItem();
                if (v != null && v.id == -1)
                    v = null;

                // Get selected status
                SimpleEntity selectedStatus = (SimpleEntity) cbStatuses.getSelectedItem();

                // Get assignment choice
                SimpleEntity assignment = (SimpleEntity) cbAssignment.getSelectedItem();

                // Convert rows to List<Task>
                java.util.List<Task> tasks = new java.util.ArrayList<>();
                for (int r : rows) {
                    try {
                        tasks.add(view.model.getTaskAt(view.table.convertRowIndexToModel(r)));
                    } catch (Exception ex) {
                        // ignore if row invalid
                    }
                }

                controller.performMultiClose(tasks, v, selectedStatus, assignment);
            }
        });

        JPanel pnlBot = new JPanel();
        pnlBot.add(btnClose);

        d.add(pnl, BorderLayout.CENTER);
        d.add(pnlBot, BorderLayout.SOUTH);

        d.pack();
        d.setLocationRelativeTo(view);
        d.setVisible(true);
    }

    public void openTwinClosureDialog(List<Task> twinsToClose, InstanceController source) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(view),
                I18n.get("twin.dialog.title"),
                Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout());
        UIHelper.addEscapeListener(d);

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        pnl.add(new javax.swing.JLabel(I18n.format("twin.msg.confirm", twinsToClose.size(), controller.getTitle())),
                gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        pnl.add(new javax.swing.JLabel(I18n.format("twin.label.version", controller.getTitle())), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JComboBox<SimpleEntity> cbVersions = new JComboBox<>();
        cbVersions.addItem(new SimpleEntity(-1, I18n.get("version.combo.no_change")));
        cbVersions.addItem(new SimpleEntity(0, I18n.get("version.combo.empty")));
        if (controller.getVersions() != null) {
            controller.getVersions().forEach(cbVersions::addItem);
        }
        pnl.add(cbVersions, gbc);

        gbc.gridy++;
        gbc.gridx = 0;

        gbc.gridx = 1;
        JComboBox<SimpleEntity> cbStatuses = new JComboBox<>();
        cbStatuses.addItem(new SimpleEntity(-1, I18n.get("dialog.multiclose.status.loading")));
        cbStatuses.setEnabled(false);

        // Obtener el tracker y el ID de la primera tarea gemela para filtrar
        int trackerId = twinsToClose.get(0).trackerId;
        int firstIssueId = twinsToClose.get(0).id;
        controller.getAsyncService()
                .fetchAllowedStatusesAsync(controller.getConfig().projectId, trackerId, firstIssueId)
                .thenAccept(allowed -> SwingUtilities.invokeLater(() -> {
                    cbStatuses.removeAllItems();
                    if (allowed == null || allowed.isEmpty()) {
                        // Fallback a los estados generales si falla o no hay específicos
                        if (controller.getStatuses() != null) {
                            controller.getStatuses().forEach(cbStatuses::addItem);
                        }
                    } else {
                        allowed.forEach(cbStatuses::addItem);
                    }
                    cbStatuses.setEnabled(true);

                    // Intentar pre-seleccionar un estado de cierre
                    for (int i = 0; i < cbStatuses.getItemCount(); i++) {
                        SimpleEntity s = cbStatuses.getItemAt(i);
                        if (controller.isClosedStatus(s.name)) {
                            cbStatuses.setSelectedItem(s);
                            break;
                        }
                    }
                })).exceptionally(err -> {
                    SwingUtilities.invokeLater(() -> {
                        cbStatuses.removeAllItems();
                        if (controller.getStatuses() != null) {
                            controller.getStatuses().forEach(cbStatuses::addItem);
                        }
                        cbStatuses.setEnabled(true);
                    });
                    return null;
                });
        pnl.add(cbStatuses, gbc);

        // Add Assignment Selector (New for Twin Closure - Matching MultiClose)
        gbc.gridy++;
        gbc.gridx = 0;
        pnl.add(new javax.swing.JLabel(I18n.get("task.form.label.assigned")), gbc);

        gbc.gridx = 1;
        JComboBox<SimpleEntity> cbAssignment = new JComboBox<>();
        // Option: No Change (-1)
        cbAssignment.addItem(new SimpleEntity(-1, I18n.get("dialog.assignment.no_change")));
        // Option: Nobody (0)
        cbAssignment.addItem(new SimpleEntity(0, I18n.get("dialog.assignment.nobody")));
        // Option: Author (-2)
        cbAssignment.addItem(new SimpleEntity(-2, I18n.get("dialog.assignment.author")));
        // Users list
        if (controller.getUsers() != null) {
            controller.getUsers().forEach(cbAssignment::addItem);
        }
        // Default to "Nobody" to match old implicit behavior?
        // Old behavior was: assign to Author if found.
        // To minimize disruption, let's select "Author" (-2) by default?
        // Or "Nobody" to be safe?
        // Let's set to "Nadie" (1) as requested by user ("añadir opcion...").
        // Defaulting to "Nadie" is safer than "Author" if they didn't want
        // reassignment.
        cbAssignment.setSelectedIndex(1);

        pnl.add(cbAssignment, gbc);

        JButton btnClose = new JButton(I18n.get("version.btn.save"));
        btnClose.addActionListener(e -> {
            d.dispose();
            SimpleEntity v = (SimpleEntity) cbVersions.getSelectedItem();
            if (v != null && v.id == -1)
                v = null;
            SimpleEntity s = (SimpleEntity) cbStatuses.getSelectedItem();

            // Get Assignment
            SimpleEntity assignment = (SimpleEntity) cbAssignment.getSelectedItem();

            controller.performTwinClosure(twinsToClose, v, s, assignment, source);
        });

        JButton btnCancel = new JButton("Omitir");
        btnCancel.addActionListener(e -> d.dispose());

        JPanel pnlBot = new JPanel();
        pnlBot.add(btnClose);
        pnlBot.add(btnCancel);

        d.add(pnl, BorderLayout.CENTER);
        d.add(pnlBot, BorderLayout.SOUTH);

        d.pack();
        d.setLocationRelativeTo(view);
        d.setVisible(true);
    }
}
