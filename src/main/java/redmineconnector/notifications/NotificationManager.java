package redmineconnector.notifications;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import redmineconnector.model.Notification;

/**
 * Manages desktop notifications using system tray.
 * Displays notifications for important events like new tasks, updates, etc.
 * Maintains a history of notifications with persistence.
 */
public class NotificationManager {

    private static final String HISTORY_FILE = ".redmine_notifications.dat";
    private static final String NOTIFIED_TASKS_FILE = ".redmine_notified_tasks.dat";
    private static final int MAX_HISTORY = redmineconnector.util.AppConstants.MAX_RECENT_ITEMS * 10; // 100

    private static TrayIcon trayIcon;
    private static boolean initialized = false;
    private static final List<Notification> notificationHistory = new CopyOnWriteArrayList<>();
    private static long nextId = 1;
    private static final List<Consumer<Notification>> listeners = new ArrayList<>();

    // Track which tasks have already been notified to avoid duplicates
    private static final java.util.Set<Integer> notifiedTaskIds = java.util.Collections
            .synchronizedSet(new java.util.HashSet<>());

    /**
     * Initializes the notification system.
     * Must be called before showing notifications.
     * 
     * @param appName Application name to display
     * @param icon    Application icon
     * @return true if initialization was successful
     */
    public static boolean initialize(String appName, Image icon) {
        if (!SystemTray.isSupported()) {
            System.err.println("System tray is not supported on this platform");
            return false;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Create tray icon
            trayIcon = new TrayIcon(icon, appName);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip(appName);

            tray.add(trayIcon);
            initialized = true;
            loadHistory(); // Load history on initialization
            loadNotifiedTasks(); // Load notified tasks tracking
            return true;
        } catch (AWTException e) {
            System.err.println("Failed to initialize system tray: " + e.getMessage());
            return false;
        }
    }

    public static void setPopupMenu(PopupMenu popup) {
        if (trayIcon != null) {
            trayIcon.setPopupMenu(popup);
        }
    }

    public static void addActionListener(java.awt.event.ActionListener listener) {
        if (trayIcon != null) {
            trayIcon.addActionListener(listener);
        }
    }

    /**
     * Shows a notification.
     * 
     * @param title   Notification title
     * @param message Notification message
     * @param type    Type of notification (INFO, WARNING, ERROR)
     */
    public static void showNotification(String title, String message, TrayIcon.MessageType type) {
        if (!initialized || trayIcon == null) {
            System.err.println("NotificationManager not initialized");
            return;
        }

        trayIcon.displayMessage(title, message, type);
    }

