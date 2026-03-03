package redmineconnector.ui.theme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages application themes and applies them to UI components.
 * Supports switching between themes at runtime.
 */
public class ThemeManager {
    private static ThemeConfig currentTheme = Theme.LIGHT;
    private static final List<ThemeChangeListener> listeners = new ArrayList<>();

    /**
     * Listener interface for theme changes.
     */
    public interface ThemeChangeListener {
        void onThemeChanged(ThemeConfig newTheme);
    }

    /**
     * Gets the current active theme.
     */
    public static ThemeConfig getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Sets the current theme and notifies all listeners.
     */
    public static void setTheme(ThemeConfig theme) {
        if (theme == null || theme == currentTheme) {
            return;
        }
        currentTheme = theme;
        notifyListeners();
    }

    /**
     * Registers a listener for theme changes.
     */
    public static void addThemeChangeListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a theme change listener.
     */
    public static void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (ThemeChangeListener listener : listeners) {
            try {
                listener.onThemeChanged(currentTheme);
            } catch (Exception e) {
                redmineconnector.util.LoggerUtil.logError("ThemeManager",
                        "Theme change listener failed", e);
            }
        }
    }

    /**
     * Applies the current theme to a component and all its children.
     */
    public static void applyTheme(Component component) {
        if (component == null) {
            return;
        }

        ThemeConfig theme = currentTheme;

        // Apply to the component itself
        component.setBackground(theme.getPanelBackground());
        component.setForeground(theme.getText());

        // Special handling for specific component types
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            panel.setBackground(theme.getPanelBackground());
        } else if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            label.setForeground(theme.getText());
        } else if (component instanceof JButton) {
            JButton button = (JButton) component;
            // Apply theme to buttons
            button.setBackground(theme.getButtonBackground());
            button.setForeground(theme.getButtonForeground());
            button.setFocusPainted(false);
            // Add a subtle border for buttons in dark mode to make them distinguishable
        } else if (component instanceof javax.swing.text.JTextComponent) {
            javax.swing.text.JTextComponent textComp = (javax.swing.text.JTextComponent) component;
            textComp.setBackground(theme.getBackground());
            textComp.setForeground(theme.getText());
            textComp.setCaretColor(theme.getText());
            textComp.setBorder(BorderFactory.createLineBorder(theme.getBorder()));
        } else if (component instanceof JTable) {
            applyThemeToTable((JTable) component);
        } else if (component instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) component;
            scrollPane.setBackground(theme.getBackground());
            scrollPane.getViewport().setBackground(theme.getBackground());
            if (scrollPane.getViewport().getView() != null) {
                applyTheme(scrollPane.getViewport().getView());
            }
        } else if (component instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) component;
            combo.setBackground(theme.getBackground());
            combo.setForeground(theme.getText());
        }

        // Recursively apply to children
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                applyTheme(child);
            }
        }
    }

    /**
     * Applies theme specifically to a JTable.
     */
    private static void applyThemeToTable(JTable table) {
        ThemeConfig theme = currentTheme;

        table.setBackground(theme.getTableRow());
        table.setForeground(theme.getText());
        table.setGridColor(theme.getBorder());
        table.setSelectionBackground(theme.getAccentLight());
        table.setSelectionForeground(theme.getText());

        // Header
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(theme.getTableHeader());
            header.setForeground(theme.getText());
            header.setBorder(BorderFactory.createLineBorder(theme.getBorder()));
            header.setOpaque(true);
            // Force repaint to apply changes
            header.repaint();

            // Also update the UI default for TableHeader to ensure consistency
            UIManager.put("TableHeader.background", theme.getTableHeader());
            UIManager.put("TableHeader.foreground", theme.getText());
        }

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? theme.getTableRow() : theme.getTableRowAlt());
                    c.setForeground(theme.getText());
                } else {
                    c.setBackground(theme.getAccentLight());
                    c.setForeground(theme.getText());
                }
                return c;
            }
        });
    }

    /**
     * Creates a themed border.
     */
    public static javax.swing.border.Border createThemedBorder() {
        return BorderFactory.createLineBorder(currentTheme.getBorder());
    }

    /**
     * Creates a themed titled border.
     */
    public static javax.swing.border.Border createThemedTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(currentTheme.getBorder()),
                title,
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                null,
                currentTheme.getText());
    }
}
