package redmineconnector.ui;

import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import redmineconnector.util.SwingWorkerFactory;
import redmineconnector.util.LoggerUtil;

import redmineconnector.config.ConfigManager;
import redmineconnector.config.ConnectionConfig;
import redmineconnector.config.StyleConfig;
import redmineconnector.model.SimpleEntity;
import redmineconnector.model.Task;
import redmineconnector.service.DataService;
import redmineconnector.service.AsyncDataService;
import redmineconnector.service.HttpDataService;

import redmineconnector.ui.dialogs.HelpDialog;
import redmineconnector.ui.dialogs.InstanceConfigDialog;

import redmineconnector.ui.dialogs.KeywordAnalysisDialog;
import redmineconnector.ui.dialogs.StatusColorDialog;
import redmineconnector.ui.dialogs.VersionManagerDialog;
import redmineconnector.ui.dialogs.WikiManagerDialog;

import redmineconnector.util.I18n;

/**
 * Controlador principal para una instancia de cliente Redmine.
 * 
 * <p>
 * Esta clase gestiona la interacción entre la vista ({@link InstanceView}),
 * el servicio de datos ({@link DataService}) y las operaciones sobre tareas de
 * Redmine.
 * Actúa como coordinador central para todas las operaciones relacionadas con
 * una
 * conexión específica a un servidor Redmine.
 * </p>
 * 
 * <h2>Responsabilidades Principales:</h2>
 * <ul>
 * <li><b>Gestión de Datos:</b> Carga, refresh y caché de tareas desde el
 * servidor</li>
 * <li><b>Filtrado:</b> Aplicación de filtros de búsqueda y visualización</li>
 * <li><b>Operaciones:</b> Creación, edición, clonación y descarga de
 * tareas</li>
 * <li><b>Sincronización:</b> Coordinación con el controlador peer para
 * clonación entre servidores</li>
 * <li><b>UI:</b> Manejo de eventos de usuario y actualización de la vista</li>
 * <li><b>Notificaciones:</b> Detección de nuevas tareas y notificaciones al
 * usuario</li>
 * </ul>
 * 
 * <h2>Arquitectura:</h2>
 * 
 * <pre>
 * ┌─────────────────┐
 * │  InstanceView   │ ← Vista (UI)
 * └────────┬────────┘
 *          │
 *     ┌────▼────────────────┐
 *     │ InstanceController  │ ← Este controlador
 *     └────┬────────────────┘
 *          │
 *     ┌────▼────────┐
 *     │ AsyncDataService  │ ← Wrapper Asíncrono
 *     └────┬──────────────┘
 *          │
 *     ┌────▼──────────────┐
 *     │ CachedDataService │ ← Decorador de Caché
 *     └────┬──────────────┘
 *          │
 *     ┌────▼────────┐
 *     │ HttpDataService │ ← Cliente HTTP Real
 *     └─────────────┘
 * </pre>
 * 
 * <h2>Patrón de Uso:</h2>
 * 
 * <pre>{@code
 * // Crear controlador
 * InstanceController controller = new InstanceController(
 *         "Cliente Origen",
 *         "source",
 *         config,
 *         styleConfig,
 *         logConsumer);
 * 
 * // Vincular con peer para clonación
 * controller.setPeer(targetController);
 * 
 * // Obtener vista para mostrar
 * InstanceView view = controller.getView();
 * 
 * // Refrescar datos
 * controller.refreshData();
 * }</pre>
 * 
 * <h2>Thread Safety:</h2>
 * <p>
 * Este controlador utiliza SwingWorker para operaciones en background,
 * asegurando que las actualizaciones de UI se realicen en el Event Dispatch
 * Thread (EDT).
 * Los métodos públicos son seguros para llamar desde el EDT.
 * </p>
 * 
 * @author Redmine Connector Team
 * @version 8.5.5
 * @see InstanceView
 * @see DataService
 * @see Task
 */
import redmineconnector.ui.input.KeyboardShortcutManager;

public class InstanceController {
    private final String configPrefix;
    private ConnectionConfig config;
    private StyleConfig styleConfig;
    private final InstanceView view;
    private DataService service;
    private AsyncDataService asyncService;

    private final MetadataManager metadataManager;
    private final TaskManager taskManager;
    private final ViewManager viewManager;
    private final Consumer<String> logger;

    private javax.swing.Timer autoRefreshTimer;
    private boolean metadataLoaded = false;
    private boolean isFirstLoad = true;

    // Helpers
    private DialogManager dialogManager;
    private TaskOperations taskOperations;
    private NotificationService notificationService;

    private redmineconnector.service.CloneService cloneService;
    private redmineconnector.service.ExportManager exportManager;
    private SimpleEntity currentProject; // Stores numeric ID and Name

