package redmineconnector.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import redmineconnector.config.StyleConfig;
import redmineconnector.ui.dialogs.DatePickerPopup;

public class FiltersPanel extends JPanel {

    public JButton btnRefresh = new JButton("↻");
    public JButton btnCreate = new JButton("➕");
    public JButton btnKeywords = new JButton("🔍");
    public JButton btnClearFilters = new JButton("❌");

    public JTextField txtSearch = new JTextField(12);
    public JTextField txtIdSearch = new JTextField(5);
    public JTextField txtExclude = new JTextField(8);
    public JTextField txtDateFrom = new JTextField(9);
    public JTextField txtDateTo = new JTextField(9);
    public JButton btnDateFrom = new JButton("📅");
    public JButton btnDateTo = new JButton("📅");

    public FilterMultiSelect msTracker;
    public FilterMultiSelect msStatus;
    public FilterMultiSelect msCategory;
    public FilterMultiSelect msAssigned;

    public JLabel lblOffline = new JLabel("MODO OFFLINE (Solo Lectura)");

    private final Runnable onUpdateFilters;
    private boolean isFiltersVisible = true; // Added state variable

    private final Runnable onSyncTracker; // Logic for syncing status with tracker
    private final StyleConfig styleConfig;

    // Panel References
    private JPanel actionsPanel;
    private JPanel filtersFormPanel;

    public FiltersPanel(Runnable onUpdateFilters, Runnable onSyncTracker, StyleConfig styleConfig) {
        this.onUpdateFilters = onUpdateFilters;
        this.onSyncTracker = onSyncTracker;
        this.styleConfig = styleConfig;

        setLayout(new BorderLayout());
        createUI();
        applyStyles();
    }

