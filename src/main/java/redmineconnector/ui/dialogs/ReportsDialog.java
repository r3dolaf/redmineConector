package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.Insets;

import redmineconnector.model.Task;
import redmineconnector.model.TimeEntry;
import redmineconnector.model.VersionDTO;
import redmineconnector.service.DataService;
import redmineconnector.ui.UIHelper;
import redmineconnector.util.I18n;

public class ReportsDialog extends JDialog {
    private final DataService service;
    private final String projectId;
    private final List<Task> currentViewTasks;
    private final JTextField txtFrom = new JTextField(10);
    private final JTextField txtTo = new JTextField(10);
    private final JButton btnGenerate = new JButton("Generar Informes (Fecha)");
    private final JTabbedPane tabs;
    private final DefaultTableModel modelHours;
    private final DefaultTableModel modelTasks;
    private final DefaultTableModel modelCategories;
    private final JTable tableHours;
    private final JTable tableTasks;
    private final JTable tableCategories;
    private final JTextArea txtVersionsReport = new JTextArea();

    // NUEVOS CAMPOS PARA EL INFORME DE ESTADO Y ASIGNADO
    private DefaultTableModel modelStatusAssignee;
    private JTable tableStatusAssignee;

    public ReportsDialog(Window owner, String title, DataService service, String projectId, List<Task> currentTasks) {
        super(owner, I18n.format("reports.dialog.title", title), ModalityType.MODELESS);
        this.service = service;
        this.projectId = projectId;
        this.currentViewTasks = currentTasks;
        setSize(1100, 750);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        UIHelper.addEscapeListener(this);

        tabs = new JTabbedPane();

        if (currentTasks != null) {
            JPanel statsPanel = new JPanel(new BorderLayout());
            JTabbedPane statsTabs = new JTabbedPane();
            statsTabs.addTab(I18n.get("reports.tab.general"), StatisticsDialog.createGeneralTab(currentTasks));
            statsTabs.addTab(I18n.get("reports.tab.matrix"), StatisticsDialog.createMatrixTab(currentTasks));
            statsTabs.addTab(I18n.get("reports.tab.timeline"), StatisticsDialog.createTimelineTab(currentTasks));
            statsPanel.add(statsTabs, BorderLayout.CENTER);
            tabs.addTab(I18n.get("reports.tab.visual"), statsPanel);
        }

        JPanel reportsContainer = new JPanel(new BorderLayout());

        // Split Top Bar into Filters (Left) and Config (Right)
        JPanel topContainer = new JPanel(new BorderLayout());
        JPanel pFilters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel pConfig = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        txtFrom.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        txtTo.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        JButton btnThisMonth = new JButton("Mes Actual");
        btnThisMonth.setMargin(new Insets(2, 5, 2, 5));
        btnThisMonth.addActionListener(e -> {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 1);
            txtFrom.setText(new SimpleDateFormat("dd/MM/yyyy").format(c.getTime()));
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            txtTo.setText(new SimpleDateFormat("dd/MM/yyyy").format(c.getTime()));
        });
        JButton btnPrevMonth = new JButton("Mes Anterior");
        btnPrevMonth.setMargin(new Insets(2, 5, 2, 5));
        btnPrevMonth.addActionListener(e -> shiftDate(Calendar.MONTH, -1));
        JButton btnPrevYear = new JButton("Año Anterior");
        btnPrevYear.setMargin(new Insets(2, 5, 2, 5));
        btnPrevYear.addActionListener(e -> shiftDate(Calendar.YEAR, -1));

        pFilters.add(new JLabel("Desde:"));
        pFilters.add(createDatePanel(txtFrom));
        pFilters.add(new JLabel("Hasta:"));
        pFilters.add(createDatePanel(txtTo));
        pFilters.add(btnThisMonth);
        pFilters.add(btnPrevMonth);
        pFilters.add(btnPrevYear);

