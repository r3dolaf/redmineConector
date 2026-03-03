package redmineconnector.util;

import javax.swing.JOptionPane;
import java.io.IOException;

/**
 * Centraliza el manejo de errores en la aplicación.
 * 
 * <p>
 * Proporciona métodos para manejar excepciones de forma consistente,
 * mostrando mensajes amigables al usuario y registrando errores en logs.
 * </p>
 * 
 * @author Redmine Connector Team
 * @version 8.5.5
 */
public class ErrorHandler {

    /**
     * Maneja una excepción mostrando un mensaje al usuario y registrándola.
     * 
     * @param exception La excepción a manejar
     * @param context   Contexto donde ocurrió el error (ej: "Carga de tareas")
     */
    public static void handle(Exception exception, String context) {
        LoggerUtil.logError("ErrorHandler", context, exception);
        showUserFriendlyError(exception, context);
    }

    /**
     * Maneja una excepción sin mostrar mensaje al usuario.
     * Solo registra en logs.
     * 
     * @param exception La excepción a manejar
     * @param context   Contexto donde ocurrió el error
     */
    public static void handleSilent(Exception exception, String context) {
        LoggerUtil.logError("ErrorHandler", context, exception);
    }

    /**
     * Muestra un mensaje de error amigable al usuario.
     * 
     * @param exception La excepción original
     * @param context   Contexto del error
     */
    private static void showUserFriendlyError(Exception exception, String context) {
        String message = getUserMessage(exception);
        String title = "Error: " + context;

        JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Convierte una excepción técnica en un mensaje comprensible para el usuario.
     * 
     * @param exception La excepción a convertir
     * @return Mensaje amigable para el usuario
     */
    private static String getUserMessage(Exception exception) {
        if (exception instanceof IOException) {
            return "Error de conexión. Verifica tu conexión a internet y que el servidor esté disponible.";
//        } else if (exception instanceof JsonSyntaxException) {
//            return "Error al procesar datos del servidor. El formato de respuesta no es válido.";
        } else if (exception instanceof IllegalArgumentException) {
            return "Datos inválidos: " + exception.getMessage();
        } else if (exception instanceof IllegalStateException) {
            return "Operación no permitida en el estado actual: " + exception.getMessage();
        } else if (exception instanceof NullPointerException) {
            return "Error interno: datos no disponibles.";
        }

        // Error genérico
        String message = exception.getMessage();
        return message != null && !message.isEmpty()
                ? "Error: " + message
                : "Error inesperado: " + exception.getClass().getSimpleName();
    }

    /**
     * Muestra un mensaje de advertencia al usuario.
     * 
     * @param message Mensaje de advertencia
     * @param context Contexto de la advertencia
     */
    public static void showWarning(String message, String context) {
        LoggerUtil.logWarning("ErrorHandler", context + ": " + message);
        JOptionPane.showMessageDialog(
                null,
                message,
                "Advertencia: " + context,
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Muestra un mensaje informativo al usuario.
     * 
     * @param message Mensaje informativo
     * @param context Contexto del mensaje
     */
    public static void showInfo(String message, String context) {
        LoggerUtil.logInfo("ErrorHandler", context + ": " + message);
        JOptionPane.showMessageDialog(
                null,
                message,
                context,
                JOptionPane.INFORMATION_MESSAGE);
    }
}
