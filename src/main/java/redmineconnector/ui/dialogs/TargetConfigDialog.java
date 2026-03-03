package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import redmineconnector.config.ConfigManager;
import redmineconnector.ui.UIHelper;

public class TargetConfigDialog extends JDialog {

    // Tab 2: Automation
    private final JComboBox<String>[] weekdayCombos;
    private final JTable tableRanges;
    private final DefaultTableModel modelRanges;

    // Tab 3: Tasks
    private final JCheckBox chkHideClosed;
    private final JCheckBox chkFetchMissing;

    @SuppressWarnings("unchecked")
    public TargetConfigDialog(JDialog owner, Map<String, Double> currentProfiles) {
        super(owner, "Configurar Objetivos y Automatización", ModalityType.APPLICATION_MODAL);

        Properties localConfig = ConfigManager.loadConfig();
        // this.profiles = currentProfiles; // Removed as profiles tab is gone
        // this.config = ConfigManager.loadConfig(); // Removed as profiles tab is gone

        // Initialize Components
        setSize(redmineconnector.util.AppConstants.DIALOG_WIDTH_LARGE + 100,
                redmineconnector.util.AppConstants.DIALOG_HEIGHT_MEDIUM + 100);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        // UIHelper.addEscapeListener(this); // Removed as per user's snippet

        // --- Config Panel (formerly Automation Tab) ---
        // User requested removing Profiles tab and renaming Automation -> "Informe de
        // Horas"

        JTabbedPane mainTabs = new JTabbedPane();
        JPanel pReportConfig = new JPanel(new BorderLayout());

        // --- 1. Weekday & Ranges (Existing) ---

        // Weekdays Panel
        JPanel pWeek = new JPanel(new GridLayout(4, 4, 10, 10));
        pWeek.setBorder(BorderFactory.createTitledBorder("Reglas Semanales"));
        weekdayCombos = new JComboBox[7];
        DayOfWeek[] days = { DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY };

        for (int i = 0; i < 7; i++) {
            pWeek.add(new JLabel(days[i].name()));
            weekdayCombos[i] = new JComboBox<>(new String[] { "-", "7.0", "8.0", "8.5", "9.0" });
            weekdayCombos[i].setEditable(true);

            Double val = TimeTargetAutomation.getWeekdayRule(days[i]);
            if (val != null)
                weekdayCombos[i].setSelectedItem(String.valueOf(val));
            else
                weekdayCombos[i].setSelectedItem("-");

            pWeek.add(weekdayCombos[i]);
        }
        pWeek.add(new JLabel(""));
        pWeek.add(new JLabel(""));

        // Range Panel
        JPanel pRanges = new JPanel(new BorderLayout());
        pRanges.setBorder(BorderFactory.createTitledBorder("Rangos de Fechas"));
        modelRanges = new DefaultTableModel(new String[] { "Inicio", "Fin", "Horas", "¿Prioritaria?" }, 0) {
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 3 ? Boolean.class : String.class;
            }
        };
        tableRanges = new JTable(modelRanges);
        loadRanges();

        JPanel pRangeBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddR = new JButton("Añadir Rango");
        btnAddR.addActionListener(e -> addRange());
        JButton btnDelR = new JButton("Borrar Rango");
        btnDelR.addActionListener(e -> deleteRange());
        pRangeBtns.add(btnAddR);
        pRangeBtns.add(btnDelR);

        pRanges.add(new JScrollPane(tableRanges), BorderLayout.CENTER);
        pRanges.add(pRangeBtns, BorderLayout.SOUTH);

        // --- 2. Color Configuration (New) ---
        JPanel pColors = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        pColors.setBorder(BorderFactory.createTitledBorder("Configuración de Colores"));

        JButton btnColEmpty = new JButton("Color: Falta de Horas / 0h");
        btnColEmpty.setBackground(ReportsDialog.COLOR_EMPTY);
        btnColEmpty.addActionListener(e -> {
            Color c = javax.swing.JColorChooser.showDialog(this, "Seleccionar Color", btnColEmpty.getBackground());
            if (c != null)
                btnColEmpty.setBackground(c);
        });

