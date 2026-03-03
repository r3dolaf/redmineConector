package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import redmineconnector.config.ConfigManager;
import redmineconnector.config.StyleConfig;
import redmineconnector.model.SimpleEntity;
import redmineconnector.ui.UIHelper;
import redmineconnector.util.I18n;

public class StatusColorDialog extends JDialog {
    private final Properties props;
    private final String prefix;
    private final Map<String, JPanel> colorButtons = new HashMap<>(); // Status Colors
    private final Map<String, PriorityInputGroup> priorityInputs = new HashMap<>(); // Priority Inputs (Regex, Color, Bold)
    private final List<SimpleEntity> availableStatuses;
    private Runnable saveCallback;
    
    // Priorities to configure (Hardcoded keys for now, as they map to business logic concepts)
    private static final String[] PRIO_KEYS = {"Immediate", "Urgent", "High", "Normal", "Low"};

    private static class PriorityInputGroup {
        JTextField txtRegex;
        JPanel pnlColor;
        JCheckBox chkBold;
    }

    public StatusColorDialog(Frame owner, String prefix, String title, List<SimpleEntity> statuses) {
        super(owner, I18n.format("status.color.title", title), false);
        this.prefix = prefix;
        this.props = ConfigManager.loadConfig();
        this.availableStatuses = (statuses != null && !statuses.isEmpty()) ? statuses : getDefaultStatuses();
        setupUI();
        loadValues();
        UIHelper.addEscapeListener(this);
        setSize(550, 500); // Increased size for tabs
        setLocationRelativeTo(owner);
    }

    public void onSave(Runnable r) {
        this.saveCallback = r;
    }

    private List<SimpleEntity> getDefaultStatuses() {
        List<SimpleEntity> defs = new ArrayList<>();
        defs.add(new SimpleEntity(1, I18n.get("status.default.new")));
        defs.add(new SimpleEntity(2, I18n.get("status.default.in_progress")));
        defs.add(new SimpleEntity(3, I18n.get("status.default.resolved")));
        defs.add(new SimpleEntity(4, I18n.get("status.default.closed")));
        return defs;
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(I18n.get("status.color.tab.statuses", "Estados"), createColorPanel());
        tabs.addTab(I18n.get("status.color.tab.priorities", "Prioridades"), createPriorityPanel());
        
        add(tabs, BorderLayout.CENTER);
        
        JButton bSave = new JButton(I18n.get("status.color.btn.save"));
        bSave.addActionListener(e -> {
            save();
            if (saveCallback != null)
                saveCallback.run();
            dispose();
        });
        JPanel bp = new JPanel();
        bp.add(bSave);
        add(bp, BorderLayout.SOUTH);
    }

    private JScrollPane createColorPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        p.setBackground(Color.WHITE);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;

        // Boton Reset con estilo mejorado
        JButton btnReset = new JButton(I18n.get("status.color.btn.reset"));
        btnReset.setPreferredSize(new Dimension(450, 32));
        btnReset.setFocusPainted(false);
        btnReset.addActionListener(e -> {
            // Resetear TODOS los colores a BLANCO por defecto
            colorButtons.forEach((name, btn) -> {
                btn.setBackground(Color.WHITE);
            });
        });

        g.gridwidth = 3;
        g.weightx = 1.0;
        p.add(btnReset, g);
        g.gridwidth = 1;
        g.gridy++;

        // Label intro
        JLabel lblIntro = new JLabel(I18n.get("status.color.label.intro"));
        lblIntro.setBorder(new EmptyBorder(10, 0, 10, 0));
        g.gridx = 0;
        g.gridwidth = 3;
        p.add(lblIntro, g);
        g.gridwidth = 1;
        g.gridy++;

        // Crear filas de colores estilo ThemeEditorDialog
        for (SimpleEntity status : availableStatuses) {
            g.gridx = 0;
            g.weightx = 0.0;

            // Label del estado
            JLabel lblStatus = new JLabel(status.name);
            lblStatus.setPreferredSize(new Dimension(150, 24));
            p.add(lblStatus, g);

            g.gridx = 1;
            g.weightx = 0.0;

            // Preview del color (más grande, como en ThemeEditorDialog)
            JPanel colorPreview = new JPanel();
            colorPreview.setPreferredSize(new Dimension(60, 28));
            colorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            colorPreview.setOpaque(true);
            colorPreview.setBackground(Color.WHITE); // Default BLANCO

            String key = status.name.toLowerCase();
            p.add(colorPreview, g);

            g.gridx = 2;
            g.weightx = 1.0;

            // Boton Edit (estilo ThemeEditorDialog)
            JButton btnEdit = new JButton("Edit");
            // btnEdit.setPreferredSize(new Dimension(280, 28)); // Flexible width
            btnEdit.setFocusPainted(false);
            btnEdit.addActionListener(e -> {
                Color c = JColorChooser.showDialog(
                        this,
                        I18n.format("status.color.chooser.title", status.name),
                        colorPreview.getBackground());
                if (c != null) {
                    colorPreview.setBackground(c);
                }
            });
            p.add(btnEdit, g);

            // Guardar referencia al preview panel (no al botón)
            colorButtons.put(key, colorPreview);

            g.gridy++;
        }
        