    public InstanceController(String configPrefix, String title, Properties props, Consumer<String> globalLogger) {
        this.configPrefix = configPrefix;
        this.config = new ConnectionConfig(configPrefix, props);
        this.styleConfig = new StyleConfig();
        this.styleConfig.load(props, configPrefix);
        this.viewManager = new ViewManager(title);
        this.viewManager.setTitle(computeTitle());
        this.view = new InstanceView(this.viewManager.getTitle(), styleConfig);
        this.logger = globalLogger;

        this.metadataManager = new MetadataManager();
        this.taskManager = new TaskManager();

        this.notificationService = new NotificationService(view, this);
        initService();

        // Fetch Project Info (Async)
        if (config.projectId != null && !config.projectId.isEmpty()) {
            asyncService.fetchProjectAsync(config.projectId)
                    .thenAccept(p -> this.currentProject = p)
                    .exceptionally(e -> {
                        Exception exToLog = (e instanceof Exception) ? (Exception) e : new Exception(e);
                        LoggerUtil.logError("InstanceController", "Failed to load project info: " + e.getMessage(),
                                exToLog);
                        return null;
                    });
        }

        loadPins();
        initListeners();

        // Initialize Helpers
        this.dialogManager = new DialogManager(this, view, config);
        this.taskOperations = new TaskOperations(this, view, service, config, dialogManager, notificationService);
        this.cloneService = new redmineconnector.service.CloneService(this, notificationService, config);
        this.exportManager = new redmineconnector.service.ExportManager(view, msg -> log(msg));

        initAutoRefresh();
        view.setController(this);
        view.setDataService(service);
    }

    public SimpleEntity getCurrentProject() {
        return currentProject;
    }

    public boolean hasAllTwins(List<Task> tasks) {
        return cloneService.hasAllTwins(tasks, viewManager.getPeers());
    }

    public boolean hasMissingTwins(List<Task> tasks) {
        return cloneService.hasMissingTwins(tasks, viewManager.getPeers());
    }

    private String computeTitle() {
        String name = config.clientName;
        String dTitle = viewManager.getDefaultTitle();
        if (name == null || name.trim().isEmpty() || "Cliente".equalsIgnoreCase(name)) {
            return dTitle;
        }
        String suffix = "";
        if (dTitle.toLowerCase().contains("origen"))
            suffix = " " + I18n.get("controller.title.source");
        else if (dTitle.toLowerCase().contains("destino"))
            suffix = " " + I18n.get("controller.title.target");
        return name + suffix;
    }

    private void initService() {
        // Core HTTP Service
        DataService httpService = new HttpDataService(config.url, config.apiKey, msg -> log(msg));

        // Caching Layer (Wraps HTTP Service)
        redmineconnector.service.CacheService simpleCache = new redmineconnector.service.SimpleCacheService();
        redmineconnector.service.CachedDataService cachedService = new redmineconnector.service.CachedDataService(
                httpService, simpleCache);

        // Async Wrapper (Wraps Cached Service), available for future SwingWorker
        // replacement
        this.asyncService = new redmineconnector.service.AsyncDataService(cachedService);

        // Controller uses cached service by default
        this.service = cachedService;

        // Initialize Custom Fields Cache (Load known fields for this instance)
        redmineconnector.service.CustomFieldsCache.load(config.url);

        // Trigger background learning: scan recent tasks to find custom fields
        // This is useful if the user lacks permissions to fetch /custom_fields.json
        SwingWorkerFactory.executeAsync(
                () -> {
                    try {
                        // Fetch last 50 tasks to learn schema
                        return cachedService.fetchTasks(config.projectId, false, 50);
                    } catch (Exception e) {
                        return null;
                    }
                },
                tasks -> {
                    if (tasks != null) {
                        redmineconnector.service.CustomFieldsCache.learnFromTasks(config.url, tasks);
                        log("Background: Learned custom fields from " + tasks.size() + " recent tasks.");
                    }
                },
                e -> {
                });
    }

