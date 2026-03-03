package redmineconnector.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;

import redmineconnector.util.I18n;
import redmineconnector.util.LoggerUtil;

import redmineconnector.config.ConnectionConfig;
import redmineconnector.model.Attachment;
import redmineconnector.model.SimpleEntity;
import redmineconnector.model.Task;
import redmineconnector.model.CustomField;
import redmineconnector.model.CustomFieldDefinition;
import redmineconnector.model.UploadToken;
import redmineconnector.service.DataService;
import redmineconnector.service.CustomFieldsCache;

/**
 * Encapsula operaciones relacionadas con tareas (Crear, Actualizar, Clonar,
 * Descargar)
 * para desacoplar la lógica del InstanceController.
 */
public class TaskOperations {
    private final InstanceController controller;
    private final InstanceView view;
    private final DataService service;
    private final ConnectionConfig config;
    private final DialogManager dialogs;
    private final NotificationService notifications;

    public TaskOperations(InstanceController controller, InstanceView view, DataService service,
            ConnectionConfig config, DialogManager dialogs, NotificationService notifications) {
        this.controller = controller;
        this.view = view;
        this.service = service;
        this.config = config;
        this.dialogs = dialogs;
        this.notifications = notifications;
    }

    public void performCreate(Task t) {
        performCreate(t, null); // Llamada sobrecargada por defecto
    }

    public void performCreate(Task t, DataService src) {
        performCreate(t, src, null);
    }

    public void performCreate(Task t, DataService src, java.util.function.Consumer<Integer> onSuccess) {
        view.setLoading(true);
        controller.log(I18n.format("op.log.create.start", t.subject));

        java.util.concurrent.CompletableFuture<Integer> createFuture;

        if (src != null && t.attachments != null && !t.attachments.isEmpty()) {
            // Migrar adjuntos en PARALELO
            java.util.List<java.util.concurrent.CompletableFuture<UploadToken>> uploadFutures = t.attachments.stream()
                    .filter(att -> att.filesize > 0)
                    .map(att -> {
                        controller.log(I18n.format("op.log.attach.migrating", att.filename));
                        // Descargar y subir en paralelo
                        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                            try {
                                byte[] data = src.downloadAttachment(att);
                                String token = service.uploadFile(data, att.contentType);
                                return new UploadToken(token, att.filename, att.contentType);
                            } catch (Exception e) {
                                controller.log(I18n.format("op.log.attach.error", att.filename, e.getMessage()));
                                throw new RuntimeException(e);
                            }
                        });
                    })
                    .collect(java.util.stream.Collectors.toList());

            // Esperar a todas las subidas, luego crear la tarea
            createFuture = java.util.concurrent.CompletableFuture.allOf(
                    uploadFutures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .thenApply(v -> uploadFutures.stream()
                            .map(java.util.concurrent.CompletableFuture::join)
                            .collect(java.util.stream.Collectors.toList()))
                    .thenApply(tokens -> {
                        t.pendingUploads.addAll(tokens);
                        return t;
                    })
                    .thenCompose(task -> {
                        try {
                            return java.util.concurrent.CompletableFuture.completedFuture(
                                    service.createTask(config.projectId, task));
                        } catch (Exception e) {
                            java.util.concurrent.CompletableFuture<Integer> failed = new java.util.concurrent.CompletableFuture<>();
                            failed.completeExceptionally(e);
                            return failed;
                        }
                    });
        } else {
            // Sin adjuntos, crear directamente
            createFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return service.createTask(config.projectId, t);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Manejar resultado
        redmineconnector.util.AsyncUIHelper.executeAsync(
                createFuture,
                id -> {
                    controller.log(I18n.format("op.log.create.success", id));
                    if (onSuccess != null) {
                        try {
                            onSuccess.accept(id);
                        } catch (Exception e) {
                            controller.log("Error in success callback: " + e.getMessage());
                        }
                    }
                    // Delay refresh slightly to ensure index availability
                    javax.swing.Timer timer = new javax.swing.Timer(1000, evt -> controller.refreshData());
                    timer.setRepeats(false);
                    timer.start();
                },
                e -> {
                    controller.log(I18n.format("op.log.create.error", e.getMessage()));
                },
                () -> view.setLoading(false));
    }

