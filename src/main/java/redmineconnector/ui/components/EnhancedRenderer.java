package redmineconnector.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import redmineconnector.config.StyleConfig;

public class EnhancedRenderer extends DefaultTableCellRenderer {
    StyleConfig styles;
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    JProgressBar progressBar = new JProgressBar(0, 100);
    private int mouseRow = -1;
    private java.util.function.Predicate<Integer> pinChecker;

    public void setPinChecker(java.util.function.Predicate<Integer> pinChecker) {
        this.pinChecker = pinChecker;
    }

    public EnhancedRenderer(StyleConfig s) {
        this.styles = s;
        progressBar.setStringPainted(true);
        progressBar.setBorderPainted(false);
    }

    public void setMouseRow(int r) {
        this.mouseRow = r;
    }

    public int getMouseRow() {
        return this.mouseRow;
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
        if (c == 10 && v instanceof Integer) { // % column
            progressBar.setValue((Integer) v);
            progressBar.setBackground(sel ? t.getSelectionBackground() : Color.WHITE);
            return progressBar;
        }
        Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
        if (v instanceof Date)
            setValue(sdf.format((Date) v));
        if (v instanceof Double)
            setValue(String.format("%.2f", (Double) v));

        if (!sel) {
            int modelRow = t.convertRowIndexToModel(r);
            String status = (String) t.getModel().getValueAt(modelRow, 2);
            Color bg = styles.getColor(status);

            // Default to table background instead of WHITE
            if (bg == null) {
                bg = t.getBackground();
            }

            // Hover effect
            if (r == mouseRow) {
                float[] hsb = Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null);
                // In dark mode, lighten; in light mode, darken
                boolean isDark = ThemeTable.isColorDark(bg);
                float brightness = hsb[2];
                if (isDark) {
                    brightness = Math.min(1.0f, brightness + 0.1f);
                } else {
                    brightness = Math.max(0.0f, brightness - 0.05f);
                }
                bg = Color.getHSBColor(hsb[0], hsb[1], brightness);
            }
            comp.setBackground(bg);

            // Adjust text color for contrast
            if (ThemeTable.isColorDark(bg)) {
                comp.setForeground(Color.WHITE);
            } else {
                comp.setForeground(Color.BLACK);
            }

            String p = (String) t.getModel().getValueAt(modelRow, 3);
            if (p == null)
                p = "";
            p = p.toLowerCase();
            StyleConfig.PriorityStyle pStyle = styles.getPriorityStyle(p);
            
            if (pStyle != null && pStyle.bold)
                comp.setFont(comp.getFont().deriveFont(Font.BOLD));

            if (c == 0) { // ID Column: show priority dot and/or pin star
                // final String priority = p; // No longer needed
                Object idObj = t.getModel().getValueAt(modelRow, 0);
                final int taskId = (idObj instanceof Integer) ? (Integer) idObj : -1;
                final boolean isPinned = pinChecker != null && pinChecker.test(taskId);

                final Color fgColor = comp.getForeground(); // Capture current text color for icon drawing

                setIcon(new Icon() {
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int drawX = x;
                        if (isPinned) {
                            g2.setColor(styles.actionWarning); // Pin color (Gold)
                            g2.drawString("⭐", drawX - 2, y + 11);
                            drawX += 14;
                        }

                        // Use configurable priority color
                        Color dotColor = (pStyle != null && pStyle.color != null) ? pStyle.color : styles.actionSuccess;
                        g2.setColor(dotColor);
                        
                        g2.fillOval(drawX, y + 4, 8, 8);

                        // Use contrast color for border
                        g2.setColor(redmineconnector.ui.components.ThemeTable.isColorDark(fgColor) ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                        g2.drawOval(drawX, y + 4, 8, 8);
                    }

                    public int getIconWidth() {
                        return isPinned ? 26 : 12;
                    }

                    public int getIconHeight() {
                        return 12;
                    }
                });
            } else {
                setIcon(null);
            }
        } else {
            setIcon(null);
        }
        ((JComponent) comp).setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        return comp;
    }
}