    public void applyStyles() {
        if (actionsPanel != null) {
            actionsPanel.setBackground(styleConfig.bgHeader);
            actionsPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, styleConfig.border));
        }

        btnRefresh.setBackground(styleConfig.bgMain);
        btnRefresh.setForeground(styleConfig.textPrimary);
        btnCreate.setBackground(styleConfig.actionSuccess);
        btnCreate.setForeground(styleConfig.textInverted);
        btnKeywords.setBackground(styleConfig.bgMain);
        btnKeywords.setForeground(styleConfig.textPrimary);

        btnClearFilters.setBackground(styleConfig.bgMain); // Optional
        // btnClearFilters.setForeground(styleConfig.actionDanger);

        if (filtersFormPanel != null) {
            filtersFormPanel.setBackground(styleConfig.bgPanel);
        }

        // Recursive update for simple components if needed, or specific updates
        updateDateFieldsStyles();

        if (lblOffline.isVisible()) {
            lblOffline.setBackground(styleConfig.actionDanger);
            lblOffline.setForeground(styleConfig.textInverted);
        }
    }

    private void updateDateFieldsStyles() {
        txtDateFrom.setBackground(styleConfig.bgInput);
        txtDateTo.setBackground(styleConfig.bgInput);
        txtIdSearch.setBackground(styleConfig.bgInput);
        txtSearch.setBackground(styleConfig.bgInput);
        txtExclude.setBackground(styleConfig.bgInput);

        txtDateFrom.setForeground(
                txtDateFrom.getText().equals("dd/MM/yyyy") ? styleConfig.textSecondary : styleConfig.textPrimary);
        txtDateTo.setForeground(
                txtDateTo.getText().equals("dd/MM/yyyy") ? styleConfig.textSecondary : styleConfig.textPrimary);
        txtIdSearch.setForeground(styleConfig.textPrimary);
        txtSearch.setForeground(styleConfig.textPrimary);
        txtExclude.setForeground(styleConfig.textPrimary);

        txtIdSearch.setCaretColor(styleConfig.textPrimary);
        txtSearch.setCaretColor(styleConfig.textPrimary);
    }

    private void createUI() {
        lblOffline.setOpaque(true);
        lblOffline.setBackground(styleConfig.actionDanger);
        lblOffline.setForeground(styleConfig.textInverted);
        lblOffline.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblOffline.setHorizontalAlignment(SwingConstants.CENTER);
        lblOffline.setVisible(false);
        add(lblOffline, BorderLayout.SOUTH);

        actionsPanel = new JPanel(new BorderLayout());
        // actions.setBackground(styleConfig.bgHeader); // Moved to applyStyles

        // Left Actions
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.setOpaque(false);

        btnRefresh.setToolTipText("Refrescar (Ctrl+R o F5)");
        btnCreate.setToolTipText("Nueva Tarea (Ctrl+N)");
        btnKeywords.setToolTipText("Buscador Keywords");
        btnClearFilters.setToolTipText("Limpiar filtros");

        // Colors moved to applyStyles
        // btnRefresh.setBackground(styleConfig.bgMain);
        // btnCreate.setBackground(styleConfig.actionSuccess);
        // btnKeywords.setBackground(styleConfig.bgMain);

        btnRefresh.setFocusPainted(false);
        btnCreate.setFocusPainted(false);
        btnKeywords.setFocusPainted(false);
        btnClearFilters.setFocusPainted(false);

        left.add(btnRefresh);
        left.add(Box.createHorizontalStrut(5));
        left.add(btnCreate);

        // Right Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);

        right.add(Box.createHorizontalStrut(5));
        right.add(btnKeywords);
        btnClearFilters.setToolTipText("Limpiar filtros");
        right.add(btnClearFilters);
        right.add(Box.createHorizontalStrut(10));
        right.add(new JSeparator(SwingConstants.VERTICAL));
        right.add(Box.createHorizontalStrut(10));

        // Quick Filters buttons
        setupQuickFilters(right);

        // Filters Form
        setupMultiSelectors();

        filtersFormPanel = new JPanel(new GridBagLayout());
        // filters.setBackground(styleConfig.bgPanel); // Moved to applyStyles
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 5, 2, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        setupDateFields();

        // Row 0: Dates
        g.gridy = 0;
        g.gridx = 0;
        filtersFormPanel.add(new JLabel("Fecha:"), g);
        g.gridx = 1;
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        datePanel.setOpaque(false);
        datePanel.add(txtDateFrom);
        datePanel.add(btnDateFrom);
        datePanel.add(new JLabel(" - "));
        datePanel.add(txtDateTo);
        datePanel.add(btnDateTo);
        filtersFormPanel.add(datePanel, g);

        g.gridx = 2;
        g.gridwidth = 2;
        JPanel excludeP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        excludeP.setOpaque(false);
        excludeP.add(new JLabel(" No contiene: "));
        txtExclude.setToolTipText("Separa bloques con coma (ej: test, bug)");
        txtExclude.setPreferredSize(new Dimension(120, 24));
        excludeP.add(txtExclude);
        filtersFormPanel.add(excludeP, g);
        g.gridwidth = 1;

        // Row 1: ID, Subject
        g.gridy = 1;
        g.gridx = 0;
        filtersFormPanel.add(new JLabel("ID:"), g);
        g.gridx = 1;
        filtersFormPanel.add(txtIdSearch, g);
        g.gridx = 2;
        filtersFormPanel.add(new JLabel("Asunto:"), g);
        g.gridx = 3;
        g.weightx = 1.0;
        g.gridwidth = 3;
        JPanel searchP = new JPanel(new BorderLayout());
        searchP.add(txtSearch, BorderLayout.CENTER);
        filtersFormPanel.add(searchP, g);
        g.weightx = 0.0;
        g.gridwidth = 1;

        // Row 2: Type, Status, Prio
        g.gridy = 2;
        g.gridx = 0;
        filtersFormPanel.add(new JLabel("Tipo:"), g);
        g.gridx = 1;
        g.weightx = 0.5;
        filtersFormPanel.add(msTracker, g);
        g.gridx = 2;
        g.weightx = 0;
        filtersFormPanel.add(new JLabel("Estado:"), g);
        g.gridx = 3;
        g.gridwidth = 3;
        g.weightx = 0.5;
        filtersFormPanel.add(msStatus, g);
        g.gridwidth = 1;
        g.weightx = 0;

        // Row 3: Cat, Assigned
        g.gridy = 3;
        g.gridx = 0;
        g.weightx = 0;
        filtersFormPanel.add(new JLabel("Cat:"), g);
        g.gridx = 1;
        g.weightx = 0.5;
        filtersFormPanel.add(msCategory, g);
        g.gridx = 2;
        g.weightx = 0;
        filtersFormPanel.add(new JLabel("Asig:"), g);
        g.gridx = 3;
        g.gridwidth = 3;
        g.weightx = 0.5;
        filtersFormPanel.add(msAssigned, g);
        g.gridwidth = 1;

        JButton btnToggle = new JButton("▼");
        btnToggle.setToolTipText("Mostrar Filtros");
        btnToggle.setPreferredSize(new Dimension(24, 24));
        btnToggle.setMargin(new Insets(0, 0, 0, 0));
        btnToggle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnToggle.setFocusable(false);
        right.add(btnToggle);

        actionsPanel.add(left, BorderLayout.WEST);
        actionsPanel.add(right, BorderLayout.EAST);
        add(actionsPanel, BorderLayout.NORTH);
        add(filtersFormPanel, BorderLayout.CENTER);

        btnToggle.addActionListener(e -> {
            isFiltersVisible = !isFiltersVisible;
            filtersFormPanel.setVisible(isFiltersVisible);
            btnToggle.setText(isFiltersVisible ? "▼" : "▲");
            btnToggle.setToolTipText(isFiltersVisible ? "Ocultar Filtros" : "Mostrar Filtros");
            revalidate();
        });

        setupListeners();

        btnClearFilters.addActionListener(e -> clearFilters());
    }

    private void setupQuickFilters(JPanel right) {
        JButton btnUnassigned = new JButton("⚪ Sin Asignar");
        btnUnassigned.setToolTipText("Tareas sin asignar");
        btnUnassigned.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnUnassigned.addActionListener(e -> {
            msAssigned.clearSelection();
            msAssigned.getSelectedNames().add("⚪ Sin Asignar");
            msAssigned.updateLabel();
            onUpdateFilters.run();
        });

        JButton btnToday = new JButton("📅 Hoy");
        btnToday.setToolTipText("Tareas con vencimiento hoy");
        btnToday.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnToday.addActionListener(e -> {
            String today = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            txtDateFrom.setText(today);
            txtDateFrom.setForeground(styleConfig.textPrimary);
            txtDateTo.setText(today);
            txtDateTo.setForeground(styleConfig.textPrimary);
            onUpdateFilters.run();
        });

        right.add(btnUnassigned);
        right.add(btnToday);

        right.add(Box.createHorizontalStrut(10));
        right.add(new JSeparator(SwingConstants.VERTICAL));
        right.add(Box.createHorizontalStrut(10));

    }

    private void setupMultiSelectors() {
        msTracker = new FilterMultiSelect("Tipo", () -> {
            onSyncTracker.run();
            onUpdateFilters.run();
        });
        msStatus = new FilterMultiSelect("Estado", onUpdateFilters);
        msCategory = new FilterMultiSelect("Cat", onUpdateFilters);
        msAssigned = new FilterMultiSelect("Asig", onUpdateFilters);
    }

    private void setupDateFields() {
        setPlaceholder(txtDateFrom, "dd/MM/yyyy");
        setPlaceholder(txtDateTo, "dd/MM/yyyy");
        btnDateFrom.addActionListener(e -> showDatePicker(txtDateFrom, btnDateFrom));
        btnDateTo.addActionListener(e -> showDatePicker(txtDateTo, btnDateTo));

        btnDateFrom.setPreferredSize(new Dimension(25, 20));
        btnDateTo.setPreferredSize(new Dimension(25, 20));
        btnDateFrom.setMargin(new Insets(0, 0, 0, 0));
        btnDateTo.setMargin(new Insets(0, 0, 0, 0));

        DocumentListener dlDate = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }

            public void removeUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }

            public void changedUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }
        };
        txtDateFrom.getDocument().addDocumentListener(dlDate);
        txtDateTo.getDocument().addDocumentListener(dlDate);
    }

    private void setupListeners() {
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }

            public void removeUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }

            public void changedUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        txtIdSearch.getDocument().addDocumentListener(dl);
        txtExclude.getDocument().addDocumentListener(dl);
    }

    private void showDatePicker(JTextField txt, JButton btn) {
        DatePickerPopup dp = new DatePickerPopup(dateStr -> {
            if (dateStr != null) {
                txt.setText(dateStr);
                txt.setForeground(styleConfig.textPrimary);
                onUpdateFilters.run();
            }
        });
        dp.show(btn, 0, btn.getHeight());
    }

    private void setPlaceholder(JTextField txt, String ph) {
        txt.setForeground(styleConfig.textSecondary);
        txt.setText(ph);
        txt.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (txt.getText().equals(ph)) {
                    txt.setText("");
                    txt.setForeground(styleConfig.textPrimary);
                }
            }

            public void focusLost(FocusEvent e) {
                if (txt.getText().isEmpty()) {
                    txt.setForeground(styleConfig.textSecondary);
                    txt.setText(ph);
                }
            }
        });
        txt.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }

            public void removeUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }

            public void changedUpdate(DocumentEvent e) {
                onUpdateFilters.run();
            }
        });
    }

    public void setOfflineMode(boolean offline) {
        lblOffline.setVisible(offline);
        btnCreate.setEnabled(!offline);
        // Buttons in other panels (Bulk, etc.) are managed elsewhere if not in
        // FiltersPanel
        if (offline) {
            btnRefresh.setText("↻ Reconectar");
            btnRefresh.setBackground(styleConfig.actionDanger); // Bold feedback for offline
        } else {
            btnRefresh.setText("↻");
            btnRefresh.setBackground(styleConfig.bgMain);
        }
    }

    public void addLeftAction(javax.swing.JComponent component) {
        if (actionsPanel != null) {
            // Left is the first component in actionsPanel
            JPanel left = (JPanel) actionsPanel.getComponent(0);
            left.add(component);
        }
    }

    public void clearFilters() {
        txtSearch.setText("");
        txtIdSearch.setText("");
        txtExclude.setText("");
        txtDateFrom.setForeground(styleConfig.textSecondary);
        txtDateFrom.setText("dd/MM/yyyy");
        txtDateTo.setForeground(styleConfig.textSecondary);
        txtDateTo.setText("dd/MM/yyyy");

        msTracker.clearSelection();
        msStatus.clearSelection();
        msCategory.clearSelection();
        msAssigned.clearSelection();
    }

    // Getters for MultiSelectors
    public FilterMultiSelect getMsTracker() {
        return msTracker;
    }

    public FilterMultiSelect getMsStatus() {
        return msStatus;
    }

    public FilterMultiSelect getMsCategory() {
        return msCategory;
    }

    public FilterMultiSelect getMsAssigned() {
        return msAssigned;
    }

    // Getters for text fields
    public JTextField getTxtSearch() {
        return txtSearch;
    }

    public JTextField getTxtIdSearch() {
        return txtIdSearch;
    }

    public JTextField getTxtExclude() {
        return txtExclude;
    }

    public JTextField getTxtDateFrom() {
        return txtDateFrom;
    }

    public JTextField getTxtDateTo() {
        return txtDateTo;
    }

    // Getters for Buttons
    public JButton getBtnClearFilters() {
        return btnClearFilters;
    }

    public JButton getBtnRefresh() {
        return btnRefresh;
    }

    public JButton getBtnCreate() {
        return btnCreate;
    }

    public JButton getBtnKeywords() {
        return btnKeywords;
    }

    public JButton getBtnDateFrom() {
        return btnDateFrom;
    }

    public JButton getBtnDateTo() {
        return btnDateTo;
    }
}