        // Push everything up
        GridBagConstraints gPush = new GridBagConstraints();
        gPush.gridy = g.gridy + 1;
        gPush.weighty = 1.0;
        gPush.fill = GridBagConstraints.VERTICAL;
        p.add(new JPanel() {{ setBackground(Color.WHITE); }}, gPush);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(p, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }
    
    private JScrollPane createPriorityPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        p.setBackground(Color.WHITE);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        
        JLabel lblIntro = new JLabel("<html>Configura cómo detectar y visualizar las prioridades.<br>Usa expresiones regulares (Regex) para las palabras clave.</html>");
        g.gridx = 0;
        g.gridwidth = 4;
        p.add(lblIntro, g);
        g.gridy++;
        
        // Headers
        g.gridwidth = 1;
        p.add(new JLabel("Nivel"), g);
        
        g.gridx = 1;
        p.add(new JLabel("Palabras Clave (Regex)"), g);
        
        g.gridx = 2;
        p.add(new JLabel("Color"), g);
        
        g.gridx = 3;
        p.add(new JLabel("Negrita"), g);
        
        g.gridy++;
        
        for (String prioKey : PRIO_KEYS) {
            String cleanKey = prioKey.toLowerCase();
            PriorityInputGroup inputs = new PriorityInputGroup();
            
            g.gridx = 0;
            g.weightx = 0.0;
            p.add(new JLabel(prioKey), g);
            
            g.gridx = 1;
            g.weightx = 1.0;
            inputs.txtRegex = new JTextField();
            p.add(inputs.txtRegex, g);
            
            g.gridx = 2;
            g.weightx = 0.0;
            inputs.pnlColor = new JPanel();
            inputs.pnlColor.setPreferredSize(new Dimension(24, 24));
            inputs.pnlColor.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            inputs.pnlColor.setBackground(Color.WHITE);
            
            // Click to edit color
            inputs.pnlColor.addMouseListener(new java.awt.event.MouseAdapter() {
               public void mouseClicked(java.awt.event.MouseEvent evt) {
                   Color c = JColorChooser.showDialog(StatusColorDialog.this, "Color: " + prioKey, inputs.pnlColor.getBackground());
                   if (c != null) inputs.pnlColor.setBackground(c);
               } 
            });
            p.add(inputs.pnlColor, g);
            
            g.gridx = 3;
            inputs.chkBold = new JCheckBox();
            inputs.chkBold.setBackground(Color.WHITE);
            p.add(inputs.chkBold, g);
            
            priorityInputs.put(cleanKey, inputs);
            
            g.gridy++;
        }
        
        // Push up
        GridBagConstraints gPush = new GridBagConstraints();
        gPush.gridy = g.gridy;
        gPush.weighty = 1.0;
        p.add(new JPanel() {{ setBackground(Color.WHITE); }}, gPush);
        
        JScrollPane scroll = new JScrollPane(p);
        scroll.setBorder(null);
        return scroll;
    }

    private void loadValues() {
        StyleConfig sc = new StyleConfig();
        sc.load(props, prefix);
        
        // Status Colors
        colorButtons.forEach((key, btn) -> {
            Color c = sc.getColor(key);
            if (c != null)
                btn.setBackground(c);
            else
                btn.setBackground(Color.WHITE);
        });
        
        // Priority Styles
        // Ensure defaults are loaded in StyleConfig so we have something to show
        Map<String, StyleConfig.PriorityStyle> pStyles = sc.getPriorityStyles();
        priorityInputs.forEach((key, inputs) -> {
            StyleConfig.PriorityStyle ps = pStyles.get(key);
            if (ps != null) {
                inputs.txtRegex.setText(ps.regex);
                inputs.pnlColor.setBackground(ps.color);
                inputs.chkBold.setSelected(ps.bold);
            }
        });
    }

    private void save() {
        // Save Status Colors
        colorButtons.forEach((key, btn) -> {
            Color c = btn.getBackground();
            String hex = String.format("#%06X", (0xFFFFFF & c.getRGB()));
            props.setProperty(prefix + ".color." + key, hex);
        });
        
        // Save Priority Styles
        priorityInputs.forEach((key, inputs) -> {
             props.setProperty(prefix + ".priority." + key + ".regex", inputs.txtRegex.getText());
             
             Color c = inputs.pnlColor.getBackground();
             String hex = String.format("#%06X", (0xFFFFFF & c.getRGB()));
             props.setProperty(prefix + ".priority." + key + ".color", hex);
             
             props.setProperty(prefix + ".priority." + key + ".bold", String.valueOf(inputs.chkBold.isSelected()));
        });
        
        ConfigManager.saveConfig(props);
    }
}
