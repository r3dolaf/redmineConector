package redmineconnector.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import redmineconnector.model.LogEntry;
import redmineconnector.model.LogEntry.Level;
import redmineconnector.ui.UIHelper;
import redmineconnector.ui.theme.ThemeConfig;
import redmineconnector.ui.theme.ThemeManager;
import redmineconnector.util.RollingFileLogger;

public class LogPanel extends JPanel implements ThemeManager.ThemeChangeListener {
    private final JTextPane textPane;
    private final List<LogEntry> allEntries = new ArrayList<>();

    private String searchText = "";
    private boolean isMinimized = false; // Added state variable
    private boolean autoScroll = true;

    private final JCheckBox chkDebug, chkInfo, chkWarn, chkError, chkAutoScroll;
    private final JTextField txtSearch;
    private final JComboBox<String> comboSource;
    private final JScrollPane scroll;

    public LogPanel() {
        super(new BorderLayout());

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textPane.setOpaque(true);

        ThemeManager.addThemeChangeListener(this);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        toolbar.add(new JLabel("Log:"));

        chkDebug = new JCheckBox("D", true);
        chkDebug.setToolTipText("Mostrar Debug");
        chkInfo = new JCheckBox("I", true);
        chkInfo.setToolTipText("Mostrar Info");
        chkWarn = new JCheckBox("W", true);
        chkWarn.setToolTipText("Mostrar Warnings");
        chkError = new JCheckBox("E", true);
        chkError.setToolTipText("Mostrar Errores");

        toolbar.add(chkDebug);
        toolbar.add(chkInfo);
        toolbar.add(chkWarn);
        toolbar.add(chkError);

        chkDebug.addActionListener(e -> refreshView());
        chkInfo.addActionListener(e -> refreshView());
        chkWarn.addActionListener(e -> refreshView());
        chkError.addActionListener(e -> refreshView());

        toolbar.add(new JLabel("| Buscar:"));
        txtSearch = new JTextField(10);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applySearch();
            }

            public void removeUpdate(DocumentEvent e) {
                applySearch();
            }

