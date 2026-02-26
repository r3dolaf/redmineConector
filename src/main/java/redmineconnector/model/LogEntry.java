package redmineconnector.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    public final LocalDateTime timestamp;
    public final Level level;
    public final String message;
    public final String source;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public LogEntry(Level level, String message) {
        this(level, message, "System");
    }

    public LogEntry(Level level, String message, String source) {
        this.timestamp = LocalDateTime.now();
        this.level = level;
        this.message = message;
        this.source = source != null ? source : "System";
    }

    public String getFormattedTime() {
        return timestamp.format(TIME_FMT);
    }

    @Override
    public String toString() {
        if ("System".equalsIgnoreCase(source)) {
            return String.format("[%s] [%s] %s", getFormattedTime(), level, message);
        }
        return String.format("[%s] [%s] [%s] %s", getFormattedTime(), level, source, message);
    }
}
