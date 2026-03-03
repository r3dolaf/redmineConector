package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import redmineconnector.config.ConfigManager;
import redmineconnector.ui.UIHelper;
import redmineconnector.ui.theme.CustomTheme;
import redmineconnector.ui.theme.Theme;
import redmineconnector.ui.theme.ThemeConfig;
import redmineconnector.ui.theme.ThemeManager;

public class ThemeEditorDialog extends JDialog {
    private CustomTheme editingTheme;
    private final ThemeConfig originalTheme;
    private Runnable saveCallback; // Callback para recargar datos

    public ThemeEditorDialog(Frame owner) {
        super(owner, "Editor de Tema", true);

        ThemeConfig current = ThemeManager.getCurrentTheme();
        originalTheme = current;

        // Initialize editing theme
        // If current is a preset (Enum), convert to CustomTheme for editing
        if (current instanceof Theme) {
            editingTheme = new CustomTheme(current, "Custom from " + current.getDisplayName());
        } else if (current instanceof CustomTheme) {
            // Clone it
            editingTheme = new CustomTheme(current, current.getDisplayName());
        } else {
            // Fallback
            editingTheme = new CustomTheme(Theme.LIGHT, "Custom");
        }

        setupUI();
        setSize(redmineconnector.util.AppConstants.DIALOG_WIDTH_MEDIUM - 100,
                redmineconnector.util.AppConstants.DIALOG_HEIGHT_MEDIUM + 100);
        setLocationRelativeTo(owner);
    }

    /**
     * Establece un callback que se ejecutará al guardar el tema.
     * El callback debe recargar la conexión actual para aplicar los cambios.
     */
    public void onSave(Runnable callback) {
        this.saveCallback = callback;
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        JPanel pnlColors = new JPanel(new GridBagLayout());
        // Add scroll pane
        JScrollPane scroll = new JScrollPane(pnlColors);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scroll, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        addColorRow(pnlColors, gbc, "Background", editingTheme.getBackground(), c -> editingTheme.background = c);
        addColorRow(pnlColors, gbc, "Panel Background", editingTheme.getPanelBackground(),
                c -> editingTheme.panelBackground = c);
        addColorRow(pnlColors, gbc, "Text", editingTheme.getText(), c -> editingTheme.text = c);
        addColorRow(pnlColors, gbc, "Text Secondary", editingTheme.getTextSecondary(),
                c -> editingTheme.textSecondary = c);
        addColorRow(pnlColors, gbc, "Border", editingTheme.getBorder(), c -> editingTheme.border = c);
        addColorRow(pnlColors, gbc, "Table Header", editingTheme.getTableHeader(), c -> editingTheme.tableHeader = c);
        addColorRow(pnlColors, gbc, "Table Row", editingTheme.getTableRow(), c -> editingTheme.tableRow = c);
        addColorRow(pnlColors, gbc, "Table Row Alt", editingTheme.getTableRowAlt(), c -> editingTheme.tableRowAlt = c);
        addColorRow(pnlColors, gbc, "Accent", editingTheme.getAccent(), c -> editingTheme.accent = c);
        addColorRow(pnlColors, gbc, "Accent Light", editingTheme.getAccentLight(), c -> editingTheme.accentLight = c);
        addColorRow(pnlColors, gbc, "Accent Dark", editingTheme.getAccentDark(), c -> editingTheme.accentDark = c);
        addColorRow(pnlColors, gbc, "Button Background", editingTheme.getButtonBackground(),
                c -> editingTheme.buttonBackground = c);
        addColorRow(pnlColors, gbc, "Button Foreground", editingTheme.getButtonForeground(),
                c -> editingTheme.buttonForeground = c);

        // Buttons
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnReset = new JButton("Reset to Light");
        btnReset.addActionListener(e -> {
            editingTheme = new CustomTheme(Theme.LIGHT, "Custom");
            ThemeManager.setTheme(editingTheme); // Live preview
            // También ejecutar callback para refrescar
            if (saveCallback != null) {
                saveCallback.run();
            }
            getContentPane().removeAll();
            setupUI();
            revalidate();
            repaint();
        });

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> {
            ThemeManager.setTheme(originalTheme); // Revert
            // Recargar con tema original
            if (saveCallback != null) {
                saveCallback.run();
            }
            dispose();
        });

        JButton btnSave = new JButton("Guardar");
        btnSave.addActionListener(e -> {
            saveAndClose();
        });

        pnlButtons.add(btnReset);
        pnlButtons.add(btnCancel);
        pnlButtons.add(btnSave);
        add(pnlButtons, BorderLayout.SOUTH);

        UIHelper.addEscapeListener(this);
    }

    private void addColorRow(JPanel panel, GridBagConstraints gbc, String label, Color color,
            java.util.function.Consumer<Color> setter) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel(label), gbc);

        JPanel pnlPreview = new JPanel();
        pnlPreview.setBackground(color);
        pnlPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        pnlPreview.setPreferredSize(new java.awt.Dimension(50, 20));

        JButton btnEdit = new JButton("Edit");
        btnEdit.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Select " + label, color);
            if (newColor != null) {
                setter.accept(newColor);
                pnlPreview.setBackground(newColor);
                ThemeManager.setTheme(editingTheme); // Live preview
                // También ejecutar callback para refrescar vista
                if (saveCallback != null) {
                    saveCallback.run();
                }
            }
        });

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        panel.add(pnlPreview, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        panel.add(btnEdit, gbc);

        gbc.gridy++;
    }

    private void saveAndClose() {
        // Save to config
        ThemeManager.setTheme(editingTheme);
        Properties props = ConfigManager.loadConfig();
        ConfigManager.saveTheme(props, editingTheme);
        ConfigManager.saveConfig(props);

        // Ejecutar callback para recargar conexión actual
        if (saveCallback != null) {
            saveCallback.run();
        }

        dispose();
    }
}
