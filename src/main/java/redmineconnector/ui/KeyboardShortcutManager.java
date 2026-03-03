package redmineconnector.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages keyboard shortcuts for the application.
 * Provides centralized registration and handling of keyboard shortcuts.
 */
public class KeyboardShortcutManager {

    private final JComponent rootComponent;
    private final Map<String, Action> actions = new HashMap<>();

    public KeyboardShortcutManager(JComponent rootComponent) {
        this.rootComponent = rootComponent;
    }

    /**
     * Registers a keyboard shortcut.
     * 
     * @param name      Unique name for the action
     * @param keyStroke KeyStroke for the shortcut
     * @param action    Action to execute
     */
    public void registerShortcut(String name, KeyStroke keyStroke, Action action) {
        InputMap inputMap = rootComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootComponent.getActionMap();

        inputMap.put(keyStroke, name);
        actionMap.put(name, action);
        actions.put(name, action);
    }

    /**
     * Registers a shortcut with a Runnable.
     */
    public void registerShortcut(String name, KeyStroke keyStroke, Runnable runnable) {
        registerShortcut(name, keyStroke, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        });
    }

    /**
     * Unregisters a keyboard shortcut.
     */
    public void unregisterShortcut(String name) {
        InputMap inputMap = rootComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootComponent.getActionMap();

        Action action = actions.get(name);
        if (action != null) {
            // Find and remove the keystroke
            KeyStroke[] keys = inputMap.allKeys();
            if (keys != null) {
                for (KeyStroke key : keys) {
                    if (name.equals(inputMap.get(key))) {
                        inputMap.remove(key);
                        break;
                    }
                }
            }
            actionMap.remove(name);
            actions.remove(name);
        }
    }

    /**
     * Common keyboard shortcuts used throughout the application.
     */
    public static class CommonShortcuts {
        public static final KeyStroke NEW_TASK = KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK);
        public static final KeyStroke REFRESH = KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK);
        public static final KeyStroke REFRESH_F5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
        public static final KeyStroke FIND = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK);
        public static final KeyStroke EDIT = KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK);
        public static final KeyStroke CLONE = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK);
        public static final KeyStroke DELETE = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        public static final KeyStroke INSTANCE_1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK);
        public static final KeyStroke INSTANCE_2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_DOWN_MASK);
        public static final KeyStroke HELP = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
        public static final KeyStroke HELP_ALT = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, KeyEvent.CTRL_DOWN_MASK);
        public static final KeyStroke OPEN = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        public static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        public static final KeyStroke COPY_ID = KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
    }
}
