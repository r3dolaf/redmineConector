package redmineconnector.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a notification in the system.
 * Notifications can be triggered by various events like new tasks, assignments,
 * comments, etc.
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        NEW_TASK("Nueva Tarea", "🆕"),
        ASSIGNMENT("Asignación", "👤"),
        COMMENT("Comentario", "💬"),
        DUE_DATE("Fecha Límite", "⏰"),
        STATUS_CHANGE("Cambio Estado", "🔄"),
        UPDATE("Actualización", "📝");

        public final String label;
        public final String icon;

        Type(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    public final long id;
    public final Type type;
    public final String message;
    public final Date timestamp;
    public final int taskId;
    public boolean read;

    public Notification(long id, Type type, String message, int taskId) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.taskId = taskId;
        this.timestamp = new Date();
        this.read = false;
    }

    public void markAsRead() {
        this.read = true;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (Task #%d)",
                type.icon, type.label, message, taskId);
    }
}
