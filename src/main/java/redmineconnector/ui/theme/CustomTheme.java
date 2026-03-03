package redmineconnector.ui.theme;

import java.awt.Color;

public class CustomTheme implements ThemeConfig {
    private String displayName;
    public Color background;
    public Color panelBackground;
    public Color text;
    public Color textSecondary;
    public Color border;
    public Color tableHeader;
    public Color tableRow;
    public Color tableRowAlt;
    public Color accent;
    public Color accentLight;
    public Color accentDark;
    public Color buttonBackground;
    public Color buttonForeground;

    public CustomTheme(String displayName) {
        this.displayName = displayName;
        // IMPORTANTE: Inicializar con valores por defecto seguros
        // Esto previene fondos negros cuando se deserializa desde configuracion
        this.background = Color.WHITE;
        this.panelBackground = new Color(248, 250, 252);
        this.text = Color.BLACK;
        this.textSecondary = new Color(100, 100, 100);
        this.border = new Color(220, 220, 220);
        this.tableHeader = new Color(240, 245, 248);
        this.tableRow = Color.WHITE; // CRITICO: Fondo blanco para tablas
        this.tableRowAlt = new Color(248, 250, 252);
        this.accent = new Color(0, 114, 184);
        this.accentLight = new Color(230, 245, 252);
        this.accentDark = new Color(0, 90, 150);
        this.buttonBackground = Color.WHITE;
        this.buttonForeground = Color.BLACK;
    }

    // Copy constructor
    public CustomTheme(ThemeConfig source, String newName) {
        this.displayName = newName;
        this.background = source.getBackground();
        this.panelBackground = source.getPanelBackground();
        this.text = source.getText();
        this.textSecondary = source.getTextSecondary();
        this.border = source.getBorder();
        this.tableHeader = source.getTableHeader();
        this.tableRow = source.getTableRow();
        this.tableRowAlt = source.getTableRowAlt();
        this.accent = source.getAccent();
        this.accentLight = source.getAccentLight();
        this.accentDark = source.getAccentDark();
        this.buttonBackground = source.getButtonBackground();
        this.buttonForeground = source.getButtonForeground();
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
}
