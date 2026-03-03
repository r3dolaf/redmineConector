package redmineconnector.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilidad centralizada para logging en la aplicación.
 * 
 * <p>
 * Proporciona métodos para registrar mensajes con diferentes niveles de
 * severidad
 * (INFO, WARNING, ERROR, DEBUG) con formato consistente.
 * </p>
 * 
 * <p>
 * Formato de log: [NIVEL] [TIMESTAMP] Fuente: Mensaje
 * </p>
 * 
 * @author Redmine Connector Team
 * @version 8.5.5
 */
public class LoggerUtil {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static boolean debugEnabled = false;

    /**
     * Habilita o deshabilita los logs de nivel DEBUG.
     * 
     * @param enabled true para habilitar DEBUG, false para deshabilitar
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    /**
     * Verifica si el nivel DEBUG está habilitado.
     * 
     * @return true si DEBUG está habilitado
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Registra un mensaje de nivel INFO.
     * 
     * @param source  Fuente del mensaje (clase o componente)
     * @param message Mensaje a registrar
     */
    public static void logInfo(String source, String message) {
        log("INFO", source, message);
    }

    /**
     * Registra un mensaje de nivel WARNING.
     * 
     * @param source  Fuente del mensaje
     * @param message Mensaje a registrar
     */
    public static void logWarning(String source, String message) {
        log("WARNING", source, message);
    }

    /**
     * Registra un mensaje de nivel ERROR con excepción.
     * 
     * @param source    Fuente del error
     * @param message   Mensaje descriptivo
     * @param exception Excepción asociada
     */
    public static void logError(String source, String message, Exception exception) {
        log("ERROR", source, message);
        if (exception != null) {
            exception.printStackTrace(System.err);
        }
    }

    /**
     * Registra un mensaje de nivel ERROR sin excepción.
     * 
     * @param source  Fuente del error
     * @param message Mensaje descriptivo
     */
    public static void logError(String source, String message) {
        logError(source, message, null);
    }

    /**
     * Registra un mensaje de nivel DEBUG.
     * Solo se muestra si DEBUG está habilitado.
     * 
     * @param source  Fuente del mensaje
     * @param message Mensaje a registrar
     */
    public static void logDebug(String source, String message) {
        if (debugEnabled) {
            log("DEBUG", source, message);
        }
    }

    /**
     * Método interno para formatear y escribir logs.
     * 
     * @param level   Nivel de severidad
     * @param source  Fuente del mensaje
     * @param message Mensaje
     */
    private static void log(String level, String source, String message) {
        String timestamp = TIME_FORMAT.format(new Date());
        String formattedMessage = String.format("[%s] [%s] %s: %s",
                level, timestamp, source, message);

        if ("ERROR".equals(level)) {
            System.err.println(formattedMessage);
        } else {
            System.out.println(formattedMessage);
        }
    }

    /**
     * Registra el inicio de una operación.
     * 
     * @param source    Fuente de la operación
     * @param operation Nombre de la operación
     */
    public static void logOperationStart(String source, String operation) {
        logDebug(source, "Iniciando: " + operation);
    }

    /**
     * Registra la finalización exitosa de una operación.
     * 
     * @param source    Fuente de la operación
     * @param operation Nombre de la operación
     */
    public static void logOperationSuccess(String source, String operation) {
        logDebug(source, "Completado: " + operation);
    }

    /**
     * Registra la finalización fallida de una operación.
     * 
     * @param source    Fuente de la operación
     * @param operation Nombre de la operación
     * @param exception Excepción que causó el fallo
     */
    public static void logOperationFailure(String source, String operation, Exception exception) {
        logError(source, "Falló: " + operation, exception);
    }
}
