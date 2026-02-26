package redmineconnector;

import java.awt.Color;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import redmineconnector.config.ConfigManager;
import redmineconnector.ui.MainFrame;

public class RedmineConnectorApp {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            redmineconnector.util.LoggerUtil.logError("UncaughtExceptionHandler",
                    "Uncaught exception in thread " + t.getName(),
                    e instanceof Exception ? (Exception) e : new Exception(e));
            JOptionPane.showMessageDialog(null, "Error Crítico: " + e.getMessage(), "Error Fatal",
                    JOptionPane.ERROR_MESSAGE);
        });

        SwingUtilities.invokeLater(() -> {
            setupLookAndFeel();
            new MainFrame().setVisible(true);
        });
    }

    private static void setupLookAndFeel() {
        try {
            // Cargar configuración de Look&Feel
            Properties props = ConfigManager.loadConfig();
            String savedLaf = props.getProperty("app.lookandfeel");

            if (savedLaf != null && !savedLaf.isEmpty()) {
                try {
                    UIManager.setLookAndFeel(savedLaf);
                    redmineconnector.util.LoggerUtil.logInfo("RedmineConnectorApp",
                            "[L&F] Look and Feel cargado: " + savedLaf);
                } catch (Exception e) {
                    redmineconnector.util.LoggerUtil.logError("RedmineConnectorApp",
                            "[WARN] No se pudo cargar L&F guardado, usando sistema", e);
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
            } else {
                // Usar el Look and Feel del sistema por defecto
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }

            // ... (omitting lines 30-31 as they are unchanged)
            UIManager.put("EditorPane.background", Color.WHITE);
            UIManager.put("EditorPane.foreground", Color.BLACK);

            redmineconnector.util.LoggerUtil.logInfo("RedmineConnectorApp",
                    "[L&F] Look and Feel activo: " + UIManager.getLookAndFeel().getName());

        } catch (Exception e) {
            redmineconnector.util.LoggerUtil.logError("RedmineConnectorApp",
                    "[ERROR] No se pudo cargar el LookAndFeel del sistema", e);
        }
    }
}
