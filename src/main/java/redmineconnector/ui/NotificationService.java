package redmineconnector.ui;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import redmineconnector.util.I18n;
import redmineconnector.util.LoggerUtil;

/**
 * Servicio centralizado para gestionar notificaciones al usuario y registro de
 * errores.
 * Abstrae la forma en que se muestran los mensajes (Diálogos, Toasts, Logs).
 */
public class NotificationService {
    private final Component parentComponent;
    private final InstanceController controller;

    /**
     * @param parentComponent Componente padre para los diálogos modales (puede ser
     *                        null)
     * @param controller      Controlador principal para acceder al log
     */
    public NotificationService(Component parentComponent, InstanceController controller) {
        this.parentComponent = parentComponent;
        this.controller = controller;
    }

    /**
     * Muestra un mensaje de error crítico al usuario y lo registra en el log.
     * 
     * @param message Mensaje descriptivo para el usuario
     * @param t       Excepción (opcional)
     */
    public void showError(String message, Throwable t) {
        String logMsg = message + (t != null ? ": " + t.getMessage() : "");
        if (controller != null) {
            controller.log("ERROR: " + logMsg);
        } else {
            LoggerUtil.logError("NotificationService", logMsg, t instanceof Exception ? (Exception) t : null);
        }

        if (t != null) {
            // Stack trace already logged by LoggerUtil above
        }

        // Check config
        if (controller != null && controller.getConfig() != null && !controller.getConfig().notifyErrors) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parentComponent,
                    message + "\n" + (t != null ? t.getMessage() : ""),
                    I18n.get("notification.title.error"),
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Muestra un mensaje de error simple.
     */
    public void showError(String message) {
        showError(message, null);
    }

    /**
     * Muestra un mensaje de información.
     */
    public void showInfo(String message) {
        if (controller != null) {
            controller.log("INFO: " + message);
        }

        // Confirmation/Info
        if (controller != null && controller.getConfig() != null && !controller.getConfig().notifyConfirmations) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            // Use Toast for non-intrusive notification
            if (parentComponent instanceof java.awt.Window) {
                new redmineconnector.ui.components.ToastNotification((java.awt.Window) parentComponent, message,
                        redmineconnector.ui.components.ToastNotification.Type.INFO).showToast();
            } else {
                // Fallback if parent is not a window
                JOptionPane.showMessageDialog(parentComponent, message, I18n.get("notification.title.info"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    /**
     * Muestra un mensaje de advertencia.
     */
    public void showWarning(String message) {
        if (controller != null) {
            controller.log("WARN: " + message);
        }

        if (controller != null && controller.getConfig() != null && !controller.getConfig().notifyWarnings) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            // Use Toast for warning
            if (parentComponent instanceof java.awt.Window) {
                new redmineconnector.ui.components.ToastNotification((java.awt.Window) parentComponent, message,
                        redmineconnector.ui.components.ToastNotification.Type.WARNING).showToast();
            } else {
                JOptionPane.showMessageDialog(parentComponent, message, I18n.get("notification.title.warn"),
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    /**
     * Muestra un mensaje de éxito (podría ser un toast en el futuro).
     * Actualmente loguea y actualiza la barra de estado si existe, o muestra
     * diálogo si es crítico.
     * Para operaciones comunes, preferimos solo log y actualizar estado, no
     * interrumpir con modal.
     */
    /**
     * Muestra un mensaje de éxito (podría ser un toast en el futuro).
     * Actualmente loguea y actualiza la barra de estado si existe, o muestra
     * diálogo si es crítico.
     * Para operaciones comunes, preferimos solo log y actualizar estado, no
     * interrumpir con modal.
     */
    public void showSuccess(String message) {
        if (controller != null) {
            controller.log("SUCCESS: " + message);
        }

        if (controller != null && controller.getConfig() != null && !controller.getConfig().notifyConfirmations) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (parentComponent instanceof java.awt.Window) {
                new redmineconnector.ui.components.ToastNotification((java.awt.Window) parentComponent, message,
                        redmineconnector.ui.components.ToastNotification.Type.SUCCESS).showToast();
            } else {
                JOptionPane.showMessageDialog(parentComponent, message, I18n.get("notification.title.success"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}
