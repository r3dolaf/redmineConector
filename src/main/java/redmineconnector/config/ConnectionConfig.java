package redmineconnector.config;

import java.util.Properties;
import java.util.regex.Pattern;

public class ConnectionConfig {
    public String prefix, url, apiKey, projectId;
    public int limit, refreshInterval;
    public boolean notifyNewTasks;
    public boolean notifyWarnings;
    public boolean notifyConfirmations;
    public boolean notifyErrors;
    public boolean isDetached;

    // Restored fields
    public boolean showNotifications;
    public boolean showClosed;
    public boolean includeEpics;
    public String refPattern;
    public String downloadPath;
    public String clientName;
    public String userEmail;
    public String folderPattern;
    public String attachmentFormat;
    public String columnWidths;
    public String columnVisibility;
    public String pinnedTaskIds;

    public ConnectionConfig(String prefix, Properties props) {
        this.prefix = prefix;
        this.url = props.getProperty(prefix + ".url", "https://redmine.ejemplo.com/");
        this.apiKey = props.getProperty(prefix + ".key", "");
        this.projectId = props.getProperty(prefix + ".project", "");
        this.limit = parseSafeInt(props.getProperty(prefix + ".limit"), 0);
        this.refreshInterval = parseSafeInt(props.getProperty(prefix + ".refresh"), 5);
        this.showNotifications = "true".equalsIgnoreCase(props.getProperty(prefix + ".notifications", "true"));

        // Granular notifications (default to true)
        this.notifyNewTasks = "true".equalsIgnoreCase(props.getProperty(prefix + ".notify.new", "true"));
        this.notifyWarnings = "true".equalsIgnoreCase(props.getProperty(prefix + ".notify.warn", "true"));
        this.notifyConfirmations = "true".equalsIgnoreCase(props.getProperty(prefix + ".notify.conf", "true"));
        this.notifyErrors = "true".equalsIgnoreCase(props.getProperty(prefix + ".notify.err", "true"));

        this.showClosed = "true".equalsIgnoreCase(props.getProperty(prefix + ".closed", "false"));
        this.includeEpics = "true".equalsIgnoreCase(props.getProperty(prefix + ".epics", "true"));
        this.refPattern = props.getProperty(prefix + ".pattern", "[Ref #{id}]");
        this.downloadPath = props.getProperty(prefix + ".downloadPath", "");
        this.clientName = props.getProperty(prefix + ".clientName", "Cliente");
        this.userEmail = props.getProperty(prefix + ".userEmail", "");
        this.folderPattern = props.getProperty(prefix + ".folderPattern", "{id}_{subject}");
        this.attachmentFormat = props.getProperty(prefix + ".attachmentFormat", "textile");
        this.columnWidths = props.getProperty(prefix + ".columnWidths", "");
        this.columnVisibility = props.getProperty(prefix + ".columnVisibility", "");
        this.pinnedTaskIds = props.getProperty(prefix + ".pinnedTaskIds", "");
        this.isDetached = "true".equalsIgnoreCase(props.getProperty(prefix + ".isDetached", "false"));
    }

    private int parseSafeInt(String val, int def) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    public String formatReference(int id) {
        if (refPattern == null || refPattern.isEmpty())
            return " #" + id;
        return refPattern.replace("{id}", String.valueOf(id));
    }

    public Pattern getExtractionPattern() {
        if (refPattern == null || refPattern.trim().isEmpty())
            return null;
        String safeRegex = Pattern.quote(refPattern).replace("{id}", "\\E(\\d+)\\Q");
        return Pattern.compile(safeRegex);
    }
}
