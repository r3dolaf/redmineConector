package redmineconnector.ui.components;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

/**
 * A specialized JTable that adapts to the current Look and Feel (including Dark
 * Mode).
 * It provides helper methods to determine if the current theme is dark.
 */
public class ThemeTable extends JTable {

    public ThemeTable(TableModel model) {
        super(model);
        setupDefaults();
    }

    private void setupDefaults() {
        setRowHeight(24);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        getTableHeader().setReorderingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Ensure header follows theme
        JTableHeader header = getTableHeader();
        header.setOpaque(true);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // Re-apply defaults if LaF changes
        setShowGrid(false);
    }

    /**
     * Determines if the current table background suggests a Dark Mode context.
     */
    public boolean isDark() {
        Color bg = getBackground();
        if (bg == null)
            bg = Color.WHITE;
        return isColorDark(bg);
    }

    public static boolean isColorDark(Color color) {
        if (color == null)
            return false;
        double darkness = 1 - (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return darkness > 0.5;
    }
}