    private final java.util.Map<Integer, Long> lastUpdateTimes = new java.util.concurrent.ConcurrentHashMap<>();

    public void performUpdate(Task t, List<Attachment> deletedAttachments, List<File> newAttachments) {
        // Debounce check: Prevent duplicate updates within 2 seconds
        long now = System.currentTimeMillis();
        Long lastTime = lastUpdateTimes.get(t.id);
        if (lastTime != null && (now - lastTime) < 2000) {
            controller.log(I18n.format("op.warn.debounce", t.id));
            return;
        }
        lastUpdateTimes.put(t.id, now);

        view.setLoading(true);
        controller.log(I18n.format("op.log.update.sending", t.id));

        java.util.concurrent.CompletableFuture<Void> updateFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    try {
                        service.updateTask(t);
                        t.comment = null; // Prevent double posting if object reused
                        return null;
                    } catch (Exception e) {
                        lastUpdateTimes.remove(t.id); // Allow retry on error
                        throw new RuntimeException(e);
                    }
                });

        redmineconnector.util.AsyncUIHelper.executeAsyncVoid(
                updateFuture,
                () -> {
                    controller.log(I18n.format("op.log.update.success", t.id));
                    controller.partialRefresh(t.id);

                    // Trigger twin closure if status is closed
                    if (controller.isClosedStatus(t.status)) {
                        controller.handleTwinClosures(java.util.Collections.singletonList(t));
                    }
                },
                e -> {
                    notifications.showError(I18n.format("op.error.update", t.id),
                            e.getCause() != null ? e.getCause() : e);
                },
                () -> view.setLoading(false));
    }

    public void promptClone(Task originalStub, InstanceController sourceController) {
        view.setLoading(true);
        controller.log(I18n.format("op.log.clone.prepare", originalStub.id));

        java.util.concurrent.CompletableFuture<Task> fetchFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return sourceController.getDataService().fetchTaskDetails(originalStub.id);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        redmineconnector.util.AsyncUIHelper.executeAsync(
                fetchFuture,
                originalTask -> {
                    int originalId = originalTask.id;
                    LoggerUtil.logDebug("TaskOperations", "Original Task ID: " + originalTask.id);
                    LoggerUtil.logDebug("TaskOperations", "Original Task CF Count: "
                            + (originalTask.customFields != null ? originalTask.customFields.size() : "null"));
                    if (originalTask.customFields != null) {
                        for (redmineconnector.model.CustomField cf : originalTask.customFields) {
                            LoggerUtil.logDebug("TaskOperations", " - CF: " + cf.name + " = " + cf.value);
                        }
                    }

                    Task clonedTask = new Task(originalTask);

                    LoggerUtil.logDebug("TaskOperations", "Cloned Task CF Count: "
                            + (clonedTask.customFields != null ? clonedTask.customFields.size() : "null"));
                    clonedTask.parentId = 0; // Detach from parent to avoid restricted tracker errors
                    clonedTask.parentName = "";
                    clonedTask.trackerId = 0; // Reset tracker to force default selection

                    String sourceAssigneeName = clonedTask.assignedTo;
                    // FIX: Validate Source Name
                    if (sourceAssigneeName == null || sourceAssigneeName.isEmpty()) {
                        controller.log(
                                "DEBUG: Source assignee name is EMPTY in task details. Checking source metadata...");
                        if (originalTask.assignedToId > 0) {
                            SimpleEntity cachedUser = sourceController.getMetadataManager()
                                    .getUserById(originalTask.assignedToId);
                            if (cachedUser != null) {
                                sourceAssigneeName = cachedUser.name;
                                controller.log("DEBUG: Found assignee name from cache: " + sourceAssigneeName);
                            } else {
                                controller.log("DEBUG: Assignee ID " + originalTask.assignedToId
                                        + " not found in source cache.");
                            }
                        }
                    }

                    // CRITICAL: Wipe source assignee ID/Name to prevent invalid cross-instance
                    // mapping
                    clonedTask.assignedTo = "";
                    clonedTask.assignedToId = 0;

                    controller.log("DEBUG: Cloned task created. Parent/Tracker/Assignee IDs cleared.");

                    // Corregir referencias a imágenes en la descripción
                    if (clonedTask.attachments != null && !clonedTask.attachments.isEmpty()) {
                        clonedTask.description = redmineconnector.util.DescriptionHelper.fixClonedDescription(
                                clonedTask.description, clonedTask.attachments, config.attachmentFormat);
                    }

                    // [MOD] Append Notes (Journals) from the original task
                    if (originalTask.journals != null && !originalTask.journals.isEmpty()) {
                        StringBuilder notesBuilder = new StringBuilder();
                        boolean hasNotes = false;
                        for (redmineconnector.model.Journal j : originalTask.journals) {
                            if (j.notes != null && !j.notes.trim().isEmpty()) {
                                if (!hasNotes) {
                                    notesBuilder.append("\n\n--- Historial de Notas ---\n");
                                    hasNotes = true;
                                }
                                notesBuilder.append("\n**").append(j.user).append("** (").append(j.createdOn)
                                        .append("):\n");
                                notesBuilder.append(j.notes).append("\n");
                            }
                        }
                        if (hasNotes) {
                            clonedTask.description = (clonedTask.description == null ? "" : clonedTask.description)
                                    + notesBuilder.toString();
                        }
                    }

                    // [MOD] Prepend Source Reference
                    if (config.formatReference(originalId) != null) {
                        clonedTask.subject = config.formatReference(originalId) + " " + clonedTask.subject;
                    }

                    // Establecer tracker por defecto
                    List<SimpleEntity> trackers = controller.getTrackers();
                    if (trackers != null) {
                        for (SimpleEntity tracker : trackers) {
                            if (tracker.name.equalsIgnoreCase("Tarea") ||
                                    tracker.name.equalsIgnoreCase("Task")) {
                                clonedTask.tracker = tracker.name;
                                clonedTask.trackerId = tracker.id;
                                break;
                            }
                        }
                    } else {
                        clonedTask.trackerId = 0;
                    }

                    // Intentar buscar el usuario original por nombre
                    boolean assigneeMatched = false;
                    List<SimpleEntity> users = controller.getUsers();

                    controller.log("DEBUG: Trying to match assignee. Source: '" + sourceAssigneeName + "'");
                    if (users != null) {
                        controller.log("DEBUG: Target users count: " + users.size());
                    } else {
                        controller.log("DEBUG: Target users list is NULL");
                    }

                    if (sourceAssigneeName != null && !sourceAssigneeName.isEmpty() && users != null) {
                        for (SimpleEntity user : users) {
                            // Use exact match or contains check? Start with exact.
                            if (user.name.equalsIgnoreCase(sourceAssigneeName)) {
                                clonedTask.assignedTo = user.name;
                                clonedTask.assignedToId = user.id;
                                assigneeMatched = true;
                                controller.log("DEBUG: Exact match found: " + user.name + " (ID: " + user.id + ")");
                                break;
                            }
                        }
                    }

                    // Intentar auto-asignación basada en el mapeo de nombres del cliente en config
                    // (Fallback)
                    if (!assigneeMatched && config.clientName != null && !config.clientName.isEmpty()
                            && users != null) {
                        controller.log("DEBUG: No exact match. Trying client fallback for: " + config.clientName);
                        for (SimpleEntity user : users) {
                            if (user.name.toLowerCase().contains(config.clientName.toLowerCase()) ||
                                    (user.name.contains("@") && user.name.equalsIgnoreCase(config.clientName))) {
                                clonedTask.assignedTo = user.name;
                                clonedTask.assignedToId = user.id;
                                controller.log(I18n.format("op.log.clone.assigned", user.name));
                                break;
                            }
                        }
                    }

                    // Set status ID to match the "Nueva" status set by Task copy constructor
                    List<SimpleEntity> statuses = controller.getStatuses();
                    if (statuses != null && clonedTask.statusId == 0) {
                        for (SimpleEntity status : statuses) {
                            if (status.name.equalsIgnoreCase("Nuevo") ||
                                    status.name.equalsIgnoreCase("New") ||
                                    status.name.equalsIgnoreCase("Nueva")) {
                                clonedTask.statusId = status.id;
                                clonedTask.status = status.name;
                                controller.log(I18n.format("op.log.clone.status", status.name));
                                break;
                            }
                        }
                    }

                    // [MOD] Auto-fill "External Tracker" custom field with Original ID
                    // 1. Try to find definition in Cache to get ID
                    List<CustomFieldDefinition> defs = CustomFieldsCache.getDefinitions(config.url);
                    int targetFieldId = -1;
                    String targetFieldName = null;

                    if (defs != null) {
                        for (CustomFieldDefinition d : defs) {
                            String lower = d.name.toLowerCase();
                            if (lower.contains("tracker") && (lower.contains("extern") || lower.contains("origin"))) {
                                targetFieldId = d.id;
                                targetFieldName = d.name;
                                break;
                            }
                        }
                    }

                    // 2. If found in cache, ensure it exists in clonedTask
                    if (targetFieldId != -1) {
                        if (clonedTask.customFields == null)
                            clonedTask.customFields = new ArrayList<>();
                        boolean found = false;
                        for (CustomField cf : clonedTask.customFields) {
                            if (cf.id == targetFieldId) {
                                cf.value = String.valueOf(originalId);
                                found = true;
                                controller.log("Auto-filled '" + cf.name + "' with Original ID: " + originalId);
                                break;
                            }
                        }
                        if (!found && targetFieldName != null) {
                            clonedTask.customFields
                                    .add(new CustomField(targetFieldId, targetFieldName, String.valueOf(originalId)));
                            controller
                                    .log("Added and filled '" + targetFieldName + "' with Original ID: " + originalId);
                        }
                    } else {
                        // 3. Fallback: Try to find in existing fields (heuristic if cache missed but
                        // field is present)
                        if (clonedTask.customFields != null) {
                            for (CustomField cf : clonedTask.customFields) {
                                if (cf.name != null) {
                                    String lower = cf.name.toLowerCase();
                                    if (lower.contains("tracker")
                                            && (lower.contains("extern") || lower.contains("origin"))) {
                                        cf.value = String.valueOf(originalId);
                                        controller.log("Auto-filled '" + cf.name + "' (Fallback) with Original ID: "
                                                + originalId);
                                    }
                                }
                            }
                        }
                    }

                    String finalSourceAssignee = sourceAssigneeName;
                    dialogs.openCreateDialog(clonedTask, sourceController.getDataService(), originalId, (newId) -> {
                        // [NEW] Reverse Sync: If source had no assignee, but target is assigned, update
                        // source.
                        if (finalSourceAssignee == null || finalSourceAssignee.isEmpty()) {
                            String newAssignee = clonedTask.assignedTo;
                            if (newAssignee != null && !newAssignee.isEmpty() && !newAssignee.trim().isEmpty()) {
                                controller.log("Syncing assignee back to source: " + newAssignee);

                                // Find user in source
                                SimpleEntity sourceUser = null;
                                List<SimpleEntity> sourceUsers = sourceController.getUsers();
                                if (sourceUsers != null) {
                                    for (SimpleEntity u : sourceUsers) {
                                        if (u.name.equalsIgnoreCase(newAssignee)) {
                                            sourceUser = u;
                                            break;
                                        }
                                    }
                                }

                                if (sourceUser != null) {
                                    final SimpleEntity userToSet = sourceUser;
                                    redmineconnector.util.SwingWorkerFactory.executeAsync(
                                            () -> {
                                                try {
                                                    Task t = sourceController.getDataService()
                                                            .fetchTaskDetails(originalId);
                                                    t.assignedToId = userToSet.id;
                                                    t.assignedTo = userToSet.name;
                                                    t.comment = "Asignado automáticamente tras clonación en "
                                                            + config.clientName;
                                                    // Avoid full update overhead if possible, but updateTask is
                                                    // standard
                                                    sourceController.getDataService().updateTask(t);
                                                    return true;
                                                } catch (Exception ex) {
                                                    controller.log("Error updating source task: " + ex.getMessage());
                                                    return false;
                                                }
                                            },
                                            v -> {
                                                if (Boolean.TRUE.equals(v)) {
                                                    controller.log(
                                                            "Source task updated with assignee: " + userToSet.name);
                                                    sourceController.onRefresh(); // Refresh source again
                                                }
                                            },
                                            e -> controller.log("Failed to sync assignee back: " + e.getMessage()));
                                } else {
                                    controller.log("Could not find user '" + newAssignee + "' in source instance.");
                                }
                            }
                        }
                    });

                    // [NEW] Refresh source controller after dialog closes (Task created or
                    // canceled)
                    // If modal, this runs after close. If created, source might want to see
                    // updates.
                    controller.log("Refreshing source instance after clone dialog close...");
                    sourceController.onRefresh();
                },
                e -> controller.log(I18n.format("op.log.clone.error", e.getMessage())),
                () -> view.setLoading(false));
    }

    public void downloadTaskToDesktop(Task stub) {
        view.setLoading(true);
        controller.log(I18n.format("op.log.download.start", stub.id));

        java.util.concurrent.CompletableFuture<String> downloadFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    try {
                        controller.log(I18n.format("op.log.download.details", stub.id));
                        Task full = service.fetchTaskDetails(stub.id);
                        String folderName = config.folderPattern != null ? config.folderPattern : "{id}_{subject}";
                        folderName = folderName.replace("{id}", String.valueOf(full.id))
                                .replace("{subject}", (full.subject == null ? "" : full.subject))
                                .replace("{tracker}", (full.tracker == null ? "" : full.tracker))
                                .replace("{priority}", (full.priority == null ? "" : full.priority))
                                .replaceAll("[^a-zA-Z0-9.-]", "_");

                        // Truncate if too long
                        if (folderName.length() > redmineconnector.util.AppConstants.MAX_SUBJECT_DISPLAY_LENGTH)
                            folderName = folderName.substring(0,
                                    redmineconnector.util.AppConstants.MAX_SUBJECT_DISPLAY_LENGTH);

                        File rootDir;
                        if (config.downloadPath != null && !config.downloadPath.trim().isEmpty()) {
                            rootDir = new File(config.downloadPath);
                            if (!rootDir.exists())
                                rootDir.mkdirs();
                        } else {
                            rootDir = javax.swing.filechooser.FileSystemView.getFileSystemView().getHomeDirectory();
                        }

                        File dir = new File(rootDir, folderName);
                        if (!dir.exists()) {
                            if (!dir.mkdirs()) {
                                throw new Exception(I18n.format("op.error.dir.create", dir.getAbsolutePath()));
                            }
                        }

                        // Escribir Detalles.txt
                        File detailsFile = new File(dir, I18n.get("op.file.details.filename"));
                        try (java.io.PrintWriter pw = new java.io.PrintWriter(detailsFile, "UTF-8")) {
                            pw.println(I18n.format("op.file.details.header", full.id, full.subject));
                            pw.println(I18n.format("op.file.details.status", full.status));
                            pw.println(I18n.format("op.file.details.assigned", full.assignedTo));
                            pw.println(I18n.get("op.file.details.desc") + "\n"
                                    + (full.description != null ? full.description : ""));
                            pw.println("\n" + I18n.get("op.file.details.history"));
                            if (full.journals != null && !full.journals.isEmpty()) {
                                for (redmineconnector.model.Journal j : full.journals) {
                                    if (j.notes != null && !j.notes.trim().isEmpty()) {
                                        pw.println("----------------------------------------");
                                        pw.println(j.user + " (" + j.createdOn + "):");
                                        pw.println(j.notes);
                                    }
                                }
                            } else {
                                pw.println("(Sin notas en el historial)");
                            }
                        }

                        // Descargar adjuntos en paralelo
                        if (full.attachments != null && !full.attachments.isEmpty()) {
                            File attDir = new File(dir, I18n.get("op.dir.attachments"));
                            attDir.mkdirs();

                            java.util.List<java.util.concurrent.CompletableFuture<Void>> attachmentFutures = full.attachments
                                    .stream()
                                    .map(att -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                                        try {
                                            controller.log(I18n.format("op.log.download.attach", att.filename));
                                            byte[] data = service.downloadAttachment(att);

                                            // Validación básica de contenido binario
                                            if (data.length > 0 && data.length < 5000) {
                                                String start = new String(data, 0, Math.min(data.length, 100))
                                                        .toLowerCase();
                                                if (start.contains("<!doctype html") || start.contains("<html")) {
                                                    throw new Exception(
                                                            "El servidor devolvió una página HTML en lugar del archivo (posible error de permisos o login).");
                                                }
                                            }

                                            String safeFileName = att.filename == null ? "adjunto_" + att.id
                                                    : att.filename;
                                            safeFileName = safeFileName.replaceAll("[^a-zA-Z0-9.-]", "_");

                                            try (FileOutputStream fos = new FileOutputStream(
                                                    new File(attDir, safeFileName))) {
                                                fos.write(data);
                                            }
                                        } catch (Exception ex) {
                                            controller.log(I18n.format("op.log.download.attach.error", att.filename,
                                                    ex.getMessage()));
                                        }
                                    }))
                                    .collect(java.util.stream.Collectors.toList());

                            // Esperar a todos los adjuntos
                            java.util.concurrent.CompletableFuture.allOf(
                                    attachmentFutures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
                        }

                        String result = I18n.format("op.log.download.success", dir.getAbsolutePath());
                        controller.log(result);
                        return result;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        redmineconnector.util.AsyncUIHelper.executeAsync(
                downloadFuture,
                result -> notifications.showSuccess(I18n.get("op.msg.download.success")),
                e -> {
                    String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    notifications.showError(I18n.format("op.error.download", msg), e);
                },
                () -> view.setLoading(false));
    }

    public void performBulkUpdate(int[] rows, SimpleEntity s, SimpleEntity p, SimpleEntity a, SimpleEntity cat,
            SimpleEntity v, Integer doneRatio, String note) {
        view.setLoading(true);
        controller.log(I18n.format("op.log.bulk.start", rows.length));

        // Crear lista de futuros de actualización (ejecución paralela)
        java.util.List<java.util.concurrent.CompletableFuture<String>> updateFutures = new java.util.ArrayList<>();

        for (int r : rows) {
            Task t = view.model.getTaskAt(view.table.convertRowIndexToModel(r));

            // Aplicar cambios
            if (s != null) {
                t.statusId = s.id;
                t.status = s.name;
            }
            if (p != null) {
                t.priorityId = p.id;
                t.priority = p.name;
            }
            if (a != null) {
                t.assignedToId = a.id;
                t.assignedTo = a.name;
            }
            if (cat != null) {
                t.categoryId = cat.id;
                t.category = cat.name;
            }
            if (v != null) {
                t.targetVersionId = v.id;
                t.targetVersion = v.name;
            }
            if (doneRatio != null) {
                t.doneRatio = doneRatio;
            }
            if (note != null && !note.trim().isEmpty()) {
                t.comment = note;
            }

            // Crear actualización asíncrona para esta tarea
            final Task taskToUpdate = t;
            java.util.concurrent.CompletableFuture<String> updateFuture = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            service.updateTask(taskToUpdate);
                            taskToUpdate.comment = ""; // reset
                            return I18n.format("op.res.bulk.updated", taskToUpdate.id);
                        } catch (Exception e) {
                            return I18n.format("op.res.bulk.error", taskToUpdate.id, e.getMessage());
                        }
                    });

            updateFutures.add(updateFuture);
        }

        // Esperar a que TODAS las actualizaciones se completen
        java.util.concurrent.CompletableFuture.allOf(
                updateFutures.toArray(new java.util.concurrent.CompletableFuture[0])).thenRun(() -> {
                    // Loguear todos los resultados
                    updateFutures.forEach(future -> {
                        try {
                            String result = future.join();
                            controller.log(result);
                        } catch (Exception e) {
                            controller.log("Error: " + e.getMessage());
                        }
                    });

                    javax.swing.SwingUtilities.invokeLater(() -> {
                        view.setLoading(false);
                        controller.refreshData();
                        controller.log(I18n.get("op.msg.bulk.complete"));
                    });
                }).exceptionally(e -> {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        view.setLoading(false);
                        controller.log(I18n.format("op.log.bulk.error", e.getMessage()));
                    });
                    return null;
                });
    }

    public void performMultiClose(java.util.List<Task> tasks, SimpleEntity version, SimpleEntity selectedStatus,
            SimpleEntity assignment) {
        if (tasks == null || tasks.isEmpty())
            return;

        view.setLoading(true);
        controller.log(I18n.format("op.log.multiclose.start", tasks.size()));

        // Use the selected status from dialog
        SimpleEntity closedStatus = selectedStatus;
        if (closedStatus == null) {
            // Fallback: find a closed status if none was provided
            List<SimpleEntity> stats = controller.getStatuses();
            if (stats != null) {
                for (SimpleEntity s : stats) {
                    if (controller.isClosedStatus(s.name)) {
                        closedStatus = s;
                        break;
                    }
                }
            }
        }

        // Final fallback if still null? Logic seems to allow null (unchanged status
        // usually, but this is "Close", so it should probably set a status.
        // But original code allowed null.

        java.util.List<java.util.concurrent.CompletableFuture<String>> updateFutures = new java.util.ArrayList<>();

        // Capture success for twin notification safely
        java.util.List<Task> successfullyClosed = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (Task t : tasks) {
            // Aplicar cambios para Multi-Close
            if (closedStatus != null) {
                t.statusId = closedStatus.id;
                t.status = closedStatus.name;
            }

            // Handle Assignment Logic
            if (assignment != null) {
                if (assignment.id == -2) {
                    // Assign to Author
                    if (t.authorId > 0) {
                        t.assignedToId = t.authorId;
                        t.assignedTo = t.author;
                    }
                } else if (assignment.id == 0) {
                    // Assign to Nobody
                    t.assignedToId = 0;
                    t.assignedTo = "";
                } else if (assignment.id > 0) {
                    // Assign to specific user
                    t.assignedToId = assignment.id;
                    t.assignedTo = assignment.name;
                }
                // If id is -1 (Unchanged), do nothing.
            } else {
                // Stick to legacy safe default or do nothing? Original had 'else' block that
                // seemed legacy/buggy comments?
                // Original logic for "rows" loop had specific check.
                // Let's assume passed safe assignment.
            }

            t.doneRatio = 100;
            if (version != null) {
                t.targetVersionId = version.id;
                t.targetVersion = version.name;
            }
            t.comment = I18n.get("version.msg.multiclose_note");

            final Task taskToUpdate = t;
            java.util.concurrent.CompletableFuture<String> updateFuture = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            service.updateTask(taskToUpdate);
                            taskToUpdate.comment = "";
                            successfullyClosed.add(taskToUpdate);
                            return I18n.format("op.res.bulk.updated", taskToUpdate.id);
                        } catch (Exception e) {
                            return I18n.format("op.res.bulk.error", taskToUpdate.id, e.getMessage());
                        }
                    });
            updateFutures.add(updateFuture);
        }

        java.util.concurrent.CompletableFuture.allOf(
                updateFutures.toArray(new java.util.concurrent.CompletableFuture[0])).thenRun(() -> {
                    updateFutures.forEach(future -> {
                        try {
                            controller.log(future.join());
                        } catch (Exception ignored) {
                        }
                    });

                    javax.swing.SwingUtilities.invokeLater(() -> {
                        view.setLoading(false);
                        controller.refreshData();
                        controller.log(I18n.get("op.msg.multiclose.complete"));

                        if (!successfullyClosed.isEmpty()) {
                            controller.handleTwinClosures(new java.util.ArrayList<>(successfullyClosed));
                        }
                    });
                }).exceptionally(e -> {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        view.setLoading(false);
                        controller.log(I18n.format("op.log.multiclose.error", e.getMessage()));
                    });
                    return null;
                });
    }

    public void performTwinClosure(java.util.List<Task> twins, SimpleEntity version, SimpleEntity status,
            SimpleEntity assignment, InstanceController source) {
        view.setLoading(true);
        controller.log("Iniciando cierre sincronizado de " + twins.size() + " tareas gemelas.");

        SimpleEntity termStatus = status;
        List<SimpleEntity> stats2 = controller.getStatuses();
        if (termStatus == null && stats2 != null) {
            for (SimpleEntity s : stats2) {
                String name = s.name.toLowerCase();
                if (name.contains("termin") || name.contains("finish") || name.contains("resol")
                        || name.contains("complet")) {
                    termStatus = s;
                    break;
                }
            }
        }
        if (termStatus == null && stats2 != null) {
            for (SimpleEntity s : stats2) {
                if (controller.isClosedStatus(s.name)) {
                    termStatus = s;
                    break;
                }
            }
        }

        java.util.List<java.util.concurrent.CompletableFuture<String>> futures = new java.util.ArrayList<>();
        for (Task t : twins) {
            if (termStatus != null) {
                t.statusId = termStatus.id;
                t.status = termStatus.name;
            }

            // Handle Assignment Logic
            if (assignment != null) {
                if (assignment.id == -2) {
                    // Assign to Author
                    if (t.authorId > 0) {
                        t.assignedToId = t.authorId;
                        t.assignedTo = t.author;
                    }
                } else if (assignment.id == 0) {
                    // Assign to Nobody
                    t.assignedToId = 0;
                    t.assignedTo = "";
                } else if (assignment.id > 0) {
                    // Assign to specific user
                    t.assignedToId = assignment.id;
                    t.assignedTo = assignment.name;
                }
                // If id is -1 (Unchanged), do nothing.
            } else {
                // Legacy behavior for twin closure was: t.assignedTo = t.author if exist?
                // Checking original code:
                // if (t.authorId > 0) { t.assignedToId = t.authorId; ... }
                // So legacy behavior WAS to assign to Author!
                // Let's preserve that if assignment is null?
                // Or better, strictly follow the new UI.
            }

            t.doneRatio = 100;
            if (version != null) {
                t.targetVersionId = version.id;
                t.targetVersion = version.name;
            }
            t.comment = I18n.format("twin.msg.note", source.getTitle());

            final Task taskToUpdate = t;
            futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    service.updateTask(taskToUpdate);
                    taskToUpdate.comment = "";
                    return I18n.format("op.res.bulk.updated", taskToUpdate.id);
                } catch (Exception e) {
                    return I18n.format("op.res.bulk.error", taskToUpdate.id, e.getMessage());
                }
            }));
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .thenRun(() -> {
                    futures.forEach(f -> {
                        try {
                            controller.log(f.join());
                        } catch (Exception e) {
                        }
                    });

                    // Capture closed twins BEFORE refresh to avoid race conditions
                    java.util.List<Task> closedTwins = new java.util.ArrayList<>(twins);

                    javax.swing.SwingUtilities.invokeLater(() -> {
                        view.setLoading(false);
                        controller.refreshData();
                        controller.log("Sincronización completada.");

                        // User Feedback: Option 1 & 3 (Toast + Beep)
                        int closedCount = closedTwins.size();
                        String msg = "Cierre sincronizado completado con éxito (" + closedCount + " tareas).";
                        notifications.showSuccess(msg);
                        java.awt.Toolkit.getDefaultToolkit().beep();

                        // Trigger bidirectional sync: check if status checks are needed?
                        // FIX: Do NOT recursive trigger handleTwinClosures, as this causes loop
                        // (A->B->C->B...)
                        // The source has already notified all peers. We are a leaf node here.
                        /*
                         * if (!closedTwins.isEmpty()) {
                         * controller.log("Verificando cierre bidireccional en instancia de origen...");
                         * controller.handleTwinClosures(closedTwins);
                         * }
                         */
                    });
                });
    }
}