    private void initAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
        }
        if (config.refreshInterval > 0) {
            // Auto-refresh timer
            int delay = config.refreshInterval * 60 * 1000;
            autoRefreshTimer = new javax.swing.Timer(delay, e -> {
                if (view.isShowing()) {
                    log(I18n.get("controller.log.autorefresh"));
                    refreshData();
                }
            });
            autoRefreshTimer.start();
        }
    }

    private void initListeners() {
        // Refresh and Create events are bound in InstanceView -> FiltersPanel
        view.btnBulk.addActionListener(e -> {
            log(I18n.get("controller.log.bulk"));
            openBulkUpdateDialog();
        });
        view.btnMultiClose.addActionListener(e -> {
            log(I18n.get("controller.log.multiclose"));
            openMultiCloseDialog();
        });
        // Multi-edit actions
        view.setSmartMatchAction(t -> {
            if (t != null && !viewManager.getPeers().isEmpty()) {
                log(I18n.format("controller.log.smart", t.id));
                String searchContext = cleanSubject(t.subject);
                for (InstanceController p : viewManager.getPeers()) {
                    p.performKeywordSearch(searchContext);
                }
                log(I18n.format("controller.log.search_dest", searchContext));
            }
        });
        view.setMultiTwinClosureAction(tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                handleTwinClosures(tasks);
            }
        });
        view.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !viewManager.isSelectionSyncing()) {
                Task t = view.getSelectedTask();
                if (t != null && !viewManager.getPeers().isEmpty()) {
                    for (InstanceController p : viewManager.getPeers()) {
                        p.syncSelection(t, config);
                    }
                }
            }
        });
        view.table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Task t = view.getSelectedTask();
                    if (t != null) {
                        log(I18n.format("controller.log.double_click", t.id));
                        view.setLoading(true);

                        SwingWorkerFactory.executeAsync(
                                () -> {
                                    try {
                                        return service.fetchTaskDetails(t.id);
                                    } catch (Exception e1) {
                                        redmineconnector.util.LoggerUtil.logError("InstanceController",
                                                "Failed to fetch task details for ID " + t.id, e1);
                                        return null;
                                    }
                                },
                                fullTask -> {
                                    view.setLoading(false);
                                    openCreateDialog(fullTask);
                                },
                                error -> {
                                    view.setLoading(false);
                                    log(I18n.format("controller.warn.incomplete", error.getMessage()));
                                    openCreateDialog(t);
                                });
                    }
                }
            }
        });
        view.setCloneAction(task -> {
            log(I18n.format("controller.log.clone_req", task.id));
            requestClone(task);
        });
        view.setDownloadAction(task -> {
            log(I18n.format("controller.log.download_req", task.id));
            downloadTaskToDesktop(task);
        });
        view.setCreateChildAction(parent -> {
            log(I18n.format("controller.log.child_req", parent.id));
            Task child = new Task();
            child.parentId = parent.id;
            child.parentName = parent.subject;
            openCreateDialog(child);
        });

        // Configurar atajos de teclado
        setupKeyboardShortcuts();
    }

    public void toggleShowClosed(boolean show) {
        log(I18n.format("controller.log.toggle_closed", show));
        this.config.showClosed = show;
        Properties p = ConfigManager.loadConfig();
        p.setProperty(configPrefix + ".closed", String.valueOf(show));
        ConfigManager.saveConfig(p);
        refreshData();
    }

    public void toggleIncludeEpics(boolean include) {
        log(I18n.format("controller.log.toggle_epics", include));
        this.config.includeEpics = include;
        Properties p = ConfigManager.loadConfig();
        p.setProperty(configPrefix + ".epics", String.valueOf(include));
        ConfigManager.saveConfig(p);
        refreshData();
    }

    private String cleanSubject(String s) {
        if (s == null)
            return "";
        return s.replaceAll("^(\\[[^\\]]+\\]\\s*)*", "").replaceAll("(?i)^(Re:|Fwd:|Rv:|Enc:)\\s*", "").trim();
    }

    public void performKeywordSearch(String text) {
        log(I18n.format("controller.log.perf_search", text));
        view.setSearchText(text);
    }

    public void onRefresh() {
        log(I18n.get("controller.log.refresh"));
        refreshData();
    }

    public void onCreate() {
        log(I18n.get("controller.log.new"));
        openCreateDialog(null);
    }

    public void setPeers(List<InstanceController> peers) {
        for (InstanceController p : peers) {
            if (p != this) {
                viewManager.addPeer(p);
            }
        }
    }

    public List<InstanceController> getPeers() {
        return viewManager.getPeers();
    }

    private void requestClone(Task task) {
        cloneService.requestClone(task, viewManager.getPeers(), this.service);
    }

    public InstanceView getView() {
        return view;
    }

    void log(String m) {
        if (logger != null)
            logger.accept(getTitle() + ": " + m);
    }

    public ConnectionConfig getConfig() {
        return config;
    }

    public byte[] downloadAttachment(redmineconnector.model.Attachment att) throws Exception {
        return service.downloadAttachment(att);
    }

    public DataService getDataService() {
        return service;
    }

    public MetadataManager getMetadataManager() {
        return metadataManager;
    }

    public boolean isPinned(int taskId) {
        return taskManager.getPinnedTaskIds().contains(taskId);
    }

    public void togglePin(int taskId) {
        if (taskManager.getPinnedTaskIds().contains(taskId)) {
            taskManager.getPinnedTaskIds().remove(taskId);
            log(I18n.format("controller.log.unpinned", taskId));
        } else {
            taskManager.getPinnedTaskIds().add(taskId);
            log(I18n.format("controller.log.pinned", taskId));
        }
        savePins();
        view.model.fireTableDataChanged();
    }

    private void loadPins() {
        taskManager.getPinnedTaskIds().clear();
        String s = config.pinnedTaskIds;
        if (s != null && !s.trim().isEmpty()) {
            for (String id : s.split(",")) {
                try {
                    taskManager.getPinnedTaskIds().add(Integer.parseInt(id.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private void savePins() {
        String s = taskManager.getPinnedTaskIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        config.pinnedTaskIds = s;
        Properties p = ConfigManager.loadConfig();
        p.setProperty(configPrefix + ".pinnedTaskIds", s);
        ConfigManager.saveConfig(p);
    }

    public void syncSelection(Task t, ConnectionConfig sourceConfig) {
        viewManager.setSelectionSyncing(true);
        try {
            log(I18n.format("controller.log.sync_sel", t.subject));
            boolean found = view.selectMatch(t, sourceConfig, config);
            if (viewManager.getOnSyncMatch() != null) {
                viewManager.getOnSyncMatch().accept(found);
            }
        } finally {
            viewManager.setSelectionSyncing(false);
        }
    }

    public void setOnSyncMatch(Consumer<Boolean> callback) {
        viewManager.setOnSyncMatch(callback);
    }

    public void refreshData() {
        Task selected = view.getSelectedTask();
        int selectedId = selected != null ? selected.id : -1;
        view.setLoading(true);
        log(I18n.get("controller.log.refresh_start"));

        // Load tasks (always)
        java.util.concurrent.CompletableFuture<List<Task>> tasksFuture = asyncService.fetchTasksAsync(config.projectId,
                config.showClosed, config.limit);

        // Metadata futures (only if not loaded)
        java.util.concurrent.CompletableFuture<List<SimpleEntity>> usersFuture = metadataLoaded
                ? java.util.concurrent.CompletableFuture.completedFuture(metadataManager.getUsers())
                : asyncService.fetchMetadataAsync("users", config.projectId);
        java.util.concurrent.CompletableFuture<List<SimpleEntity>> trackersFuture = metadataLoaded
                ? java.util.concurrent.CompletableFuture.completedFuture(metadataManager.getTrackers())
                : asyncService.fetchMetadataAsync("trackers", config.projectId);
        java.util.concurrent.CompletableFuture<List<SimpleEntity>> prioritiesFuture = metadataLoaded
                ? java.util.concurrent.CompletableFuture.completedFuture(metadataManager.getPriorities())
                : asyncService.fetchMetadataAsync("priorities", config.projectId);
        java.util.concurrent.CompletableFuture<List<SimpleEntity>> statusesFuture = metadataLoaded
                ? java.util.concurrent.CompletableFuture.completedFuture(metadataManager.getStatuses())
                : asyncService.fetchMetadataAsync("statuses", config.projectId);
        java.util.concurrent.CompletableFuture<List<SimpleEntity>> categoriesFuture = metadataLoaded
                ? java.util.concurrent.CompletableFuture.completedFuture(metadataManager.getCategories())
                : asyncService.fetchMetadataAsync("categories", config.projectId);
        java.util.concurrent.CompletableFuture<List<SimpleEntity>> versionsFuture = metadataLoaded
                ? java.util.concurrent.CompletableFuture.completedFuture(metadataManager.getVersions())
                : asyncService.fetchMetadataAsync("versions", config.projectId);
        java.util.concurrent.CompletableFuture<List<SimpleEntity>> activitiesFuture = metadataLoaded
                ? java.util.concurrent.CompletableFuture.completedFuture(metadataManager.getActivities())
                : asyncService.fetchMetadataAsync("activities", config.projectId);
        java.util.concurrent.CompletableFuture<SimpleEntity> currentUserFuture = (metadataLoaded
                && metadataManager.getCurrentUser() != null)
                        ? java.util.concurrent.CompletableFuture.completedFuture(metadataManager.getCurrentUser())
                        : asyncService.fetchCurrentUserAsync().exceptionally(e -> null);

        // Wait for all
        java.util.concurrent.CompletableFuture.allOf(tasksFuture, usersFuture, trackersFuture, prioritiesFuture,
                statusesFuture, categoriesFuture, versionsFuture, activitiesFuture, currentUserFuture).thenRun(() -> {
                    try {
                        List<SimpleEntity> users = usersFuture.join();
                        List<SimpleEntity> trackers = trackersFuture.join();
                        List<SimpleEntity> priorities = prioritiesFuture.join();
                        List<SimpleEntity> statuses = statusesFuture.join();
                        List<SimpleEntity> categories = categoriesFuture.join();
                        List<SimpleEntity> versions = versionsFuture.join();
                        List<SimpleEntity> activities = activitiesFuture.join();
                        SimpleEntity currentUser = currentUserFuture.join();
                        List<Task> rawTasks = tasksFuture.join();

                        metadataManager.setMetadata(users, trackers, priorities, statuses, categories, versions,
                                activities);
                        metadataManager.setCurrentUser(currentUser);
                        metadataLoaded = true;

                        if (currentUser != null) {
                            log(I18n.format("controller.debug.user", currentUser.name, currentUser.id));
                        }
                        log(I18n.format("controller.debug.metadata", rawTasks.size()));

                        taskManager.classifyTasks(rawTasks, config.includeEpics);
                        List<Task> effectiveTasks = taskManager.getCurrentTasks();
                        metadataManager.enrichMetadataFromTasks(effectiveTasks);

                        // Process metadata on EDT
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                List<SimpleEntity> cats = categories != null ? categories : new ArrayList<>();
                                Set<Integer> catIds = cats.stream().map(c -> c.id).collect(Collectors.toSet());
                                for (Task t : effectiveTasks) {
                                    if (t.categoryId > 0 && t.category != null && !t.category.isEmpty()
                                            && !catIds.contains(t.categoryId)) {
                                        cats.add(new SimpleEntity(t.categoryId, t.category));
                                        catIds.add(t.categoryId);
                                    }
                                }
                                cats.sort(Comparator.comparing(c -> c.name));

                                List<SimpleEntity> stats = statuses != null ? statuses : new ArrayList<>();
                                Set<Integer> statIds = stats.stream().map(s -> s.id).collect(Collectors.toSet());
                                for (Task t : effectiveTasks) {
                                    if (t.statusId > 0 && t.status != null && !t.status.isEmpty()
                                            && !statIds.contains(t.statusId)) {
                                        stats.add(new SimpleEntity(t.statusId, t.status));
                                        statIds.add(t.statusId);
                                    }
                                }
                                stats.sort(Comparator.comparing(s -> s.name));

                                if (versions != null)
                                    versions.sort(Comparator.comparing(v -> v.name));
                                if (activities != null)
                                    activities.sort(Comparator.comparing(a -> a.name));

                                List<SimpleEntity> activeStatuses = new ArrayList<>();
                                Set<Integer> activeIds = new HashSet<>();
                                for (Task t : effectiveTasks) {
                                    if (t.statusId > 0 && activeIds.add(t.statusId)) {
                                        activeStatuses.add(new SimpleEntity(t.statusId, t.status));
                                    }
                                }
                                activeStatuses.sort(Comparator.comparing(s -> s.name));

                                if (users != null) {
                                    users.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
                                }

                                if (currentUser != null) {
                                    view.setCurrentUser(currentUser);
                                }

                                view.bindData(effectiveTasks);
                                view.updateMultiSelectors(trackers, users, activeStatuses, cats);

                                if (selectedId != -1) {
                                    for (int i = 0; i < view.table.getRowCount(); i++) {
                                        try {
                                            Task t = view.model.getTaskAt(view.table.convertRowIndexToModel(i));
                                            if (t.id == selectedId) {
                                                view.table.setRowSelectionInterval(i, i);
                                                break;
                                            }
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }

                                log(I18n.format("controller.log.sync_count", effectiveTasks.size()));
                                checkNotifications(effectiveTasks);
                                view.setOfflineMode(false);
                                view.setLoading(false);

                                // Notify peers that my data has changed (so they can update clone status
                                // indicators)
                                notifyPeersOfUpdate();
                            } catch (Exception e) {
                                view.setLoading(false);
                                notificationService.showError(I18n.format("controller.error.process", e.getMessage()),
                                        e);
                            }
                        });
                    } catch (Exception e) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            view.setLoading(false);
                            view.setOfflineMode(true);
                            log("Aviso: No se pudo refrescar. Usando datos en caché (Modo Offline)");
                        });
                    }
                }).exceptionally(e -> {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        view.setLoading(false);
                        view.setOfflineMode(true);
                        log("Aviso: No se pudo refrescar. Usando datos en caché (Modo Offline)");
                    });
                    return null;
                });
    }

    private void notifyPeersOfUpdate() {
        if (viewManager.getPeers() != null) {
            for (InstanceController peer : viewManager.getPeers()) {
                peer.onPeerDataUpdated(this);
            }
        }
    }

    public void onPeerDataUpdated(InstanceController peer) {
        // When a peer updates, its tasks might have changed.
        // If we are displaying "Clone Status" (colors), we need to repaint to reflect
        // new matches.
        javax.swing.SwingUtilities.invokeLater(() -> {
            view.model.fireTableDataChanged();
            // log("Refreshed view because peer '" + peer.getTitle() + "' updated data.");
        });
    }

    // --- Delegation Getters for Managers ---
    public List<SimpleEntity> getUsers() {
        return metadataManager.getUsers();
    }

    public List<SimpleEntity> getTrackers() {
        return metadataManager.getTrackers();
    }

    public List<Task> getTasks() {
        return view.getTasks();
    }

    public List<SimpleEntity> getPriorities() {
        return metadataManager.getPriorities();
    }

    public List<SimpleEntity> getStatuses() {
        return metadataManager.getStatuses();
    }

    public List<SimpleEntity> getCategories() {
        return metadataManager.getCategories();
    }

    public List<SimpleEntity> getVersions() {
        return metadataManager.getVersions();
    }

    public List<SimpleEntity> getActivities() {
        return metadataManager.getActivities();
    }

    public SimpleEntity getCurrentUser() {
        return metadataManager.getCurrentUser();
    }

    public List<Task> getCurrentTasks() {
        return taskManager.getCurrentTasks();
    }

    public List<Task> getEpicTasks() {
        return taskManager.getEpicTasks();
    }

    public List<Task> getAllLoadedTasks() {
        return taskManager.getAllLoadedTasks();
    }

    private void downloadTaskToDesktop(Task stub) {
        taskOperations.downloadTaskToDesktop(stub);
    }

    public void promptClone(Task originalStub, InstanceController sourceController) {
        taskOperations.promptClone(originalStub, sourceController);
    }

    private void openCreateDialog(Task template) {
        openCreateDialog(template, null, 0);
    }

    public void forceRefreshMetadata() {
        metadataLoaded = false;
        refreshData();
    }

    public StyleConfig getStyleConfig() {
        return styleConfig;
    }

    public void openCreateDialog(Task template, DataService sourceService) {
        openCreateDialog(template, sourceService, 0);
    }

    public boolean isClosedStatus(String status) {
        if (status == null)
            return false;

        // 1. Try metadata first (reliable)
        List<SimpleEntity> stats = metadataManager.getStatuses();
        if (stats != null) {
            for (SimpleEntity se : stats) {
                if (status.equalsIgnoreCase(se.name)) {
                    return se.isClosed;
                }
            }
        }

        // 2. Fallback to patterns
        String s = status.toLowerCase();
        return s.contains("close") || s.contains("cerrad") || s.contains("resol") || s.contains("resuel")
                || s.contains("termin") || s.contains("fin")
                || s.contains("reject") || s.contains("rechaz");
    }

    public Task findMatch(Task incomingTask) {
        return findMatch(incomingTask, null);
    }

    public Task findMatch(Task incomingTask, ConnectionConfig sourceConfig) {
        if (incomingTask == null)
            return null;
        int incomingId = incomingTask.id;

        // 1. Direct ID match
        List<Task> pool = taskManager.getAllLoadedTasks().isEmpty() ? taskManager.getCurrentTasks()
                : taskManager.getAllLoadedTasks();
        for (Task t : pool) {
            if (t.id == incomingId)
                return t;
        }

        // 2. Pattern Match
        Pattern localPattern = config.getExtractionPattern();
        String localSearchString = config.formatReference(incomingId);

        Pattern sourcePattern = sourceConfig != null ? sourceConfig.getExtractionPattern() : null;
        String sourceSearchString = sourceConfig != null ? sourceConfig.formatReference(incomingId) : null;

        for (Task t : pool) {
            if (t.subject == null)
                continue;

            // A. Target task has source ID in its subject (most common)
            if (sourceSearchString != null && t.subject.contains(sourceSearchString))
                return t;
            if (localSearchString != null && t.subject.contains(localSearchString))
                return t;

            if (sourcePattern != null) {
                Matcher m = sourcePattern.matcher(t.subject);
                if (m.find()) {
                    try {
                        if (Integer.parseInt(m.group(1)) == incomingId)
                            return t;
                    } catch (Exception ignored) {
                    }
                }
            }

            // B. Source task has target ID in its subject
            if (localPattern != null && incomingTask.subject != null) {
                Matcher m = localPattern.matcher(incomingTask.subject);
                if (m.find()) {
                    try {
                        if (Integer.parseInt(m.group(1)) == t.id)
                            return t;
                    } catch (Exception ignored) {
                    }
                }
            }

            // C. Subject equality fallback
            if (incomingTask.subject != null) {
                String s1 = view.normalize(incomingTask.subject);
                String s2 = view.normalize(t.subject);
                if (s1.length() > 5 && s2.length() > 5 && s1.equals(s2)) {
                    return t;
                }
            }
        }
        return null;
    }

    public void handleTwinClosures(List<Task> sourceTasks) {
        List<InstanceController> peers = viewManager.getPeers();
        if (peers == null || peers.isEmpty() || sourceTasks == null || sourceTasks.isEmpty())
            return;

        log("Sincronización: Buscando tareas gemelas en " + peers.size() + " proyectos adicionales.");
        int matchCount = 0;

        for (InstanceController peer : peers) {
            if (peer == this)
                continue; // Safety check
            log("Sincronización: Comprobando proyecto '" + peer.getTitle() + "'...");
            List<Task> twinsToClose = new java.util.ArrayList<>();
            for (Task src : sourceTasks) {
                Task twin = peer.findMatch(src, this.config);
                if (twin != null) {
                    log("Sincronización: ¡Coincidencia encontrada! #" + twin.id + " en '" + peer.getTitle() + "'");
                    // Solo sugerir si no está ya cerrada en el destino
                    if (!peer.isClosedStatus(twin.status)) {
                        twinsToClose.add(twin);
                    } else {
                        log("Sincronización: Se ignora #" + twin.id + " porque ya está cerrada.");
                    }
                }
            }

            if (!twinsToClose.isEmpty()) {
                matchCount += twinsToClose.size();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    peer.dialogManager.openTwinClosureDialog(twinsToClose, this);
                });
            }
        }

        if (matchCount > 0) {
            log("Sincronización: Se encontraron " + matchCount + " tareas gemelas para cerrar.");
        }
    }

    public void performTwinClosure(java.util.List<Task> twins, SimpleEntity version, SimpleEntity status,
            SimpleEntity assignment, InstanceController source) {
        taskOperations.performTwinClosure(twins, version, status, assignment, source);
    }

    public DialogManager getDialogManager() {
        return dialogManager;
    }

    private void openCreateDialog(Task template, DataService sourceService, int originalTaskId) {
        dialogManager.openCreateDialog(template, sourceService, originalTaskId);
    }

    void performCreate(Task t, DataService src) {
        performCreate(t, src, null);
    }

    void performCreate(Task t, DataService src, java.util.function.Consumer<Integer> onSuccess) {
        taskOperations.performCreate(t, src, onSuccess);
    }

    void performUpdate(Task t) {
        taskOperations.performUpdate(t, null, null);
    }

    public void openBulkUpdateDialog() {
        dialogManager.openBulkUpdateDialog();
    }

    void performBulkUpdate(int[] rows, SimpleEntity s, SimpleEntity p, SimpleEntity a, SimpleEntity cat,
            SimpleEntity v, Integer doneRatio, String note) {
        taskOperations.performBulkUpdate(rows, s, p, a, cat, v, doneRatio, note);
    }

    public void openMultiCloseDialog() {
        dialogManager.openMultiCloseDialog();
    }

    public void performMultiClose(java.util.List<Task> tasks, SimpleEntity version, SimpleEntity selectedStatus,
            SimpleEntity assignment) {
        taskOperations.performMultiClose(tasks, version, selectedStatus, assignment);
    }

    public void exportToCsv() {
        exportManager.exportToCsv(taskManager.getCurrentTasks());
    }

    public void showKeywordAnalysis() {
        if (taskManager.getCurrentTasks().isEmpty()) {
            notificationService.showInfo(I18n.get("controller.msg.no_tasks"));
            return;
        }
        KeywordAnalysisDialog dialog = new KeywordAnalysisDialog(SwingUtilities.getWindowAncestor(view), getTitle(),
                taskManager.getCurrentTasks(), this);
        dialog.setVisible(true);
    }

    public void openReportsDialog() {
        exportManager.openReportsDialog(getTitle(), service, config.projectId, taskManager.getCurrentTasks());
    }

    public void openConfigDialog() {
        log(I18n.get("controller.log.config"));
        InstanceConfigDialog d = new InstanceConfigDialog((Frame) SwingUtilities.getWindowAncestor(view), configPrefix,
                getTitle());
        d.onSave(this::reloadConfig);
        d.setVisible(true);
    }

    public void openColorConfigDialog() {
        log(I18n.get("controller.log.colors"));
        StatusColorDialog d = new StatusColorDialog((Frame) SwingUtilities.getWindowAncestor(view), configPrefix,
                getTitle(),
                metadataManager.getStatuses());
        d.onSave(this::reloadConfig);
        d.setVisible(true);
    }

    public void openVersionManager() {
        if (config.projectId == null || config.projectId.isEmpty()) {
            notificationService.showWarning(I18n.get("controller.warn.project_id"));
            return;
        }
        VersionManagerDialog d = new VersionManagerDialog(SwingUtilities.getWindowAncestor(view), getTitle(), service,
                config.projectId, config.clientName, this::forceRefreshMetadata);
        d.setVisible(true);
    }

    public void openWikiManager() {
        if (config.projectId == null || config.projectId.isEmpty()) {
            notificationService.showWarning(I18n.get("controller.warn.project_id"));
            return;
        }
        log(I18n.get("controller.log.wiki"));
        WikiManagerDialog d = new WikiManagerDialog(SwingUtilities.getWindowAncestor(view), getTitle(), service,
                config.url,
                config.projectId, config.clientName, this.logger);
        d.setVisible(true);
    }

    public void openHelpDialog() {
        HelpDialog d = new HelpDialog((Frame) SwingUtilities.getWindowAncestor(view));
        d.setVisible(true);
    }

    void reloadConfig() {
        log(I18n.get("controller.log.reload"));
        Properties p = ConfigManager.loadConfig();
        this.config = new ConnectionConfig(configPrefix, p);
        view.updateTitle(computeTitle());
        this.styleConfig.load(p, configPrefix);
        view.table.repaint();
        initService();
        initAutoRefresh();
        notificationService.showInfo(I18n.format("controller.msg.config_saved", getTitle()));
        refreshData();
    }

    /**
     * Configura atajos de teclado para acciones comunes.
     */
    private void setupKeyboardShortcuts() {
        KeyboardShortcutManager shortcuts = new KeyboardShortcutManager(view);

        // Help (F1)
        shortcuts.registerShortcut("help",
                KeyboardShortcutManager.CommonShortcuts.HELP,
                () -> {
                    log("Ayuda (F1): Abriendo diálogo de ayuda");
                    openHelpDialog();
                });

        // New Task
        shortcuts.registerShortcut("newTask",
                KeyboardShortcutManager.CommonShortcuts.NEW_TASK,
                () -> {
                    log(I18n.get("controller.log.shortcut.new"));
                    openCreateDialog(null);
                });

        // Refresh
        shortcuts.registerShortcut("refresh",
                KeyboardShortcutManager.CommonShortcuts.REFRESH,
                () -> {
                    log(I18n.get("controller.log.shortcut.refresh"));
                    refreshData();
                });

        // Find (Focus Search)
        shortcuts.registerShortcut("find",
                KeyboardShortcutManager.CommonShortcuts.FIND,
                () -> {
                    log("Atajo Ctrl+F: Enfocando búsqueda");
                    if (view.getFiltersPanel() != null) {
                        view.getFiltersPanel().getTxtSearch().requestFocusInWindow();
                        view.getFiltersPanel().getTxtSearch().selectAll();
                    }
                });

        // Open
        shortcuts.registerShortcut("open",
                KeyboardShortcutManager.CommonShortcuts.OPEN,
                () -> {
                    Task selected = view.getSelectedTask();
                    if (selected != null) {
                        log("Atajo Enter: Abriendo tarea #" + selected.id);
                        view.setLoading(true);
                        new SwingWorker<Task, Void>() {
                            @Override
                            protected Task doInBackground() throws Exception {
                                return service.fetchTaskDetails(selected.id);
                            }

                            @Override
                            protected void done() {
                                view.setLoading(false);
                                try {
                                    openCreateDialog(get());
                                } catch (Exception ex) {
                                    log("Aviso: No se cargaron detalles completos. " + ex.getMessage());
                                    openCreateDialog(selected);
                                }
                            }
                        }.execute();
                    }
                });

        // Download (Ctrl+D)
        shortcuts.registerShortcut("download",
                KeyboardShortcutManager.CommonShortcuts.DOWNLOAD,
                () -> {
                    Task selected = view.getSelectedTask();
                    if (selected != null) {
                        log("Atajo Ctrl+D: Descargando tarea #" + selected.id);
                        downloadTaskToDesktop(selected);
                    }
                });

        // Copy ID
        shortcuts.registerShortcut("copyId",
                KeyboardShortcutManager.CommonShortcuts.COPY_ID,
                () -> {
                    Task selected = view.getSelectedTask();
                    if (selected != null) {
                        view.copyToClip(String.valueOf(selected.id));
                        log("Atajo Ctrl+Shift+C: ID copiado: " + selected.id);
                    }
                });
    }

    /**
     * Abre el dashboard de métricas para este cliente.
     */
    public void openDashboard() {
        JDialog dashboardDialog = new JDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(view),
                "📊 Dashboard - " + getTitle(),
                false);
        dashboardDialog.setSize(1200, 700);
        dashboardDialog.setLocationRelativeTo(view);

        MetricsDashboard dashboard = new MetricsDashboard();
        dashboard.updateMetrics(taskManager.getCurrentTasks());

        dashboardDialog.add(dashboard);
        dashboardDialog.setVisible(true);

        log("Dashboard de métricas abierto");
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    public AsyncDataService getAsyncService() {
        return asyncService;
    }

    public String getTitle() {
        return viewManager.getTitle();
    }

    public DataService getService() {
        return service;
    }

    void onLogTime(int taskId, String date, double hours, int userId, int activityId, String comment) {
        view.setLoading(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.logTime(taskId, date, hours, userId, activityId, comment);
                return null;
            }

            @Override
            protected void done() {
                view.setLoading(false);
                try {
                    get();
                    partialRefresh(taskId);
                    notificationService.showSuccess("Horas imputadas correctamente.");
                } catch (Exception e) {
                    log("Error logTime: " + e.getMessage());
                    notificationService.showError("Error al imputar horas: " + e.getMessage(), e);
                }
            }
        }.execute();
    }

    public void updateViewConfig(String widths, String visibility) {
        config.columnWidths = widths;
        config.columnVisibility = visibility;

        Properties props = ConfigManager.loadConfig();
        props.setProperty(configPrefix + ".columnWidths", widths);
        props.setProperty(configPrefix + ".columnVisibility", visibility);
        ConfigManager.saveConfig(props);
    }

    public void partialRefresh(int taskId) {
        view.setLoading(true);
        SwingWorkerFactory.executeAsync(
                () -> {
                    try {
                        return service.fetchTaskDetails(taskId);
                    } catch (Exception e) {
                        return null;
                    }
                },
                task -> {
                    view.setLoading(false);
                    if (task != null) {
                        view.model.updateTask(task);
                        log("Tarea #" + taskId + " refrescada localmente.");
                    }
                },
                err -> view.setLoading(false));

    }

    /**
     * Shuts down the controller and its services.
     */
    public void shutdown() {
        asyncService.shutdown();
    }

    /**
     * Checks for notification-worthy events in the task list.
     * Notifies about newly created tasks assigned to current user.
     * Uses tracking to prevent duplicate notifications.
     */
    private void checkNotifications(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        SimpleEntity currentUser = metadataManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Check for newly created tasks assigned to user (created in last hour)
        // Check for newly created tasks assigned to user (created in last hour)
        long oneHour = 60 * 60 * 1000L;
        long now = System.currentTimeMillis();
        for (Task task : tasks) {
            // Only notify about tasks assigned to current user
            if (task.assignedToId == currentUser.id && task.createdOn != null) {
                long timeSinceCreation = now - task.createdOn.getTime();
                // If created in last hour, notify (tracking prevents duplicates)
                if (timeSinceCreation > 0 && timeSinceCreation < oneHour) {
                    // Check if user is the author (created by themselves)
                    boolean createdByMe = task.authorId == currentUser.id;
                    String message = createdByMe
                            ? "Nueva tarea creada: " + task.subject
                            : "Nueva tarea asignada: " + task.subject;

                    redmineconnector.notifications.NotificationManager.createNotification(
                            redmineconnector.model.Notification.Type.NEW_TASK,
                            message,
                            task.id);
                }
            }
        }
    }
}