    /**
     * Shows an info notification.
     */
    public static void showInfo(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.INFO);
    }

    /**
     * Shows a warning notification.
     */
    public static void showWarning(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.WARNING);
    }

    /**
     * Shows an error notification.
     */
    public static void showError(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.ERROR);
    }

    /**
     * Shows a notification for a new task.
     */
    public static void showNewTaskNotification(int taskId, String subject) {
        showInfo("Nueva Tarea Asignada",
                String.format("#%d: %s", taskId, truncate(subject, 50)));
    }

    /**
     * Shows a notification for a task update.
     */
    public static void showTaskUpdateNotification(int taskId, String subject) {
        showInfo("Tarea Actualizada",
                String.format("#%d: %s", taskId, truncate(subject, 50)));
    }

    /**
     * Shows a notification for a new comment.
     */
    public static void showNewCommentNotification(int taskId, String subject, String author) {
        showInfo("Nuevo Comentario",
                String.format("#%d: %s comentó en \"%s\"", taskId, author, truncate(subject, 40)));
    }

    /**
     * Sets the handler for when a task notification is clicked.
     */

    /**
     * Removes the tray icon and cleans up.
     */
    public static void shutdown() {
        if (trayIcon != null && initialized) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
            initialized = false;
        }
        saveHistory();
    }

    // ===== Notification History Management =====

    /**
     * Adds a notification to history and notifies listeners.
     */
    public static void addNotification(Notification notification) {
        notificationHistory.add(0, notification);
        while (notificationHistory.size() > MAX_HISTORY) {
            notificationHistory.remove(notificationHistory.size() - 1);
        }
        for (Consumer<Notification> listener : listeners) {
            listener.accept(notification);
        }
        saveHistory();
    }

    /**
     * Creates and adds a notification.
     */
    public static void createNotification(Notification.Type type, String message, int taskId) {
        // Check if this task has already been notified
        if (hasBeenNotified(taskId)) {
            return; // Skip duplicate notification
        }

        Notification notification = new Notification(nextId++, type, message, taskId);
        addNotification(notification);
        markAsNotified(taskId); // Mark this task as notified
        showInfo(type.icon + " " + type.label, message);
    }

    public static List<Notification> getAllNotifications() {
        return new ArrayList<>(notificationHistory);
    }

    public static List<Notification> getUnreadNotifications() {
        List<Notification> unread = new ArrayList<>();
        for (Notification n : notificationHistory) {
            if (!n.read)
                unread.add(n);
        }
        return unread;
    }

    public static int getUnreadCount() {
        int count = 0;
        for (Notification n : notificationHistory) {
            if (!n.read)
                count++;
        }
        return count;
    }

    public static void markAsRead(long notificationId) {
        for (Notification n : notificationHistory) {
            if (n.id == notificationId) {
                n.markAsRead();
                saveHistory();
                notifyListeners();
                break;
            }
        }
    }

    public static void markAllAsRead() {
        for (Notification n : notificationHistory) {
            n.markAsRead();
        }
        saveHistory();
        notifyListeners();
    }

    public static void clearAll() {
        notificationHistory.clear();
        saveHistory();
        notifyListeners();
    }

    public static void addNotificationListener(Consumer<Notification> listener) {
        listeners.add(listener);
    }

    private static void notifyListeners() {
        for (Consumer<Notification> listener : listeners) {
            listener.accept(null);
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists())
            return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<Notification> loaded = (List<Notification>) ois.readObject();
            notificationHistory.clear();
            notificationHistory.addAll(loaded);
            for (Notification n : loaded) {
                if (n.id >= nextId)
                    nextId = n.id + 1;
            }
        } catch (Exception e) {
            System.err.println("Failed to load notification history: " + e.getMessage());
        }
    }

    public static void saveHistory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HISTORY_FILE))) {
            oos.writeObject(new ArrayList<>(notificationHistory));
        } catch (Exception e) {
            System.err.println("Failed to save notification history: " + e.getMessage());
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    // ===== Notification Tracking =====

    /**
     * Checks if a task has already been notified.
     */
    public static boolean hasBeenNotified(int taskId) {
        return notifiedTaskIds.contains(taskId);
    }

    /**
     * Marks a task as notified.
     */
    public static void markAsNotified(int taskId) {
        notifiedTaskIds.add(taskId);
        saveNotifiedTasks();
    }

    /**
     * Clears old notified tasks (older than 7 days).
     */
    public static void cleanupNotifiedTasks() {
        // For now, just limit the size
        if (notifiedTaskIds.size() > 1000) {
            // Keep only the most recent 500
            java.util.List<Integer> list = new java.util.ArrayList<>(notifiedTaskIds);
            notifiedTaskIds.clear();
            for (int i = Math.max(0, list.size() - 500); i < list.size(); i++) {
                notifiedTaskIds.add(list.get(i));
            }
            saveNotifiedTasks();
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadNotifiedTasks() {
        File file = new File(NOTIFIED_TASKS_FILE);
        if (!file.exists())
            return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            java.util.Set<Integer> loaded = (java.util.Set<Integer>) ois.readObject();
            notifiedTaskIds.clear();
            notifiedTaskIds.addAll(loaded);
        } catch (Exception e) {
            System.err.println("Failed to load notified tasks: " + e.getMessage());
        }
    }

    private static void saveNotifiedTasks() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(NOTIFIED_TASKS_FILE))) {
            oos.writeObject(new java.util.HashSet<>(notifiedTaskIds));
        } catch (Exception e) {
            System.err.println("Failed to save notified tasks: " + e.getMessage());
        }
    }
}