        btnGenerate.addActionListener(e -> generate());
        pFilters.add(btnGenerate);

        // Config Button on the Right
        JButton btnConfig = new JButton("⚙️ Configurar Informes");
        btnConfig.setMargin(new Insets(2, 5, 2, 5));
        btnConfig.addActionListener(e -> {
            new TargetConfigDialog(this, targetProfiles).setVisible(true);
        });
        pConfig.add(btnConfig);

        topContainer.add(pFilters, BorderLayout.CENTER);
        topContainer.add(pConfig, BorderLayout.EAST);

        reportsContainer.add(topContainer, BorderLayout.NORTH);

        JTabbedPane reportTabs = new JTabbedPane();
        modelHours = new DefaultTableModel();
        tableHours = new JTable(modelHours);
        reportTabs.addTab(I18n.get("reports.tab.hours"), new JScrollPane(tableHours));

        modelTasks = new DefaultTableModel(new String[] {
                I18n.get("reports.col.task_id"),
                I18n.get("reports.col.subject"),
                I18n.get("reports.col.total_hours") }, 0);
        tableTasks = new JTable(modelTasks);
        tableTasks.getColumnModel().getColumn(0).setPreferredWidth(60);
        tableTasks.getColumnModel().getColumn(1).setPreferredWidth(500);
        tableTasks.getColumnModel().getColumn(2).setPreferredWidth(80);
        reportTabs.addTab(I18n.get("reports.tab.tasks"), new JScrollPane(tableTasks));

        JPanel pVer = new JPanel(new BorderLayout());
        txtVersionsReport.setEditable(false);
        txtVersionsReport.setFont(new Font("Monospaced", Font.PLAIN, 12));
        pVer.add(new JScrollPane(txtVersionsReport), BorderLayout.CENTER);
        reportTabs.addTab(I18n.get("reports.tab.versions"), pVer);

        modelCategories = new DefaultTableModel(new String[] {
                I18n.get("reports.col.category"),
                I18n.get("reports.col.closed_count") }, 0);
        tableCategories = new JTable(modelCategories);
        reportTabs.addTab(I18n.get("reports.tab.categories"), new JScrollPane(tableCategories));

        // NUEVA PESTAÑA: Resumen de Tareas por Estado y Asignado
        // Este informe utiliza las tareas actualmente cargadas en la vista principal
        // (currentViewTasks).
        reportTabs.addTab(I18n.get("reports.tab.status_assignee"), createSummaryByStatusAndAssigneeTab());

        reportsContainer.add(reportTabs, BorderLayout.CENTER);
        tabs.addTab(I18n.get("reports.tab.historical"), reportsContainer);