        JButton btnColDev = new JButton("Color: Desviación de Objetivo");
        btnColDev.setBackground(ReportsDialog.COLOR_DEVIATION);
        btnColDev.addActionListener(e -> {
            Color c = javax.swing.JColorChooser.showDialog(this, "Seleccionar Color", btnColDev.getBackground());
            if (c != null)
                btnColDev.setBackground(c);
        });

        pColors.add(btnColEmpty);
        pColors.add(btnColDev);

        // Assemble Main Panel
        JPanel pTop = new JPanel(new BorderLayout());
        pTop.add(pWeek, BorderLayout.CENTER);
        pTop.add(pColors, BorderLayout.SOUTH);

        pReportConfig.add(pTop, BorderLayout.NORTH);
        pReportConfig.add(pRanges, BorderLayout.CENTER);

        pReportConfig.add(pRanges, BorderLayout.CENTER);

        mainTabs.addTab("Informe de Horas", pReportConfig);

        // --- 3. Tasks Report Config (New) ---
        JPanel pTasksConfig = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));

        chkHideClosed = new JCheckBox("Ocultar tareas cerradas");
        chkFetchMissing = new JCheckBox("Buscar tareas no visibles (Más lento)");

        // Load settings
        chkHideClosed.setSelected(Boolean.parseBoolean(localConfig.getProperty("reports.tasks.hide_closed", "false")));
        chkFetchMissing
                .setSelected(Boolean.parseBoolean(localConfig.getProperty("reports.tasks.fetch_missing", "false")));

        pTasksConfig.add(chkHideClosed);
        pTasksConfig.add(chkFetchMissing);

        mainTabs.addTab("Informe de Tareas", pTasksConfig);

        add(mainTabs, BorderLayout.CENTER);

        // Main Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Guardar Todo");
        btnSave.addActionListener(e -> {
            saveAndClose(btnColEmpty.getBackground(), btnColDev.getBackground());
        });
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        buttons.add(btnSave);
        buttons.add(btnCancel);

        add(buttons, BorderLayout.SOUTH);
    }

    // Removed loadProfiles()

    private void loadRanges() {
        modelRanges.setRowCount(0);
        Map<String, String[]> map = TimeTargetAutomation.getRangesDetails();
        map.forEach((range, details) -> {
            String[] split = range.split(" -> ");
            if (split.length == 2 && details.length >= 2) {
                boolean prio = Boolean.parseBoolean(details[1]);
                modelRanges.addRow(new Object[] { split[0], split[1], details[0], prio });
            }
        });
    }

    // Removed addProfile()
    // Removed deleteProfile()

    private void addRange() {
        JPanel p = new JPanel(new GridLayout(4, 2, 5, 5)); // Added gap for better look
        JTextField txtStart = new JTextField();
        JTextField txtEnd = new JTextField();
        JTextField txtVal = new JTextField("7.0");
        javax.swing.JCheckBox chkPrio = new javax.swing.JCheckBox("Alta Prioridad (Ignora Días)");

        p.add(new JLabel("Inicio (YYYY-MM-DD):"));
        p.add(createDatePanel(txtStart));
        p.add(new JLabel("Fin (YYYY-MM-DD):"));
        p.add(createDatePanel(txtEnd));
        p.add(new JLabel("Horas:"));
        p.add(txtVal);
        p.add(new JLabel("Opciones:"));
        p.add(chkPrio);

        int res = JOptionPane.showConfirmDialog(this, p, "Añadir Rango", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                // Validate format
                LocalDate.parse(txtStart.getText());
                LocalDate.parse(txtEnd.getText());
                Double.parseDouble(txtVal.getText());

                modelRanges.addRow(
                        new Object[] { txtStart.getText(), txtEnd.getText(), txtVal.getText(), chkPrio.isSelected() });
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Datos inválidos: " + e.getMessage());
            }
        }
    }

    private JPanel createDatePanel(JTextField txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(txt, BorderLayout.CENTER);
        JButton btn = new JButton("📅");
        btn.setMargin(new java.awt.Insets(1, 4, 1, 4));
        btn.addActionListener(e -> {
            DatePickerPopup popup = new DatePickerPopup(d -> {
                // Convert picker format (dd/MM/yyyy) to required format (yyyy-MM-dd)
                try {
                    java.time.format.DateTimeFormatter dtfIn = java.time.format.DateTimeFormatter
                            .ofPattern("dd/MM/yyyy");
                    java.time.format.DateTimeFormatter dtfOut = java.time.format.DateTimeFormatter
                            .ofPattern("yyyy-MM-dd");
                    LocalDate date = LocalDate.parse(d, dtfIn);
                    txt.setText(date.format(dtfOut));
                } catch (Exception ex) {
                    txt.setText(d);
                }
            });
            popup.show(btn, 0, btn.getHeight());
        });
        p.add(btn, BorderLayout.EAST);
        return p;
    }

    private void deleteRange() {
        int row = tableRanges.getSelectedRow();
        if (row != -1) {
            modelRanges.removeRow(row);
        }
    }

    private void saveAndClose(Color cEmpty, Color cDev) {
        // Save Profiles (Removed)
        // profiles.clear();
        // StringBuilder sb = new StringBuilder();

        // for (int i = 0; i < modelProfiles.getRowCount(); i++) {
        // String name = (String) modelProfiles.getValueAt(i, 0);
        // Double hours = (Double) modelProfiles.getValueAt(i, 1);
        // profiles.put(name, hours);

        // if (sb.length() > 0)
        // sb.append(";");
        // sb.append(name).append("=").append(hours);
        // }
        // config.setProperty("reports.target_profiles", sb.toString());
        // ConfigManager.saveConfig(config);

        // Save Colors
        Properties props = ConfigManager.loadConfig();
        props.setProperty("reports.color.empty", String.valueOf(cEmpty.getRGB()));
        props.setProperty("reports.color.deviation", String.valueOf(cDev.getRGB()));

        // Save Tasks Config
        props.setProperty("reports.tasks.hide_closed", String.valueOf(chkHideClosed.isSelected()));
        props.setProperty("reports.tasks.fetch_missing", String.valueOf(chkFetchMissing.isSelected()));

        // Save Automation
        DayOfWeek[] days = { DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY };
        for (int i = 0; i < 7; i++) {
            String val = (String) weekdayCombos[i].getSelectedItem();
            if (val == null || val.equals("-") || val.trim().isEmpty()) {
                TimeTargetAutomation.setWeekdayRule(days[i], null);
            } else {
                try {
                    TimeTargetAutomation.setWeekdayRule(days[i], Double.parseDouble(val.replace(",", ".")));
                } catch (Exception e) {
                }
            }
        }

        TimeTargetAutomation.clearRanges();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < modelRanges.getRowCount(); i++) {
            try {
                LocalDate s = LocalDate.parse((String) modelRanges.getValueAt(i, 0), dtf);
                LocalDate e = LocalDate.parse((String) modelRanges.getValueAt(i, 1), dtf);
                double v = Double.parseDouble((String) modelRanges.getValueAt(i, 2));
                boolean p = (Boolean) modelRanges.getValueAt(i, 3);
                TimeTargetAutomation.addRangeRule(s, e, v, p);
            } catch (Exception ex) {
            }
        }

        // Save Everything
        // Correct order:
        // 1. Save colors to disk.
        ConfigManager.saveConfig(props);
        // 2. TimeTargetAutomation.saveConfig() will load disk (including colors),
        // update automation, and save back.
        TimeTargetAutomation.saveConfig();

        // Reload colors in ReportsDialog
        ReportsDialog.loadColors();

        dispose();
    }
}