            public void changedUpdate(DocumentEvent e) {
                applySearch();
            }
        });
        toolbar.add(txtSearch);

        toolbar.add(new JLabel("| Origen:"));
        comboSource = new JComboBox<>();
        comboSource.addItem("Todos");
        comboSource.addItem("System");
        comboSource.addActionListener(e -> refreshView());
        toolbar.add(comboSource);

        JButton btnExport = new JButton("💾");
        btnExport.setToolTipText("Exportar Log");
        btnExport.addActionListener(e -> exportLog());

        JButton btnExpand = new JButton("🔍");
        btnExpand.setToolTipText("Expandir");
        btnExpand.addActionListener(e -> expandLog());

        JButton btnClear = new JButton("🗑️");
        btnClear.setToolTipText("Limpiar");
        btnClear.addActionListener(e -> {
            allEntries.clear();
            textPane.setText("");
        });

        chkAutoScroll = new JCheckBox("🔒", false);
        chkAutoScroll.setToolTipText("Bloquear Scroll (Pausar)");
        chkAutoScroll.addActionListener(e -> autoScroll = !chkAutoScroll.isSelected());

        JButton btnMinimize = new JButton("🔽");
        btnMinimize.setToolTipText("Minimizar/Restaurar Log");
        btnMinimize.addActionListener(e -> toggleMinimize(btnMinimize));

        toolbar.add(btnExport);
        toolbar.add(btnClear);
        toolbar.add(chkAutoScroll);
        toolbar.add(btnExpand);
        toolbar.add(btnMinimize);

        add(toolbar, BorderLayout.NORTH);

        // Text Area
        scroll = new JScrollPane(textPane);
        scroll.setBorder(new TitledBorder("Console Output"));
        add(scroll, BorderLayout.CENTER);

        // Start File Logger
        RollingFileLogger.init();

        // Start minimized
        toggleMinimize(btnMinimize);
    }

    private void toggleMinimize(JButton btn) {
        isMinimized = !isMinimized;

        if (!isMinimized) {
            // Restore / Expand
            setPreferredSize(new Dimension(100, 200));
            btn.setText("🔽");
        } else {
            // Minimize (keep visible but small)
            setPreferredSize(new Dimension(100, 60));
            btn.setText("🔼");
        }
        revalidate();

        // Find ancestor window/frame to re-pack or revalidate
        java.awt.Window win = SwingUtilities.getWindowAncestor(this);
        if (win != null) {
            win.validate();
        }
    }

    private void applySearch() {
        searchText = txtSearch.getText().trim().toLowerCase();
        refreshView();
    }

    public void addLog(String message) {
        addLog(Level.INFO, message);
    }

    public void addLog(Level level, String message) {
        addLog(level, message, "System");
    }

    public void addLog(Level level, String message, String source) {
        LogEntry entry = new LogEntry(level, message, source);

        // Add to list safely
        SwingUtilities.invokeLater(() -> {
            synchronized (allEntries) {
                allEntries.add(entry);
                // Update source combo if new source found
                boolean found = false;
                for (int i = 0; i < comboSource.getItemCount(); i++) {
                    if (comboSource.getItemAt(i).equals(source)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    comboSource.addItem(source);
            }
            // File logging
            RollingFileLogger.log("[" + source + "] [" + level + "] " + message);

            // UI Update check - refreshView will handle filtering and appending
            refreshView();
        });
    }

    private boolean shouldShow(LogEntry e) {
        if (!chkDebug.isSelected() && e.level == Level.DEBUG)
            return false;
        if (!chkInfo.isSelected() && e.level == Level.INFO)
            return false;
        if (!chkWarn.isSelected() && e.level == Level.WARN)
            return false;
        if (!chkError.isSelected() && e.level == Level.ERROR)
            return false;

        if (!searchText.isEmpty() && !e.message.toLowerCase().contains(searchText)) {
            return false;
        }

        String filterSource = (String) comboSource.getSelectedItem();
        if (filterSource != null && !"Todos".equals(filterSource)) {
            return filterSource.equals(e.source);
        }

        return true;
    }

    private void refreshView() {
        textPane.setText("");
        synchronized (allEntries) {
            for (LogEntry e : allEntries) {
                if (shouldShow(e)) {
                    appendToPane(e);
                }
            }
        }
    }

    private void appendToPane(LogEntry entry) {
        ThemeConfig theme = ThemeManager.getCurrentTheme();
        Color c = theme.getText();

        if (entry.level == Level.WARN) {
            c = new Color(220, 110, 0);
        } else if (entry.level == Level.ERROR) {
            c = Color.RED;
        } else if (entry.level == Level.DEBUG) {
            c = Color.GRAY;
        }

        append(c, entry.toString() + "\n");
        if (autoScroll) {
            SwingUtilities.invokeLater(() -> {
                // Simple scroll to end
                try {
                    textPane.setCaretPosition(textPane.getDocument().getLength());
                } catch (Exception ignored) {
                }
            });
        }
    }

    @Override
    public void onThemeChanged(ThemeConfig newTheme) {
        // Apply theme styles
        this.setBackground(newTheme.getPanelBackground());
        textPane.setBackground(newTheme.getBackground());
        textPane.setForeground(newTheme.getText());
    }

    private void append(Color c, String s) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        int len = textPane.getDocument().getLength();
        try {
            textPane.getDocument().insertString(len, s, aset);
        } catch (BadLocationException e) {
            redmineconnector.util.LoggerUtil.logError("LogPanel",
                    "Failed to append text to log panel", e);
        }
    }

    private void exportLog() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("export_log.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                synchronized (allEntries) {
                    for (LogEntry e : allEntries)
                        pw.println(e.toString());
                }
                JOptionPane.showMessageDialog(this, "Exportado correctamente.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    public void expandLog() {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this));
        d.setTitle("Log Completo");
        d.setModal(false);
        d.setSize(900, 700);
        d.setLocationRelativeTo(this);

        LogPanel copyPanel = new LogPanel();
        synchronized (allEntries) {
            for (LogEntry e : allEntries)
                copyPanel.addLog(e.level, e.message);
        }

        d.add(copyPanel);
        UIHelper.addEscapeListener(d);
        d.setVisible(true);
    }
}
