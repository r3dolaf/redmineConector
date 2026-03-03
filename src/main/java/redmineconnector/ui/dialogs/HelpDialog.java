package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Font;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import redmineconnector.ui.components.ThemeTable;
import redmineconnector.util.I18n;

public class HelpDialog extends JDialog {

    public HelpDialog(Frame owner) {
        super(owner, I18n.get("help.dialog.title"), false);
        createUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void createUI() {
        setLayout(new BorderLayout());
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(root);

        // Tabbed pane for organized content
        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Keyboard Shortcuts
        tabs.addTab(I18n.get("help.tab.shortcuts"), createShortcutsPanel());

        // Tab 2: Main Features
        tabs.addTab(I18n.get("help.tab.features"), createFeaturesPanel());

        // Tab 3: Quick Tips
        tabs.addTab(I18n.get("help.tab.tips"), createTipsPanel());

        root.add(tabs, BorderLayout.CENTER);

        // Close on Esc
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel createShortcutsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JLabel lblInfo = new JLabel("<html><b>" + I18n.get("help.shortcuts.title") + "</b><br>" +
                I18n.get("help.shortcuts.subtitle") + "</html>");
        lblInfo.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(lblInfo, BorderLayout.NORTH);

        // Table
        String[] columns = { I18n.get("help.column.action"), I18n.get("help.column.shortcut") };
        Object[][] data = {
                // Global Navigation
                { "═══ " + I18n.get("help.category.navigation") + " ═══", "" },
                { I18n.get("help.action.global_search"), "Ctrl+P" },
                { I18n.get("help.action.switch_tab"), "Ctrl+1 ... Ctrl+9" },
                { I18n.get("help.action.help"), "F1" },
                { I18n.get("help.action.close_dialog"), "Esc" },
                { "", "" },

                // Task Operations
                { "═══ " + I18n.get("help.category.tasks") + " ═══", "" },
                { I18n.get("help.action.refresh"), "F5 / Ctrl+R" },
                { I18n.get("help.action.new_task"), "Ctrl+N" },
                { I18n.get("help.action.search"), "Ctrl+F" },
                { I18n.get("help.action.open_task"), "Enter" },
                { I18n.get("help.action.edit_task"), "Ctrl+E / E" },
                { I18n.get("help.action.download_task"), "Ctrl+D" },
                { I18n.get("help.action.copy_id"), "Ctrl+Shift+C" },
                { I18n.get("help.action.delete_task"), "Delete" },
                { "", "" },

                // List Navigation
                { "═══ " + I18n.get("help.category.list_nav") + " ═══", "" },
                { I18n.get("help.action.next_task"), "J / ↓" },
                { I18n.get("help.action.prev_task"), "K / ↑" },
                { I18n.get("help.action.first_task"), "Home" },
                { I18n.get("help.action.last_task"), "End" },
                { I18n.get("help.action.focus_search_field"), "/" },
                { "", "" },

                // Quick View
                { "═══ " + I18n.get("help.category.quickview") + " ═══", "" },
                { I18n.get("help.action.toggle_quickview"), "Q" },
                { I18n.get("help.action.prev_tab_qv"), "W" },
                { I18n.get("help.action.next_tab_qv"), "E" },
                { I18n.get("help.action.qv_tab_direct"), "Ctrl+1 ... Ctrl+4" },
                { "", "" },

                // Multi-selection
                { "═══ " + I18n.get("help.category.multiselect") + " ═══", "" },
                { I18n.get("help.action.multi_select"), "Shift+Click" },
                { I18n.get("help.action.toggle_select"), "Ctrl+Click" }
        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ThemeTable table = new ThemeTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(500, 400));

        // Make category rows bold
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                String text = value != null ? value.toString() : "";
                if (text.startsWith("═══")) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    setForeground(new java.awt.Color(0, 102, 204));
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setForeground(java.awt.Color.BLACK);
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(table.getBackground());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFeaturesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JLabel lblInfo = new JLabel("<html><b>" + I18n.get("help.features.title") + "</b><br>" +
                I18n.get("help.features.subtitle") + "</html>");
        lblInfo.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(lblInfo, BorderLayout.NORTH);

        // Table
        String[] columns = { I18n.get("help.column.feature"), I18n.get("help.column.description") };
        Object[][] data = {
                // Task Management
                { "═══ " + I18n.get("help.category.task_mgmt") + " ═══", "" },
                { I18n.get("help.feature.create_task"), I18n.get("help.feature.create_task.desc") },
                { I18n.get("help.feature.edit_task"), I18n.get("help.feature.edit_task.desc") },
                { I18n.get("help.feature.pin_task"), I18n.get("help.feature.pin_task.desc") },
                { I18n.get("help.feature.subtask"), I18n.get("help.feature.subtask.desc") },
                { I18n.get("help.feature.download"), I18n.get("help.feature.download.desc") },
                { "", "" },

                // Twin Synchronization
                { "═══ " + I18n.get("help.category.twins") + " ═══", "" },
                { I18n.get("help.feature.clone_server"), I18n.get("help.feature.clone_server.desc") },
                { I18n.get("help.feature.twin_match"), I18n.get("help.feature.twin_match.desc") },
                { I18n.get("help.feature.sync_close"), I18n.get("help.feature.sync_close.desc") },
                { I18n.get("help.feature.multi_close"), I18n.get("help.feature.multi_close.desc") },
                { "", "" },

                // Filters & Search
                { "═══ " + I18n.get("help.category.filters") + " ═══", "" },
                { I18n.get("help.feature.basic_search"), I18n.get("help.feature.basic_search.desc") },
                { I18n.get("help.feature.exclude"), I18n.get("help.feature.exclude.desc") },
                { I18n.get("help.feature.date_filter"), I18n.get("help.feature.date_filter.desc") },
                { I18n.get("help.feature.smart_filter"), I18n.get("help.feature.smart_filter.desc") },
                { I18n.get("help.feature.keyword_analysis"), I18n.get("help.feature.keyword_analysis.desc") },
                { "", "" },

                // Notifications & Offline
                { "═══ " + I18n.get("help.category.system") + " ═══", "" },
                { I18n.get("help.feature.notifications"), I18n.get("help.feature.notifications.desc") },
                { I18n.get("help.feature.offline_mode"), I18n.get("help.feature.offline_mode.desc") },
                { I18n.get("help.feature.quickview"), I18n.get("help.feature.quickview.desc") },
                { I18n.get("help.feature.kanban"), I18n.get("help.feature.kanban.desc") }
        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ThemeTable table = new ThemeTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(600, 400));
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);

        // Make category rows bold
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                String text = value != null ? value.toString() : "";
                if (text.startsWith("═══")) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    setForeground(new java.awt.Color(0, 102, 204));
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setForeground(java.awt.Color.BLACK);
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(table.getBackground());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTipsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JLabel lblInfo = new JLabel("<html><b>" + I18n.get("help.tips.title") + "</b><br>" +
                I18n.get("help.tips.subtitle") + "</html>");
        lblInfo.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(lblInfo, BorderLayout.NORTH);

        // Table
        String[] columns = { I18n.get("help.column.tip"), I18n.get("help.column.explanation") };
        Object[][] data = {
                { I18n.get("help.tip.1.title"), I18n.get("help.tip.1.desc") },
                { I18n.get("help.tip.2.title"), I18n.get("help.tip.2.desc") },
                { I18n.get("help.tip.3.title"), I18n.get("help.tip.3.desc") },
                { I18n.get("help.tip.4.title"), I18n.get("help.tip.4.desc") },
                { I18n.get("help.tip.5.title"), I18n.get("help.tip.5.desc") },
                { I18n.get("help.tip.6.title"), I18n.get("help.tip.6.desc") },
                { I18n.get("help.tip.7.title"), I18n.get("help.tip.7.desc") },
                { I18n.get("help.tip.8.title"), I18n.get("help.tip.8.desc") },
                { I18n.get("help.tip.9.title"), I18n.get("help.tip.9.desc") },
                { I18n.get("help.tip.10.title"), I18n.get("help.tip.10.desc") }
        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ThemeTable table = new ThemeTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(600, 350));
        table.getColumnModel().getColumn(0).setPreferredWidth(250);
        table.getColumnModel().getColumn(1).setPreferredWidth(350);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(table.getBackground());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }
}
