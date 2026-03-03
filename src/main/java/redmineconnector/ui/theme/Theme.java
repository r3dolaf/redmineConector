package redmineconnector.ui.theme;

import java.awt.Color;

/**
 * Defines available themes with their color schemes.
 * Each theme provides colors for all UI components.
 */
public enum Theme implements ThemeConfig {
    LIGHT("Claro",
            new Color(255, 255, 255), // background
            new Color(248, 250, 252), // panelBackground - Subtle blue tint
            new Color(30, 30, 30), // text
            new Color(100, 100, 100), // textSecondary
            new Color(220, 220, 220), // border
            new Color(240, 245, 248), // tableHeader - Light blue tint
            new Color(255, 255, 255), // tableRow - White
            new Color(245, 252, 248), // tableRowAlt - Very light green tint
            new Color(0, 114, 184), // accent - Bilbomatica blue #0072B8
            new Color(230, 245, 252), // accentLight - Very light blue for selection
            new Color(0, 90, 150)// accentDark - Darker blue
    );

    public final String displayName;
    public final Color background;
    public final Color panelBackground;
    public final Color text;
    public final Color textSecondary;
    public final Color border;
    public final Color tableHeader;
    public final Color tableRow;
    public final Color tableRowAlt;
    public final Color accent;
    public final Color accentLight;
    public final Color accentDark;
    public final Color buttonBackground;
    public final Color buttonForeground;

    Theme(String displayName, Color background, Color panelBackground, Color text, Color textSecondary,
            Color border, Color tableHeader, Color tableRow, Color tableRowAlt,
            Color accent, Color accentLight, Color accentDark) {
        this.displayName = displayName;
        this.background = background;
        this.panelBackground = panelBackground;
        this.text = text;
        this.textSecondary = textSecondary;
        this.border = border;
        this.tableHeader = tableHeader;
        this.tableRow = tableRow;
        this.tableRowAlt = tableRowAlt;
        this.accent = accent;
        this.accentLight = accentLight;
        this.accentDark = accentDark;

        // Initialize button colors based on theme type
        if ("Claro".equals(displayName)) {
            // Light Theme Buttons: Standard White/Light Grey with Dark Text
            this.buttonBackground = new Color(255, 255, 255);
            this.buttonForeground = new Color(30, 30, 30);
        } else {
            // Dark Theme Buttons: Inverted (Black/Dark Grey with White Text)
            this.buttonBackground = new Color(0, 0, 0);
            this.buttonForeground = new Color(255, 255, 255);
        }
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public Color getPanelBackground() {
        return panelBackground;
    }

    @Override
    public Color getText() {
        return text;
    }

    @Override
    public Color getTextSecondary() {
        return textSecondary;
    }

    @Override
    public Color getBorder() {
        return border;
    }

    @Override
    public Color getTableHeader() {
        return tableHeader;
    }

    @Override
    public Color getTableRow() {
        return tableRow;
    }

    @Override
    public Color getTableRowAlt() {
        return tableRowAlt;
    }

    @Override
    public Color getAccent() {
        return accent;
    }

    @Override
    public Color getAccentLight() {
        return accentLight;
    }

    @Override
    public Color getAccentDark() {
        return accentDark;
    }

    @Override
    public Color getButtonBackground() {
        return buttonBackground;
    }

    @Override
    public Color getButtonForeground() {
        return buttonForeground;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
