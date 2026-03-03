package redmineconnector.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.StringSelection;
import javax.swing.SwingUtilities;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingWorker;
import redmineconnector.util.I18n;
import redmineconnector.model.Attachment;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import redmineconnector.config.ConnectionConfig;
import redmineconnector.config.StyleConfig;
import redmineconnector.model.SimpleEntity;
import redmineconnector.model.Task;
import redmineconnector.ui.components.EnhancedRenderer;
import redmineconnector.ui.components.FilterMultiSelect;
import redmineconnector.ui.dialogs.DatePickerPopup;
import redmineconnector.ui.components.FiltersPanel;
import redmineconnector.ui.components.TaskTablePanel;
import redmineconnector.ui.components.TaskTableModel;

public class InstanceView extends JPanel {
    private final StyleConfig styles;
    private FiltersPanel filtersPanel;

    // Components NOT in FiltersPanel
    JButton btnBulk = new JButton("✏️ Multi-Edit");
    JButton btnMultiClose = new JButton(I18n.get("version.btn.multiclose"));
    JButton btnDateClear = new JButton("✕"); // Should be removed if unused, but keeping simple for now

    // State
    private SimpleEntity currentUserEntity;
    private List<SimpleEntity> availableStatusesRef = new ArrayList<>();
    private final List<TableColumn> hiddenCols = new ArrayList<>();

    private TaskTablePanel taskTablePanel;

    private boolean firstLoad = true;
    JTable table;
    TaskTableModel model;
    TableRowSorter<TaskTableModel> sorter;
    JLabel lblStats = new JLabel("Listo");
    JLabel lblOffline = new JLabel(" MODO OFFLINE (SÓLO LECTURA) ");
    private redmineconnector.ui.components.QuickViewPanel quickViewPanel;
    private JComboBox<SimpleEntity> cbTimeUser = new JComboBox<>();
    private JComboBox<SimpleEntity> cbActivity = new JComboBox<>();
    private JTextField txtTimeDate = new JTextField(10);
    private JTextField txtTimeHours = new JTextField(5);
    private JTextField txtTimeComment = new JTextField(20);
    private JButton btnLogTimeForm = new JButton("⏰ " + I18n.get("task.form.btn.log_time"));
    // private JTabbedPane miniTabs; // Removed as it is now inside QuickViewPanel
    private JSplitPane mainSplit;
    private EnhancedRenderer renderer;
    private Consumer<Task> cloneAction;
    private Consumer<Task> smartMatchAction;
    private Consumer<Task> downloadAction;
    private Consumer<Task> createChildAction;
    private Consumer<List<Task>> multiTwinClosureAction;
    private InstanceController controller;

    // Image rendering support
    private int quickViewState = 0; // 0=Min, 1=30%, 2=50%
    private int lastQuickViewTaskId = -1; // Track last task shown in QuickView
    private redmineconnector.service.DataService dataService;

    public InstanceView(String title, StyleConfig styles) {
        super(new BorderLayout());
        this.styles = styles;
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        // Initialize FiltersPanel
        filtersPanel = new FiltersPanel(
                this::updateFilters,
                this::syncStatusFilterWithTracker,
                styles);

        // Setup internal components
        JPanel toolbar = filtersPanel; // Reusing the filters panel as toolbar
        JPanel tablePanel = createTablePanel();
        add(toolbar, BorderLayout.NORTH);

        quickViewPanel = new redmineconnector.ui.components.QuickViewPanel(this::changeQuickViewState, styles);
        quickViewPanel.addCustomTab("Tiempo", createQuickTimePanel());

        mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, quickViewPanel);
        mainSplit.setResizeWeight(1.0);
        mainSplit.setDividerLocation(600);
        mainSplit.setOneTouchExpandable(false);
        mainSplit.setEnabled(false);
        add(mainSplit, BorderLayout.CENTER);

