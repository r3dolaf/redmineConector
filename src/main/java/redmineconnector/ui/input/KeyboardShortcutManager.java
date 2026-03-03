package redmineconnector.ui.input;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class KeyboardShortcutManager {

    private static final String CONFIG_FILE = "shortcuts.properties";
    private static final Properties props = new Properties();

    static {
        loadConfig();
    }

    private final JComponent targetComponent;
    private final Map<String, Runnable> actions = new HashMap<>();

    public KeyboardShortcutManager(JComponent targetComponent) {
        this.targetComponent = targetComponent;
    }

    public static void loadConfig() {
        File f = new File(CONFIG_FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (Exception ignored) {
            }
        }
    }

    public static void saveConfig() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Keyboard KeyboardShortcutManager Config");
        } catch (Exception ignored) {
        }
    }

    public void registerShortcut(String actionName, String defaultKeyStroke, Runnable action) {
        actions.put(actionName, action);

        String keyStrokeStr = props.getProperty(actionName, defaultKeyStroke);
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeStr);

        if (keyStroke != null) {
            targetComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
            targetComponent.getActionMap().put(actionName, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(action);
                }
            });
        }
    }

    public static void setShortcut(String actionName, String keyStrokeStr) {
        props.setProperty(actionName, keyStrokeStr);
        saveConfig();
    }

    public static String getShortcut(String actionName, String defaultVal) {
        return props.getProperty(actionName, defaultVal);
    }

    public static class CommonShortcuts {
        // Existing shortcuts
        public static final String REFRESH = "control R";
        public static final String NEW_TASK = "control N";
        public static final String DOWNLOAD = "control D";
        public static final String SAVE = "control S";
        public static final String FIND = "control F";
        public static final String OPEN = "ENTER";
        public static final String COPY_ID = "control shift C";
        public static final String HELP = "F1";

        // Navigation shortcuts
        public static final String NEXT_TASK = "J";
        public static final String PREV_TASK = "K";

        // Action shortcuts
        public static final String EDIT_TASK = "E";
        public static final String COMMENT_TASK = "C";
        public static final String ASSIGN_TASK = "A";
        public static final String FOCUS_SEARCH = "SLASH";

        // Quick filters
        public static final String SHOW_MY_TASKS = "M";
        public static final String TOGGLE_CLOSED = "X";

        // QuickView shortcuts
        public static final String TOGGLE_QUICKVIEW = "Q";
        public static final String NEXT_TAB_QUICKVIEW = "E";
        public static final String PREV_TAB_QUICKVIEW = "W";
    }
}