        add(tabs, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCopy = new JButton("📋 Copiar al Portapapeles (Tabla Activa)");
        btnCopy.addActionListener(e -> {
            Component comp = tabs.getSelectedComponent();
            if (comp == reportsContainer) {
                int rIdx = reportTabs.getSelectedIndex();
                if (rIdx == 0)
                    copyTableToClipboard(tableHours);
                else if (rIdx == 1)
                    copyTableToClipboard(tableTasks);
                else if (rIdx == 2) {
                    txtVersionsReport.selectAll();
                    txtVersionsReport.copy();
                    JOptionPane.showMessageDialog(this, "Informe de versiones copiado.");
                } else if (rIdx == 3)
                    copyTableToClipboard(tableCategories);
                // NUEVA CONDICIÓN PARA LA NUEVA PESTAÑA
                else if (rIdx == 4)
                    copyTableToClipboard(tableStatusAssignee);
            } else {
                JOptionPane.showMessageDialog(this, "La copia está habilitada solo para las tablas de informes.");
            }
        });
        bottomPanel.add(btnCopy);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void copyTableToClipboard(JTable table) {
        StringBuilder sb = new StringBuilder();
        TableModel tm = table.getModel();
        for (int i = 0; i < tm.getColumnCount(); i++) {
            sb.append(tm.getColumnName(i)).append(i == tm.getColumnCount() - 1 ? "" : "\t");
        }
        sb.append("\n");
        for (int r = 0; r < tm.getRowCount(); r++) {
            for (int c = 0; c < tm.getColumnCount(); c++) {
                Object val = tm.getValueAt(r, c);
                sb.append(val == null ? "" : val.toString()).append(c == tm.getColumnCount() - 1 ? "" : "\t");
            }
            sb.append("\n");
        }
        StringSelection sel = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
        JOptionPane.showMessageDialog(this, "Tabla copiada al portapapeles.");
    }

    private void shiftDate(int field, int amount) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Calendar c1 = Calendar.getInstance();
            c1.setTime(sdf.parse(txtFrom.getText()));
            c1.add(field, amount);

            if (field == Calendar.MONTH) {
                c1.set(Calendar.DAY_OF_MONTH, 1);
                txtFrom.setText(sdf.format(c1.getTime()));
                c1.set(Calendar.DAY_OF_MONTH, c1.getActualMaximum(Calendar.DAY_OF_MONTH));
                txtTo.setText(sdf.format(c1.getTime()));
            } else {
                txtFrom.setText(sdf.format(c1.getTime()));
                Calendar c2 = Calendar.getInstance();
                c2.setTime(sdf.parse(txtTo.getText()));
                c2.add(field, amount);
                txtTo.setText(sdf.format(c2.getTime()));
            }
        } catch (Exception ex) {
        }
    }

    private JPanel createDatePanel(JTextField txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(txt, BorderLayout.CENTER);
        JButton btn = new JButton("📅");
        btn.setMargin(new Insets(1, 4, 1, 4));
        btn.addActionListener(e -> {
            DatePickerPopup popup = new DatePickerPopup(d -> txt.setText(d));
            popup.show(btn, 0, btn.getHeight());
        });
        p.add(btn, BorderLayout.EAST);
        return p;
    }

    private void generate() {
        String d1 = txtFrom.getText();
        String d2 = txtTo.getText();
        btnGenerate.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Object>, Void>() {
            @Override
            protected List<Object> doInBackground() throws Exception {
                SimpleDateFormat sdfUI = new SimpleDateFormat("dd/MM/yyyy");
                SimpleDateFormat sdfAPI = new SimpleDateFormat("yyyy-MM-dd");
                Date dateStart = sdfUI.parse(d1);
                Date dateEnd = sdfUI.parse(d2);
                String apiD1 = sdfAPI.format(dateStart);
                String apiD2 = sdfAPI.format(dateEnd);

                List<TimeEntry> entries = service.fetchTimeEntries(projectId, apiD1, apiD2);

                List<VersionDTO> allVersions = service.fetchVersionsFull(projectId);
                StringBuilder sb = new StringBuilder();
                sb.append(I18n.format("reports.version.title", d1, d2)).append("\n");
                sb.append("============================================================\n\n");

                Map<String, List<VersionDTO>> weekGroups = new TreeMap<>();
                Calendar cal = Calendar.getInstance();
                cal.setFirstDayOfWeek(Calendar.MONDAY);
                cal.setMinimalDaysInFirstWeek(4);

                for (VersionDTO v : allVersions) {
                    if ("closed".equalsIgnoreCase(v.status)) {
                        Date vDate = null;
                        if (v.dueDate != null && v.dueDate.length() >= 10) {
                            try {
                                vDate = sdfAPI.parse(v.dueDate.substring(0, 10));
                            } catch (Exception ignored) {
                            }
                        }
                        if (vDate != null && !vDate.before(dateStart) && !vDate.after(dateEnd)) {
                            cal.setTime(vDate);
                            int year = cal.get(Calendar.YEAR);
                            int week = cal.get(Calendar.WEEK_OF_YEAR);
                            if (cal.get(Calendar.MONTH) == Calendar.JANUARY && week >= 52) {
                                year--;
                            } else if (cal.get(Calendar.MONTH) == Calendar.DECEMBER && week == 1) {
                                year++;
                            }
                            String key = String.format("%04d - Semana %02d", year, week);
                            weekGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
                        }
                    }
                }

                if (weekGroups.isEmpty()) {
                    sb.append("No se encontraron versiones cerradas en este periodo.\n");
                } else {
                    for (Map.Entry<String, List<VersionDTO>> entry : weekGroups.entrySet()) {
                        sb.append("\n>>> ").append(entry.getKey()).append(" <<<\n");
                        for (VersionDTO v : entry.getValue()) {
                            sb.append(I18n.format("reports.version.item", v.name, v.dueDate)).append("\n");
                            sb.append(" ----------------------------------------------\n");
                            List<Task> tasks = service.fetchTasksByVersion(projectId, v.id);
                            if (tasks.isEmpty()) {
                                sb.append(I18n.get("reports.version.no_tasks")).append("\n");
                            } else {
                                for (Task t : tasks) {
                                    sb.append(String.format("#%-5d [%-10s] %s (%s)\n", t.id,
                                            (t.status != null ? t.status : ""), t.subject,
                                            (t.assignedTo != null ? t.assignedTo : "Sin Asignar")));
                                }
                            }
                            sb.append("\n");
                        }
                    }
                }

                List<Task> closedTasks = service.fetchClosedTasks(projectId, apiD1, apiD2);
                Map<String, Long> categoryCounts = closedTasks.stream()
                        .collect(Collectors.groupingBy(
                                t -> (t.category == null || t.category.isEmpty()) ? I18n.get("reports.category.none")
                                        : t.category,
                                Collectors.counting()));

                List<Object> res = new ArrayList<>();
                res.add(entries);
                res.add(sb.toString());
                res.add(categoryCounts);
                return res;
            }

            @Override
            protected void done() {
                try {
                    List<Object> data = get();
                    List<TimeEntry> entries = (List<TimeEntry>) data.get(0);
                    String reportVer = (String) data.get(1);
                    Map<String, Long> catCounts = (Map<String, Long>) data.get(2);

                    processHoursData(entries);
                    processTasksData(entries);
                    processCategoriesData(catCounts);

                    txtVersionsReport.setText(reportVer);
                    txtVersionsReport.setCaretPosition(0);
                } catch (Exception e) {
                    redmineconnector.util.LoggerUtil.logError("ReportsDialog",
                            "Error generating reports", e);
                    JOptionPane.showMessageDialog(ReportsDialog.this, "Error: " + e.getMessage());
                }
                btnGenerate.setEnabled(true);
                setCursor(Cursor.getDefaultCursor());
            }
        }.execute();
    }

    // Field to store targets per column index
    private final Map<Integer, Double> hourlyTargets = new HashMap<>();
    private final Map<String, Double> targetProfiles = new java.util.TreeMap<>();

    // Load profiles from config or defaults
    {
        Properties props = redmineconnector.config.ConfigManager.loadConfig();
        String saved = props.getProperty("reports.target_profiles", "");
        if (!saved.isEmpty()) {
            for (String part : saved.split(";")) {
                String[] split = part.split("=");
                if (split.length == 2) {
                    try {
                        targetProfiles.put(split[0], Double.parseDouble(split[1]));
                    } catch (Exception e) {
                    }
                }
            }
        }
        // Defaults if empty
        if (targetProfiles.isEmpty()) {
            targetProfiles.put("Estándar (8.5h)", 8.5);
            targetProfiles.put("Jornada Intensiva (7.0h)", 7.0);
        }
    }

    private void processHoursData(List<TimeEntry> entries) {
        Set<String> dates = new TreeSet<>();
        Set<String> users = new TreeSet<>();
        Map<String, Map<String, Double>> pivot = new HashMap<>();

        for (TimeEntry te : entries) {
            dates.add(te.spentOn);
            users.add(te.user);
            pivot.putIfAbsent(te.user, new HashMap<>());
            Map<String, Double> userDates = pivot.get(te.user);
            userDates.put(te.spentOn, userDates.getOrDefault(te.spentOn, 0.0) + te.hours);
        }
        Vector<String> cols = new Vector<>();
        cols.add(I18n.get("reports.col.user"));
        SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfOut = new SimpleDateFormat("EEE dd", new Locale("es", "ES"));
        for (String dStr : dates) {
            try {
                cols.add(sdfOut.format(sdfIn.parse(dStr)));
            } catch (Exception e) {
                cols.add(dStr);
            }
        }
        cols.add(I18n.get("reports.col.total_upper"));

        Vector<Vector<Object>> rows = new Vector<>();
        for (String u : users) {
            Vector<Object> row = new Vector<>();
            row.add(u);
            double total = 0;
            for (String d : dates) {
                Double h = pivot.get(u).getOrDefault(d, 0.0);
                row.add(h > 0 ? String.format("%.2f", h) : "");
                total += h;
            }
            row.add(String.format("%.2f", total));
            rows.add(row);
        }
        modelHours.setDataVector(rows, cols);

        // --- Calculate Active Columns (has any data > 0) ---
        activeColumns.clear();
        // Skip Col 0 (User). Check Date cols.
        for (int c = 1; c < modelHours.getColumnCount() - 1; c++) {
            boolean hasData = false;
            for (int r = 0; r < modelHours.getRowCount(); r++) {
                Object val = modelHours.getValueAt(r, c);
                if (val != null && !val.toString().isEmpty()) {
                    try {
                        if (Double.parseDouble(val.toString().replace(",", ".")) > 0) {
                            hasData = true;
                            break;
                        }
                    } catch (Exception e) {
                    }
                }
            }
            if (hasData)
                activeColumns.add(c);
        }

        // --- Enhancement: Setup Renderers and Targets ---
        hourlyTargets.clear();
        TimeTargetAutomation.loadConfig(); // Refresh automated rules

        // Skip first (User) and last (Total) columns
        // Cols are [User, Date1, Date2, ..., Total]
        // We need to parse Date from column name to apply rules?
        // Problem: Header renderer creates "EEE dd", not YYYY-MM-DD.
        // We have the `dates` set which is strictly sorted strings YYYY-MM-DD.
        // And we iterate `dates` to build `cols`.
        // So column index i maps to date at i-1 in our iteration?
        // Yes: col 0 = User. col 1 = date[0]. col 2 = date[1].

        List<String> dateList = new ArrayList<>(dates);
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 1; i < tableHours.getColumnCount() - 1; i++) {
            double target = 8.5; // Default fallback
            if ((i - 1) < dateList.size()) {
                try {
                    java.time.LocalDate ld = java.time.LocalDate.parse(dateList.get(i - 1), dtf);
                    target = TimeTargetAutomation.getTargetForDate(ld);
                } catch (Exception e) {
                }
            }

            hourlyTargets.put(i, target);
            tableHours.getColumnModel().getColumn(i).setHeaderRenderer(new HoursHeaderRenderer());
            tableHours.getColumnModel().getColumn(i).setCellRenderer(new HoursCellRenderer());
        }

        // setupTableContextMenu() is now efficient and only adds listeners if not
        // present
        setupTableContextMenu();
    }

    private void setupTableContextMenu() {
        // Remove existing listeners of our type to prevent duplicates
        for (java.awt.event.MouseListener ml : tableHours.getMouseListeners()) {
            if (ml instanceof HoursTableMouseListener) {
                tableHours.removeMouseListener(ml);
            }
        }

        // Remove ComponentPopupMenu if set
        tableHours.setComponentPopupMenu(null);

        // Add fresh listener
        tableHours.addMouseListener(new HoursTableMouseListener());
    }

    // Custom listener class to be identifiable for removal
    private class HoursTableMouseListener extends java.awt.event.MouseAdapter {
        private final javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        private final javax.swing.JMenu mObj = new javax.swing.JMenu("Establecer Objetivo para Columna");

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            if (e.isPopupTrigger())
                handleEvent(e);
        }

        @Override
        public void mouseReleased(java.awt.event.MouseEvent e) {
            if (e.isPopupTrigger())
                handleEvent(e);
        }

        private void handleEvent(java.awt.event.MouseEvent e) {
            int col = tableHours.columnAtPoint(e.getPoint());
            if (col > 0 && col < tableHours.getColumnCount() - 1) { // Only date columns
                mObj.removeAll();
                for (Map.Entry<String, Double> entry : targetProfiles.entrySet()) {
                    javax.swing.JMenuItem mi = new javax.swing.JMenuItem(
                            entry.getKey() + " [" + entry.getValue() + "h]");
                    mi.addActionListener(ev -> {
                        hourlyTargets.put(col, entry.getValue());
                        tableHours.getTableHeader().repaint();
                        tableHours.repaint();
                    });
                    mObj.add(mi);
                }
                popup.removeAll();
                popup.add(mObj);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    // --- Inner Classes for Time Report Enhancements ---

    private class HoursHeaderRenderer implements javax.swing.table.TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(javax.swing.UIManager.getBorder("TableHeader.cellBorder"));
            p.setBackground(javax.swing.UIManager.getColor("TableHeader.background"));

            Double target = hourlyTargets.getOrDefault(column, 8.5);

            JLabel lTitle = new JLabel("<html><div style='text-align:center'>" + value
                    + "<br><span style='font-weight:normal;font-size:9px'>(Obj: " + target + "h)</span></div></html>");
            lTitle.setFont(table.getTableHeader().getFont());
            lTitle.setHorizontalAlignment(SwingConstants.CENTER);
            p.add(lTitle, BorderLayout.CENTER);

            return p;
        }
    }

    // Track which columns have any data (to avoid highlighting empty days)
    private final Set<Integer> activeColumns = new HashSet<>();

    // Configurable Colors
    public static Color COLOR_EMPTY = new Color(255, 200, 100);
    public static Color COLOR_DEVIATION = new Color(255, 200, 200);

    public static void loadColors() {
        Properties props = redmineconnector.config.ConfigManager.loadConfig();
        try {
            String cE = props.getProperty("reports.color.empty");
            if (cE != null)
                COLOR_EMPTY = new Color(Integer.parseInt(cE));

            String cD = props.getProperty("reports.color.deviation");
            if (cD != null)
                COLOR_DEVIATION = new Color(Integer.parseInt(cD));
        } catch (Exception e) {
        }
    }

    // Initial load
    static {
        loadColors();
    }

    private class HoursCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected)
                return c; // Standard selection look

            // If this column has NO data across all users, don't paint it (assume
            // holiday/future)
            if (!activeColumns.contains(column)) {
                c.setBackground(Color.WHITE);
                return c;
            }

            try {
                String sVal = (String) value;
                if (sVal == null || sVal.isEmpty()) {
                    // User has NO hours on an active day -> Configured Empty Color
                    c.setBackground(COLOR_EMPTY);
                } else {
                    sVal = sVal.replace(",", ".");
                    double v = Double.parseDouble(sVal);
                    if (v == 0) {
                        // User has 0 hours on an active day -> Configured Empty Color
                        c.setBackground(COLOR_EMPTY);
                    } else {
                        double target = hourlyTargets.getOrDefault(column, 8.5);
                        // Significant deviation -> Configured Deviation Color
                        if (Math.abs(v - target) > 0.01) {
                            c.setBackground(COLOR_DEVIATION);
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                    }
                }
            } catch (Exception e) {
                c.setBackground(Color.WHITE);
            }
            return c;
        }
    }

    private void processTasksData(List<TimeEntry> entries) {
        // Load Settings
        Properties cfg = redmineconnector.config.ConfigManager.loadConfig();
        boolean hideClosed = Boolean.parseBoolean(cfg.getProperty("reports.tasks.hide_closed", "false"));
        boolean fetchMissing = Boolean.parseBoolean(cfg.getProperty("reports.tasks.fetch_missing", "false"));

        Map<Integer, Double> hoursPerTask = new HashMap<>();
        Map<Integer, String> taskSubjects = new HashMap<>();
        Map<Integer, redmineconnector.model.Task> taskDetails = new HashMap<>();

        // Populate specific task details from existing list
        if (currentViewTasks != null) {
            for (Task t : currentViewTasks) {
                taskSubjects.put(t.id, t.subject);
                taskDetails.put(t.id, t);
            }
        }

        // Collect IDs and Hours
        Set<Integer> missingIds = new HashSet<>();
        for (TimeEntry te : entries) {
            hoursPerTask.put(te.issueId, hoursPerTask.getOrDefault(te.issueId, 0.0) + te.hours);
            // Default subject from TimeEntry if available
            if (te.issueSubject != null && !te.issueSubject.isEmpty()) {
                taskSubjects.putIfAbsent(te.issueId, te.issueSubject);
            }
            // Check if we miss details
            if (!taskDetails.containsKey(te.issueId)) {
                missingIds.add(te.issueId);
            }
        }

        // Fetch missing if requested
        if (fetchMissing && !missingIds.isEmpty() && service != null) {
            try {
                // Optimized bulk fetch
                List<redmineconnector.model.Task> fetched = service.fetchTasksByIds(new ArrayList<>(missingIds));
                for (redmineconnector.model.Task t : fetched) {
                    taskSubjects.put(t.id, t.subject);
                    taskDetails.put(t.id, t);
                }
            } catch (Exception e) {
                redmineconnector.util.LoggerUtil.logError("ReportsDialog",
                        "Failed to fetch missing tasks for report", e);
            }
        }

        modelTasks.setRowCount(0);
        for (Map.Entry<Integer, Double> entry : hoursPerTask.entrySet()) {
            int id = entry.getKey();
            double hours = entry.getValue();
            String subject = taskSubjects.get(id);
            redmineconnector.model.Task t = taskDetails.get(id);

            // Filter Closed
            if (hideClosed && t != null) {
                String st = t.status != null ? t.status.toLowerCase() : "";
                // Heuristic for "Closed"
                if (st.equals("cerrada") || st.equals("rechazada") || st.equals("resuelta")
                        || st.equals("finalizada")) {
                    continue;
                }
            }

            if (subject == null || subject.isEmpty()) {
                subject = "Tarea #" + id + " (No visible en listado actual)";
            }
            modelTasks.addRow(new Object[] { id, subject, String.format("%.2f", hours) });
        }
    }

    private void processCategoriesData(Map<String, Long> catCounts) {
        modelCategories.setRowCount(0);
        catCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> modelCategories.addRow(new Object[] { e.getKey(), e.getValue() }));
    }

    /**
     * Crea el panel para el nuevo informe de resumen por estado y asignado.
     * Este informe utiliza las tareas actualmente cargadas en la vista principal
     * (currentViewTasks).
     * 
     * @return JPanel configurado con la tabla de resumen.
     */
    private JPanel createSummaryByStatusAndAssigneeTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel(I18n.get("reports.summary.title"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);

        // Se inicializan los modelos y tablas aquí, ya que son específicos de esta
        // pestaña.
        modelStatusAssignee = new DefaultTableModel();
        tableStatusAssignee = new JTable(modelStatusAssignee);
        tableStatusAssignee.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Permite el scroll horizontal
        panel.add(new JScrollPane(tableStatusAssignee), BorderLayout.CENTER);

        // Generar los datos del informe al crear la pestaña
        processStatusAssigneeSummary();

        return panel;
    }

    /**
     * Procesa los datos de las tareas actuales para generar el resumen por estado y
     * asignado.
     * Los resultados se cargan en `modelStatusAssignee`.
     */
    private void processStatusAssigneeSummary() {
        // Limpiar datos previos
        modelStatusAssignee.setRowCount(0);
        modelStatusAssignee.setColumnCount(0);

        if (currentViewTasks == null || currentViewTasks.isEmpty()) {
            modelStatusAssignee.addColumn(I18n.get("reports.col.info"));
            modelStatusAssignee.addRow(new Object[] { I18n.get("reports.msg.no_tasks") });
            return;
        }

        // Recopilar todos los estados y asignados únicos para las columnas y filas
        Set<String> allStatuses = currentViewTasks.stream()
                .map(t -> t.status != null && !t.status.isEmpty() ? t.status : I18n.get("reports.status.no_status"))
                .collect(Collectors.toCollection(TreeSet::new)); // TreeSet para orden natural

        Set<String> allAssignees = currentViewTasks.stream()
                .map(t -> t.assignedTo != null && !t.assignedTo.isEmpty() ? t.assignedTo
                        : I18n.get("reports.assignee.no_assignee"))
                .collect(Collectors.toCollection(TreeSet::new)); // TreeSet para orden natural

        // Crear las columnas: Estado | Asignado1 | Asignado2 | ... | Total
        Vector<String> columns = new Vector<>();
        columns.add(I18n.get("reports.col.status"));
        allAssignees.forEach(columns::add);
        columns.add(I18n.get("reports.col.total"));
        modelStatusAssignee.setColumnIdentifiers(columns);

        // Preparar la estructura de datos para los conteos: Map<Estado, Map<Asignado,
        // Conteo>>
        Map<String, Map<String, Long>> statusAssigneeCounts = currentViewTasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.status != null && !t.status.isEmpty() ? t.status : "(Sin Estado)",
                        Collectors.groupingBy(
                                t -> t.assignedTo != null && !t.assignedTo.isEmpty() ? t.assignedTo : "(Sin Asignar)",
                                Collectors.counting())));

        // Rellenar las filas de la tabla
        for (String status : allStatuses) {
            Vector<Object> row = new Vector<>();
            row.add(status);
            long statusTotal = 0;

            for (String assignee : allAssignees) {
                long count = statusAssigneeCounts.getOrDefault(status, Collections.emptyMap())
                        .getOrDefault(assignee, 0L);
                row.add(count > 0 ? count : ""); // Mostrar cadena vacía para 0 para una apariencia más limpia
                statusTotal += count;
            }
            row.add(statusTotal); // Total para esta fila de estado
            modelStatusAssignee.addRow(row);
        }

        // Añadir una fila de "Total General"
        Vector<Object> totalRow = new Vector<>();
        totalRow.add(I18n.get("reports.col.grand_total"));
        long grandTotal = 0;
        for (String assignee : allAssignees) {
            long assigneeTotal = currentViewTasks.stream()
                    .filter(t -> (t.assignedTo != null && !t.assignedTo.isEmpty() ? t.assignedTo
                            : I18n.get("reports.assignee.no_assignee"))
                            .equals(assignee))
                    .count();
            totalRow.add(assigneeTotal > 0 ? assigneeTotal : "");
            grandTotal += assigneeTotal;
        }
        totalRow.add(grandTotal);
        modelStatusAssignee.addRow(totalRow);

        // Aplicar un renderizador personalizado para la tabla para resaltar los totales
        tableStatusAssignee.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (row == table.getRowCount() - 1 || column == table.getColumnCount() - 1) { // Última fila o
                                                                                                  // última columna
                        c.setBackground(new Color(220, 230, 240)); // Azul claro para totales
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 248));
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    }
                }
                setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                return c;
            }
        });
    }
}