        // Ensure it starts minimized once the layout is calculated and visible
        mainSplit.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int h = mainSplit.getHeight();
                if (h > 100) {
                    mainSplit.setDividerLocation(h - 30);
                    mainSplit.removeComponentListener(this);
                }
            }
        });

        add(createStatusBar(), BorderLayout.SOUTH);
        applyStyles(); // Initial apply
    }

    private void applyStyles() {
        // Self styles
        setBackground(styles.bgPanel);

        // Child components
        filtersPanel.applyStyles();
        quickViewPanel.applyStyles();
        taskTablePanel.applyStyles();

        // Update Time Panel components if needed (we create them raw below)
        updateTimePanelStyles();

        // Status bar
        JPanel statusBar = (JPanel) getComponent(getComponentCount() - 1); // createStatusBar result
        if (statusBar != null) {
            statusBar.setBackground(styles.bgHeader);
            lblStats.setForeground(styles.textSecondary);
        }

        revalidate();
        repaint();
    }

    private void updateTimePanelStyles() {
        java.awt.Component comp = quickViewPanel.getCustomTab("Tiempo");
        if (comp instanceof JPanel) {
            JPanel p = (JPanel) comp;
            p.setBackground(styles.bgPanel);
            // Update children if we had references, simpler to just set background for now
            // as most are inputs which handled by L&F or need explicit references.
            // We have references to fields:
            cbTimeUser.setBackground(styles.bgInput);
            cbTimeUser.setForeground(styles.textPrimary);
            cbActivity.setBackground(styles.bgInput);
            cbActivity.setForeground(styles.textPrimary);
            txtTimeDate.setBackground(styles.bgInput);
            txtTimeDate.setForeground(styles.textPrimary);
            txtTimeHours.setBackground(styles.bgInput);
            txtTimeHours.setForeground(styles.textPrimary);
            txtTimeComment.setBackground(styles.bgInput);
            txtTimeComment.setForeground(styles.textPrimary);

            btnLogTimeForm.setBackground(styles.bgMain);
            btnLogTimeForm.setForeground(styles.textPrimary);
        }
    }

    private void changeQuickViewState(int delta) {
        int newState = quickViewState + delta;
        if (newState < 0)
            newState = 0;
        if (newState > 2)
            newState = 2; // Clamp to max state

        if (newState == quickViewState)
            return; // No change

        quickViewState = newState;
        int h = mainSplit.getHeight();
        if (h <= 0)
            h = 600;

        switch (quickViewState) {
            case 0: // Minimized
                mainSplit.setDividerLocation(h - 30);
                break;
            case 1: // 30% Height
                mainSplit.setDividerLocation((int) (h * 0.7));
                break;
            case 2: // 50% Height
                mainSplit.setDividerLocation(h / 2);
                break;
        }
    }

    public void updateTitle(String newTitle) {
        // Border is MatteBorder, not TitledBorder - title updates not supported
        // This method is kept for API compatibility but does nothing
    }

    private void syncStatusFilterWithTracker() {
        if (model == null || model.data == null || availableStatusesRef == null)
            return;
        Set<String> selectedTrackers = filtersPanel.getMsTracker().getSelectedNames();
        Set<String> validStatusNames = new HashSet<>();
        for (Task t : model.data) {
            if (selectedTrackers.isEmpty() || (t.tracker != null && selectedTrackers.contains(t.tracker))) {
                if (t.status != null) {
                    validStatusNames.add(t.status);
                }
            }
        }
        List<SimpleEntity> filteredStatuses = new ArrayList<>();
        for (SimpleEntity se : availableStatusesRef) {
            if (validStatusNames.contains(se.name)) {
                filteredStatuses.add(se);
            }
        }
        filtersPanel.getMsStatus().setItems(filteredStatuses);
    }

    public void clearFilters() {
        if (filtersPanel != null) {
            filtersPanel.clearFilters();
            updateFilters();
        }
    }

    public FiltersPanel getFiltersPanel() {
        return filtersPanel;
    }

    private JPanel createTablePanel() {
        taskTablePanel = new TaskTablePanel(styles);
        this.model = taskTablePanel.getModel();
        this.table = taskTablePanel.getTable();
        this.sorter = taskTablePanel.getSorter();
        this.renderer = taskTablePanel.getRenderer();

        // Listeners for View logic
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadTaskDetailsForQuickView();
            }
        });

        sorter.addRowSorterListener(e -> updateStats());

        setupContextMenu();
        setupHeaderMenu();

        return taskTablePanel;
    }

    private void setupHeaderMenu() {
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    for (int i = 0; i < model.getColumnCount(); i++) {
                        String name = model.getColumnName(i);
                        JCheckBoxMenuItem item = new JCheckBoxMenuItem(name, true);
                        try {
                            table.getColumnModel().getColumn(table.convertColumnIndexToView(i));
                        } catch (Exception ex) {
                            item.setSelected(false);
                        }
                        final int modelIdx = i;
                        item.addActionListener(ev -> {
                            if (item.isSelected()) {
                                TableColumn col = null;
                                for (TableColumn c : hiddenCols)
                                    if (c.getModelIndex() == modelIdx)
                                        col = c;
                                if (col != null) {
                                    table.getColumnModel().addColumn(col);
                                    hiddenCols.remove(col);
                                }
                            } else {
                                int vIdx = table.convertColumnIndexToView(modelIdx);
                                TableColumn col = table.getColumnModel().getColumn(vIdx);
                                hiddenCols.add(col);
                                table.getColumnModel().removeColumn(col);
                            }
                            saveViewConfig();
                        });
                        menu.add(item);
                    }
                    menu.show(table.getTableHeader(), e.getX(), e.getY());
                }
            }
        });

        table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            public void columnAdded(javax.swing.event.TableColumnModelEvent e) {
            }

            public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {
            }

            public void columnMoved(javax.swing.event.TableColumnModelEvent e) {
                saveViewConfig();
            }

            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
            }

            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
            }
        });

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                saveViewConfig();
            }
        });

        // Register keyboard shortcuts
        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        redmineconnector.ui.input.KeyboardShortcutManager shortcuts = new redmineconnector.ui.input.KeyboardShortcutManager(
                this);

        // Navigation shortcuts
        shortcuts.registerShortcut("next_task",
                redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.NEXT_TASK,
                this::selectNextTask);
        shortcuts.registerShortcut("prev_task",
                redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.PREV_TASK,
                this::selectPreviousTask);

        // Action shortcuts
        shortcuts.registerShortcut("edit_task",
                redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.EDIT_TASK,
                () -> {
                    if (controller != null) {
                        Task selected = getSelectedTask();
                        if (selected != null) {
                            controller.onCreate(); // Opens edit dialog
                        }
                    }
                });

        shortcuts.registerShortcut("focus_search",
                redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.FOCUS_SEARCH,
                () -> {
                    if (filtersPanel != null) {
                        filtersPanel.getTxtSearch().requestFocus();
                    }
                });

        // Toggle QuickView with Q key (Cycle forward for now, or maybe toggle min/max?)
        // Let's make Q cycle 0->1->2->0 like before, but utilizing new method?
        // Actually, user didn't specify Q. Let's make Q just cycle +1, wrapping around?
        // Or better, let's keep the cycle behavior for Q for convenience.
        shortcuts.registerShortcut("toggle_quickview",
                redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.TOGGLE_QUICKVIEW,
                () -> {
                    // Cycle: 0 -> 1 -> 2 -> 0
                    int delta = 1;
                    if (quickViewState == 2) {
                        delta = -2; // 2 -> 0
                    }
                    changeQuickViewState(delta);
                });

        // Tab Navigation Shortcuts
        shortcuts.registerShortcut("next_tab",
                redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.NEXT_TAB_QUICKVIEW,
                () -> {
                    if (quickViewPanel != null)
                        quickViewPanel.cycleTab(1);
                });
        shortcuts.registerShortcut("prev_tab",
                redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.PREV_TAB_QUICKVIEW,
                () -> {
                    if (quickViewPanel != null)
                        quickViewPanel.cycleTab(-1);
                });

        // Note: The following shortcuts are disabled pending
        // FilterMultiSelect.selectOnly()
        // and ConnectionConfig.showClosed availability. Code preserved for future use.
        /*
         * shortcuts.registerShortcut("show_my_tasks",
         * redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.
         * SHOW_MY_TASKS,
         * () -> {
         * if (filtersPanel != null && currentUserEntity != null) {
         * filtersPanel.getMsAssigned().selectOnly(currentUserEntity.name);
         * updateFilters();
         * }
         * });
         * 
         * shortcuts.registerShortcut("toggle_closed",
         * redmineconnector.ui.input.KeyboardShortcutManager.CommonShortcuts.
         * TOGGLE_CLOSED,
         * () -> {
         * if (controller != null) {
         * controller.toggleShowClosed(!controller.getConfig().showClosed);
         * }
         * });
         */
    }

    public boolean selectMatch(Task criteria, ConnectionConfig criteriaConfig, ConnectionConfig myConfig) {
        if (criteria == null)
            return false;

        // 1. Try exact ID match first
        for (int i = 0; i < model.getRowCount(); i++) {
            Task t = model.getTaskAt(i);
            if (t.id == criteria.id) {
                selectRow(i);
                return true;
            }
        }

        // 2. Try Pattern Match (if configured)
        if (criteriaConfig != null && myConfig != null) {
            // 2. Try Pattern Match (Check if criteria subject matches my pattern)
            Pattern myPattern = myConfig.getExtractionPattern();
            if (myPattern != null) {
                Matcher m = myPattern.matcher(criteria.subject);
                if (m.find()) {
                    try {
                        int extractedId = Integer.parseInt(m.group(1));
                        for (int i = 0; i < model.getRowCount(); i++) {
                            Task t = model.getTaskAt(i);
                            if (t.id == extractedId) {
                                selectRow(i);
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            // 3. Try Reverse Pattern Match (Check if ANY of my tasks reference the criteria
            // ID)
            Pattern targetPattern = criteriaConfig.getExtractionPattern();
            if (targetPattern != null) {
                // Scan all local tasks
                for (int i = 0; i < model.getRowCount(); i++) {
                    Task t = model.getTaskAt(i);
                    Matcher m = targetPattern.matcher(t.subject);
                    if (m.find()) {
                        try {
                            int extractedId = Integer.parseInt(m.group(1));
                            if (extractedId == criteria.id) {
                                selectRow(i);
                                return true;
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        }
        return false;
    }

    private void selectRow(int modelRowIndex) {
        int viewRow = table.convertRowIndexToView(modelRowIndex);
        if (viewRow >= 0) {
            table.setRowSelectionInterval(viewRow, viewRow);
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        }
    }

    private void setupContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miCopy = new JMenuItem("🆔 Copiar ID");
        miCopy.addActionListener(e -> copyToClip(String.valueOf(getSelectedTask().id)));
        JMenuItem miOpenWeb = new JMenuItem("🌐 Abrir en Navegador");
        miOpenWeb.addActionListener(e -> {
            Task t = getSelectedTask();
            if (t != null && t.webUrl != null && Desktop.isDesktopSupported())
                try {
                    Desktop.getDesktop().browse(new URI(t.webUrl));
                } catch (Exception ex) {
                }
        });
        JMenuItem miCopySubDesc = new JMenuItem("📋 Copiar Asunto + Descripción");
        miCopySubDesc.addActionListener(e -> {
            Task t = getSelectedTask();
            if (t != null) {
                String text = t.subject + "\n\n" + (t.description != null ? t.description : "");
                copyToClip(text);
            }
        });
        JMenuItem miClone = new JMenuItem("🐑 Clonar a Destino");
        miClone.addActionListener(e -> {
            if (cloneAction != null && getSelectedTask() != null)
                cloneAction.accept(getSelectedTask());
        });
        JMenuItem miMatch = new JMenuItem("🔍 Buscar Pareja en Destino");
        miMatch.addActionListener(e -> {
            if (smartMatchAction != null && getSelectedTask() != null)
                smartMatchAction.accept(getSelectedTask());
        });
        JMenuItem miDownload = new JMenuItem("📂 Descargar todo a Escritorio");
        miDownload.addActionListener(e -> {
            if (downloadAction != null && getSelectedTask() != null)
                downloadAction.accept(getSelectedTask());
        });
        JMenuItem miChild = new JMenuItem("➕ Crear Subtarea / Hija");
        miChild.addActionListener(e -> {
            if (createChildAction != null && getSelectedTask() != null)
                createChildAction.accept(getSelectedTask());
        });

        JMenuItem miPin = new JMenuItem("⭐ Pin / Unpin");
        miPin.addActionListener(e -> {
            Task t = getSelectedTask();
            if (t != null && controller != null) {
                controller.togglePin(t.id);
            }
        });

        JMenuItem miMultiTwin = new JMenuItem("🔗 Multi-cierre Sincronizado");
        miMultiTwin.addActionListener(e -> {
            if (multiTwinClosureAction != null) {
                List<Task> selected = getSelectedTasks();
                if (!selected.isEmpty()) {
                    multiTwinClosureAction.accept(selected);
                }
            }
        });
        JMenuItem miMultiCloseLocal = new JMenuItem("✅ Multi-cerrar");
        miMultiCloseLocal.addActionListener(e -> {
            btnMultiClose.doClick();
        });

        menu.add(miCopy);
        menu.add(miCopySubDesc);
        menu.add(miMatch);
        menu.add(new JSeparator());
        menu.add(miClone);
        menu.add(miDownload);
        menu.add(miChild);
        menu.add(new JSeparator());
        menu.add(miMultiTwin);
        menu.add(miMultiCloseLocal);
        menu.add(new JSeparator());
        menu.add(miPin);
        menu.add(miOpenWeb);

        // Actualizar visibilidad de opciones según selección
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                int count = table.getSelectedRowCount();
                List<Task> selected = getSelectedTasks();
                boolean allHaveTwins = controller != null && !selected.isEmpty() && controller.hasAllTwins(selected);
                boolean hasMissingTwins = controller != null && !selected.isEmpty()
                        && controller.hasMissingTwins(selected);

                // Configurar etiqueta y visibilidad de cierre sincronizado
                if (count > 1) {
                    miMultiTwin.setText("🔗 Multi-cierre Sincronizado");
                } else {
                    miMultiTwin.setText("🔗 Cierre Sincronizado");
                }
                miMultiTwin.setVisible(count >= 1 && allHaveTwins);

                // Configurar etiqueta y visibilidad de cierre local
                if (count > 1) {
                    miMultiCloseLocal.setText("✅ Cerrar selección");
                } else {
                    miMultiCloseLocal.setText("✅ Cerrar");
                }
                miMultiCloseLocal.setVisible(count >= 1 && hasMissingTwins);

                // Opciones de tarea única
                boolean single = count == 1;
                miMatch.setVisible(single);
                miChild.setVisible(single);
                miCopy.setVisible(single);
                miCopySubDesc.setVisible(single);
                miOpenWeb.setVisible(single);
                miClone.setVisible(single);
                miDownload.setVisible(single);
                miPin.setVisible(single);
                if (single && controller != null) {
                    Task t = getSelectedTask();
                    miPin.setText(controller.isPinned(t.id) ? "⭐ Quitar Pin" : "⭐ Pin (Favorito)");
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });

        table.setComponentPopupMenu(menu);
    }

    public void copyToClip(String s) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
    }

    public void setCloneAction(Consumer<Task> action) {
        this.cloneAction = action;
    }

    public void setSmartMatchAction(Consumer<Task> action) {
        this.smartMatchAction = action;
    }

    public void setDownloadAction(Consumer<Task> action) {
        this.downloadAction = action;
    }

    public void setCreateChildAction(Consumer<Task> action) {
        this.createChildAction = action;
    }

    public void setMultiTwinClosureAction(Consumer<List<Task>> action) {
        this.multiTwinClosureAction = action;
    }

    private void initListeners(InstanceController controller) {
        filtersPanel.getBtnRefresh().addActionListener(e -> controller.onRefresh());
        filtersPanel.getBtnCreate().addActionListener(e -> controller.onCreate());
        filtersPanel.getBtnKeywords().addActionListener(e -> performKeywordSearch(controller));
    }

    public void setController(InstanceController controller) {
        this.controller = controller;
        if (model != null && controller != null) {
            model.setPinChecker(id -> controller.isPinned(id));
        }
        if (renderer != null && controller != null) {
            renderer.setPinChecker(id -> controller.isPinned(id));
        }
        initListeners(controller);
        if (quickViewPanel != null)
            quickViewPanel.setController(controller);
    }

    public void setDataService(redmineconnector.service.DataService service) {
        this.dataService = service;
        if (quickViewPanel != null) {
            quickViewPanel.setDataService(service);
        }
    }

    private void performKeywordSearch(InstanceController controller) {
        controller.showKeywordAnalysis();
    }

    private JPanel createStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        lblStats.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        p.add(lblStats);
        return p;
    }

    public void setLoading(boolean l) {
        filtersPanel.getBtnRefresh().setEnabled(!l);
        setCursor(l ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    public void bindData(List<Task> tasks) {
        model.setData(tasks);
        updateStats();
        if (firstLoad) {
            applyViewConfig();
            firstLoad = false;
        }
    }

    public void applyViewConfig() {
        if (controller == null || controller.getConfig() == null)
            return;
        ConnectionConfig cfg = controller.getConfig();

        // Apply Widths
        if (cfg.columnWidths != null && !cfg.columnWidths.isEmpty()) {
            String[] widths = cfg.columnWidths.split(",");
            for (String s : widths) {
                try {
                    String[] pair = s.split(":");
                    int idx = Integer.parseInt(pair[0]);
                    int w = Integer.parseInt(pair[1]);
                    if (idx >= 0 && idx < table.getColumnCount()) {
                        table.getColumnModel().getColumn(idx).setPreferredWidth(w);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Apply Visibility
        if (cfg.columnVisibility != null && !cfg.columnVisibility.isEmpty()) {
            String[] vis = cfg.columnVisibility.split(",");
            java.util.Set<Integer> visibleModelIdx = new java.util.HashSet<>();
            for (String s : vis) {
                try {
                    visibleModelIdx.add(Integer.parseInt(s));
                } catch (Exception ignored) {
                }
            }

            // Hide those not in the list
            for (int i = model.getColumnCount() - 1; i >= 0; i--) {
                if (!visibleModelIdx.isEmpty() && !visibleModelIdx.contains(i)) {
                    try {
                        int vIdx = table.convertColumnIndexToView(i);
                        if (vIdx != -1) {
                            TableColumn col = table.getColumnModel().getColumn(vIdx);
                            hiddenCols.add(col);
                            table.getColumnModel().removeColumn(col);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public void saveViewConfig() {
        if (controller == null || controller.getConfig() == null)
            return;

        // Save Widths
        StringBuilder sbW = new StringBuilder();
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            sbW.append(col.getModelIndex()).append(":").append(col.getWidth()).append(",");
        }

        // Save Visibility (which ones are currently in the view)
        StringBuilder sbV = new StringBuilder();
        for (int i = 0; i < table.getColumnCount(); i++) {
            sbV.append(table.getColumnModel().getColumn(i).getModelIndex()).append(",");
        }

        controller.updateViewConfig(sbW.toString(), sbV.toString());
    }

    private void updateStats() {
        lblStats.setText("Viendo: " + table.getRowCount() + " / " + model.getRowCount());
    }

    /**
     * Carga los detalles completos de la tarea seleccionada en background
     * antes de actualizar QuickView.
     */
    private void loadTaskDetailsForQuickView() {
        Task selectedTask = getSelectedTask();
        if (selectedTask == null) {
            quickViewPanel.updateTask(null);
            return;
        }

        if (selectedTask.id == lastQuickViewTaskId && selectedTask.isFullDetails) {
            return;
        }

        lastQuickViewTaskId = selectedTask.id;

        // Don't show placeholder - wait for full details to load
        // quickViewPanel.updateTask(selectedTask); // Removed - causes empty view

        if (dataService == null)
            return;

        // If journals/details missing, fetch them
        if (!selectedTask.isFullDetails) {
            final int taskId = selectedTask.id;
            SwingWorker<Task, Void> worker = new SwingWorker<Task, Void>() {
                @Override
                protected Task doInBackground() throws Exception {
                    return dataService.fetchTaskDetails(taskId);
                }

                @Override
                protected void done() {
                    try {
                        Task fullTask = get();
                        if (fullTask != null) {
                            // Update model
                            for (int i = 0; i < model.getRowCount(); i++) {
                                Task t = model.getTaskAt(i);
                                if (t.id == taskId) {
                                    t.journals = fullTask.journals;
                                    t.attachments = fullTask.attachments;
                                    t.description = fullTask.description;
                                    t.isFullDetails = true;
                                    break;
                                }
                            }
                            // Update View with full details
                            quickViewPanel.updateTask(fullTask);
                        }
                    } catch (Exception e) {
                    }
                }
            };
            worker.execute();
        } else {
            // Already has full details, update immediately
            quickViewPanel.updateTask(selectedTask);
        }
    }

    public void updateMultiSelectors(List<SimpleEntity> tr, List<SimpleEntity> us, List<SimpleEntity> st,
            List<SimpleEntity> ca) {
        this.availableStatusesRef = st;
        if (filtersPanel != null) {
            filtersPanel.getMsTracker().setItems(tr);

            List<SimpleEntity> usersWithMyself = new ArrayList<>();
            if (currentUserEntity != null) {
                usersWithMyself.add(new SimpleEntity(currentUserEntity.id, "⭐ A mí mismo"));
            }
            if (us != null) {
                usersWithMyself.addAll(us);
            }
            filtersPanel.getMsAssigned().setItems(usersWithMyself);

            filtersPanel.getMsStatus().setItems(st);
            filtersPanel.getMsCategory().setItems(ca);
        }

        // Update Time Activity Combo
        cbActivity.removeAllItems();
        if (controller != null && controller.getActivities() != null) {
            for (SimpleEntity a : controller.getActivities()) {
                cbActivity.addItem(a);
            }
        }
        // Update Time User Combo
        cbTimeUser.removeAllItems();
        cbTimeUser.addItem(new SimpleEntity(0, I18n.get("task.form.combo.select_user")));
        if (us != null) {
            for (SimpleEntity u : us) {
                cbTimeUser.addItem(u);
            }
        }
        if (currentUserEntity != null) {
            setCombo(cbTimeUser, currentUserEntity.id);
        }
    }

    private JPanel createQuickTimePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(5, 10, 5, 10));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 5, 2, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        p.add(new JLabel(I18n.get("task.form.label.time_user")), g);
        g.gridx = 1;
        p.add(cbTimeUser, g);
        g.gridx = 2;
        p.add(new JLabel(I18n.get("task.form.label.time_date")), g);
        g.gridx = 3;
        JPanel dateP = new JPanel(new BorderLayout());
        txtTimeDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        JButton btnPick = new JButton("📅");
        btnPick.addActionListener(e -> {
            DatePickerPopup popup = new DatePickerPopup(d -> txtTimeDate.setText(d));
            popup.show(btnPick, 0, btnPick.getHeight());
        });
        dateP.add(txtTimeDate, BorderLayout.CENTER);
        dateP.add(btnPick, BorderLayout.EAST);
        p.add(dateP, g);

        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("task.form.label.time_hours")), g);
        g.gridx = 1;
        JPanel hoursPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hoursPanel.setOpaque(false);
        hoursPanel.add(txtTimeHours);
        hoursPanel.add(Box.createHorizontalStrut(5));
        JButton btnPlusHalf = new JButton("+0.5");
        btnPlusHalf.setMargin(new Insets(1, 3, 1, 3));
        btnPlusHalf.addActionListener(e -> {
            try {
                String t = txtTimeHours.getText().trim();
                if (t.isEmpty())
                    t = "0";
                double val = Double.parseDouble(t.replace(",", "."));
                txtTimeHours.setText(String.valueOf(val + 0.5));
            } catch (Exception ex) {
                txtTimeHours.setText("0.5");
            }
        });
        JButton btn7 = new JButton("7h");
        btn7.setMargin(new Insets(1, 3, 1, 3));
        btn7.addActionListener(e -> txtTimeHours.setText("7"));
        JButton btn8 = new JButton("8.5h");
        btn8.setMargin(new Insets(1, 3, 1, 3));
        btn8.addActionListener(e -> txtTimeHours.setText("8.5"));
        hoursPanel.add(btnPlusHalf);
        hoursPanel.add(btn7);
        hoursPanel.add(btn8);
        p.add(hoursPanel, g);

        g.gridx = 2;
        p.add(new JLabel(I18n.get("task.form.label.time_activity")), g);
        g.gridx = 3;
        p.add(cbActivity, g);

        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("task.form.label.time_comment")), g);
        g.gridx = 1;
        g.gridwidth = 2;
        p.add(txtTimeComment, g);
        g.gridx = 3;
        g.gridwidth = 1;
        btnLogTimeForm.setBackground(new Color(220, 240, 220));
        btnLogTimeForm.addActionListener(e -> {
            Task t = getSelectedTask();
            if (t == null)
                return;
            try {
                double h = Double.parseDouble(txtTimeHours.getText().replace(",", "."));
                String dStr = txtTimeDate.getText().trim();
                Date d = new SimpleDateFormat("dd/MM/yyyy").parse(dStr);
                String isoDate = new SimpleDateFormat("yyyy-MM-dd").format(d);
                SimpleEntity user = (SimpleEntity) cbTimeUser.getSelectedItem();
                int uid = user != null ? user.id : 0;
                SimpleEntity act = (SimpleEntity) cbActivity.getSelectedItem();
                int actId = act != null ? act.id : 0;
                String comment = txtTimeComment.getText();

                if (controller != null) {
                    controller.onLogTime(t.id, isoDate, h, uid, actId, comment);
                    txtTimeHours.setText("");
                    txtTimeComment.setText("");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error en formato de datos: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        p.add(btnLogTimeForm, g);

        wrapper.add(p, BorderLayout.NORTH);
        return wrapper;
    }

    private void setCombo(JComboBox<SimpleEntity> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).id == id) {
                cb.setSelectedIndex(i);
                return;
            }
        }
    }

    public void setCurrentUser(SimpleEntity user) {
        this.currentUserEntity = user;
    }

    public Task getSelectedTask() {
        int r = table.getSelectedRow();
        return r == -1 ? null : model.getTaskAt(table.convertRowIndexToModel(r));
    }

    public List<Task> getSelectedTasks() {
        int[] rows = table.getSelectedRows();
        List<Task> list = new ArrayList<>();
        for (int r : rows) {
            list.add(model.getTaskAt(table.convertRowIndexToModel(r)));
        }
        return list;
    }

    /**
     * Selects the next task in the table (keyboard shortcut J).
     */
    public void selectNextTask() {
        int current = table.getSelectedRow();
        if (current < table.getRowCount() - 1) {
            table.setRowSelectionInterval(current + 1, current + 1);
            table.scrollRectToVisible(table.getCellRect(current + 1, 0, true));
        }
    }

    /**
     * Selects the previous task in the table (keyboard shortcut K).
     */
    public void selectPreviousTask() {
        int current = table.getSelectedRow();
        if (current > 0) {
            table.setRowSelectionInterval(current - 1, current - 1);
            table.scrollRectToVisible(table.getCellRect(current - 1, 0, true));
        }
    }

    /**
     * Finds and selects a task by its ID in the table.
     * Clears search filters and scrolls the task into view.
     */
    public void selectTaskById(int taskId) {
        // 1. Clear search filters to make sure the task is visible
        if (filtersPanel != null) {
            filtersPanel.getTxtSearch().setText("");
            filtersPanel.getTxtIdSearch().setText("");
        }
        updateFilters();

        // 2. Find and select the row
        for (int i = 0; i < table.getRowCount(); i++) {
            Task t = model.getTaskAt(table.convertRowIndexToModel(i));
            if (t.id == taskId) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                break;
            }
        }
    }

    public String normalize(String s) {
        return s.replaceAll("^(\\s*\\[[^\\]]+\\]\\s*)*", "").replaceAll("(?i)^(Re:|Fwd:|Rv:|Enc:)\\s*", "").trim()
                .toLowerCase();
    }

    public void setSearchText(String text) {
        if (filtersPanel != null) {
            filtersPanel.getTxtSearch().setText(text);
        }
    }

    public void updateFilters() {
        if (filtersPanel == null)
            return;

        if (controller != null)
            controller.log("Iniciando filtrado...");

        List<RowFilter<Object, Object>> fs = new ArrayList<>();
        String txt = filtersPanel.getTxtSearch().getText().trim();
        String idTxt = filtersPanel.getTxtIdSearch().getText().trim();
        String excludeTxt = filtersPanel.getTxtExclude().getText().trim();
        String dFrom = filtersPanel.getTxtDateFrom().getText().trim();
        String dTo = filtersPanel.getTxtDateTo().getText().trim();

        if (controller != null) {
            controller.log("Filtros: Search='" + txt + "', ID='" + idTxt + "', Dates=" + dFrom + "-" + dTo);
            controller.log("Asignados Seleccionados: " + filtersPanel.getMsAssigned().getSelectedNames());
        }

        if (!txt.isEmpty())
            fs.add(RowFilter.regexFilter("(?i)" + Pattern.quote(txt)));
        if (!idTxt.isEmpty())
            fs.add(RowFilter.regexFilter("^" + Pattern.quote(idTxt), 0));

        if (!excludeTxt.isEmpty()) {
            final String[] exclusions = excludeTxt.split(",");
            fs.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<?, ?> entry) {
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        String s = entry.getStringValue(i);
                        for (String ex : exclusions) {
                            if (s.toLowerCase().contains(ex.trim().toLowerCase()))
                                return false;
                        }
                    }
                    return true;
                }
            });
        }

        addMultiFilter(fs, filtersPanel.getMsTracker(), 4);
        addMultiFilter(fs, filtersPanel.getMsStatus(), 2);
        addMultiFilter(fs, filtersPanel.getMsCategory(), 5); // Corrected from 8 to 5
        addAssignmentFilter(fs, filtersPanel.getMsAssigned(), 7); // Corrected from 6 to 7

        final boolean isFromValid = dFrom.matches("\\d{2}/\\d{2}/\\d{4}");
        final boolean isToValid = dTo.matches("\\d{2}/\\d{2}/\\d{4}");

        if (isFromValid || isToValid) {
            fs.add(new RowFilter<Object, Object>() {

                @Override
                public boolean include(Entry<?, ?> entry) {
                    try {
                        Date taskDate = (Date) entry.getValue(10); // "Fecha" (CreatedOn) is col 10
                        if (taskDate == null)
                            return false;
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        Date d1 = isFromValid ? sdf.parse(dFrom) : null;
                        Date d2 = isToValid ? sdf.parse(dTo) : null;

                        // Debug log for first few rows just to see? No, avoiding flood. But logging
                        // valid dates once:
                        // if (controller != null) controller.log("Filtrando fecha tarea: " + taskDate);

                        if (d1 != null && taskDate.before(d1))
                            return false;
                        // For 'To', we should include the whole day, so check against next day or just
                        // careful comparison
                        // If d2 is 22/12/2025 00:00, and task is 22/12/2025 10:00, it might fail check
                        // if we don't adjust
                        if (d2 != null) {
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(d2);
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                            cal.set(java.util.Calendar.MINUTE, 59);
                            cal.set(java.util.Calendar.SECOND, 59);
                            if (taskDate.after(cal.getTime()))
                                return false;
                        }
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }

            });
        }

        sorter.setRowFilter(RowFilter.andFilter(fs));
        updateStats();
    }

    private void addMultiFilter(List<RowFilter<Object, Object>> fs, FilterMultiSelect ms, int col) {
        Set<String> selected = ms.getSelectedNames();
        if (!selected.isEmpty()) {
            fs.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<?, ?> entry) {
                    try {
                        String val = entry.getStringValue(col);
                        return selected.contains(val);
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
        }
    }

    private void addAssignmentFilter(List<RowFilter<Object, Object>> fs, FilterMultiSelect ms, int col) {
        Set<String> selected = ms.getSelectedNames();
        if (!selected.isEmpty()) {
            fs.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<?, ?> entry) {
                    try {
                        String val = entry.getStringValue(col);

                        // Handle special cases
                        for (String selectedName : selected) {
                            if (selectedName.equals("⭐ A mí mismo")) {
                                // Only match if task is assigned AND assigned to current user
                                if (currentUserEntity != null && val != null
                                        && !val.trim().isEmpty() && !val.equals("-")
                                        && val.trim().equalsIgnoreCase(currentUserEntity.name.trim())) {
                                    return true;
                                }
                            } else if (selectedName.equals("⚪ Sin Asignar")) {
                                if (val == null || val.trim().isEmpty() || val.equals("-")) {
                                    return true;
                                }
                            } else if (selectedName.equals(val)) {
                                return true;
                            }
                        }
                        return false;
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
        }
    }

    public void setOfflineMode(boolean offline) {
        if (filtersPanel != null) {
            filtersPanel.setOfflineMode(offline);
        }
        btnBulk.setEnabled(!offline);
        btnMultiClose.setEnabled(!offline);
        btnLogTimeForm.setEnabled(!offline);
    }

    public List<Task> getTasks() {
        return model != null ? model.data : new ArrayList<>();
    }
}
