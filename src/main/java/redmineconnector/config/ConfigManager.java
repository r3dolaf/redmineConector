package redmineconnector.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import redmineconnector.ui.theme.Theme;
import redmineconnector.ui.theme.ThemeConfig;

public class ConfigManager {
    private static final String FILE = "config/redmine_config.properties";
    private static final String THEME_KEY = "app.theme";

    public static Properties loadConfig() {
        Properties p = new Properties();
        File f = new File(FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
                // Decrypt all keys
                for (String name : p.stringPropertyNames()) {
                    if (name.endsWith(".key")) {
                        String encrypted = p.getProperty(name);
                        p.setProperty(name, redmineconnector.util.SecurityUtils.decrypt(encrypted));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return p;
    }

    public static void saveConfig(Properties p) {
        // Create a copy to avoid encrypting the original properties in-memory
        Properties toSave = new Properties();
        for (String name : p.stringPropertyNames()) {
            String value = p.getProperty(name);
            if (name.endsWith(".key")) {
                value = redmineconnector.util.SecurityUtils.encrypt(value);
            }
            toSave.setProperty(name, value);
        }

        try {
            File f = new File(FILE);
            if (f.getParentFile() != null && !f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(f)) {
                toSave.store(out, "Redmine Config");
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Loads the saved theme preference.
     * Checks if it's a preset or a custom theme.
     * 
     * @param props Properties object
     * @return The saved theme (ThemeConfig)
     */
    public static ThemeConfig loadTheme(Properties props) {
        String themeName = props.getProperty(THEME_KEY, "LIGHT");

        // Check if it is a preset
        try {
            return Theme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            // It might be a custom theme or just invalid
            if (themeName.equals("CUSTOM")) {
                return loadCustomTheme(props);
            }
            return Theme.LIGHT;
        }
    }

    /**
     * Saves the theme preference.
     * 
     * @param props Properties object
     * @param theme Theme to save
     */
    public static void saveTheme(Properties props, ThemeConfig theme) {
        if (theme instanceof Theme) {
            props.setProperty(THEME_KEY, ((Theme) theme).name());
        } else if (theme instanceof redmineconnector.ui.theme.CustomTheme) {
            props.setProperty(THEME_KEY, "CUSTOM");
            saveCustomTheme(props, (redmineconnector.ui.theme.CustomTheme) theme);
        }
    }

    private static ThemeConfig loadCustomTheme(Properties props) {
        redmineconnector.ui.theme.CustomTheme c = new redmineconnector.ui.theme.CustomTheme("Personalizado");
        c.background = parseColor(props.getProperty("theme.custom.background"), Theme.LIGHT.getBackground());
        c.panelBackground = parseColor(props.getProperty("theme.custom.panelBackground"),
                Theme.LIGHT.getPanelBackground());
        c.text = parseColor(props.getProperty("theme.custom.text"), Theme.LIGHT.getText());
        c.textSecondary = parseColor(props.getProperty("theme.custom.textSecondary"), Theme.LIGHT.getTextSecondary());
        c.border = parseColor(props.getProperty("theme.custom.border"), Theme.LIGHT.getBorder());
        c.tableHeader = parseColor(props.getProperty("theme.custom.tableHeader"), Theme.LIGHT.getTableHeader());
        c.tableRow = parseColor(props.getProperty("theme.custom.tableRow"), Theme.LIGHT.getTableRow());
        c.tableRowAlt = parseColor(props.getProperty("theme.custom.tableRowAlt"), Theme.LIGHT.getTableRowAlt());
        c.accent = parseColor(props.getProperty("theme.custom.accent"), Theme.LIGHT.getAccent());
        c.accentLight = parseColor(props.getProperty("theme.custom.accentLight"), Theme.LIGHT.getAccentLight());
        c.accentDark = parseColor(props.getProperty("theme.custom.accentDark"), Theme.LIGHT.getAccentDark());
        c.buttonBackground = parseColor(props.getProperty("theme.custom.buttonBackground"),
                Theme.LIGHT.getButtonBackground());
        c.buttonForeground = parseColor(props.getProperty("theme.custom.buttonForeground"),
                Theme.LIGHT.getButtonForeground());
        return c;
    }

    private static void saveCustomTheme(Properties props, redmineconnector.ui.theme.CustomTheme c) {
        props.setProperty("theme.custom.background", String.valueOf(c.getBackground().getRGB()));
        props.setProperty("theme.custom.panelBackground", String.valueOf(c.getPanelBackground().getRGB()));
        props.setProperty("theme.custom.text", String.valueOf(c.getText().getRGB()));
        props.setProperty("theme.custom.textSecondary", String.valueOf(c.getTextSecondary().getRGB()));
        props.setProperty("theme.custom.border", String.valueOf(c.getBorder().getRGB()));
        props.setProperty("theme.custom.tableHeader", String.valueOf(c.getTableHeader().getRGB()));
        props.setProperty("theme.custom.tableRow", String.valueOf(c.getTableRow().getRGB()));
        props.setProperty("theme.custom.tableRowAlt", String.valueOf(c.getTableRowAlt().getRGB()));
        props.setProperty("theme.custom.accent", String.valueOf(c.getAccent().getRGB()));
        props.setProperty("theme.custom.accentLight", String.valueOf(c.getAccentLight().getRGB()));
        props.setProperty("theme.custom.accentDark", String.valueOf(c.getAccentDark().getRGB()));
        props.setProperty("theme.custom.buttonBackground", String.valueOf(c.getButtonBackground().getRGB()));
        props.setProperty("theme.custom.buttonForeground", String.valueOf(c.getButtonForeground().getRGB()));
    }

    private static java.awt.Color parseColor(String val, java.awt.Color def) {
        if (val == null)
            return def;
        try {
            return new java.awt.Color(Integer.parseInt(val));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Exports the current configuration file to the target location.
     * 
     * @param target The file to write to.
     * @throws Exception If the copy fails.
     */
    public static void exportConfig(File target) throws Exception {
        File source = new File(FILE);
        if (!source.exists()) {
            throw new java.io.FileNotFoundException("Configuration file not found: " + FILE);
        }
        java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Imports a configuration file from the source location.
     * WARNING: Overwrites current configuration.
     * 
     * @param source The file to read from.
     * @throws Exception If the copy fails.
     */
    public static void importConfig(File source) throws Exception {
        if (!source.exists()) {
            throw new java.io.FileNotFoundException("Source file not found: " + source.getAbsolutePath());
        }
        File target = new File(FILE);
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }
        java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Creates a backup of the current configuration file.
     * Appends .bak to the filename.
     * 
     * @throws Exception If copy fails.
     */
    public static void backupConfig() throws Exception {
        File source = new File(FILE);
        if (!source.exists())
            return;
        File backup = new File(FILE + ".bak");
        java.nio.file.Files.copy(source.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Resets configuration by deleting the file.
     */
    public static void resetConfig() {
        File source = new File(FILE);
        if (source.exists()) {
            source.delete();
        }
    }

    /**
     * Exports a sanitized version of the configuration (no API Keys or User data).
     * 
     * @param target File to save to.
     * @throws Exception If save fails.
     */
    public static void exportSanitizedConfig(File target) throws Exception {
        Properties p = loadConfig();
        // Remove sensitive data
        p.keySet().removeIf(k -> k.toString().endsWith(".key"));
        p.keySet().removeIf(k -> k.toString().endsWith(".userEmail"));

        try (FileOutputStream out = new FileOutputStream(target)) {
            p.store(out, "Redmine Config (Sanitized)");
        }
    }
}
