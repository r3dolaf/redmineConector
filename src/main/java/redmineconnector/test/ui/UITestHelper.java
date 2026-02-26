package redmineconnector.test.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple UI testing framework for Swing components.
 * Provides basic assertions and component finding utilities.
 * 
 * No external dependencies - uses only Java Swing APIs.
 */
public class UITestHelper {

    /**
     * Find a component by its name in a container hierarchy.
     */
    public static Component findComponentByName(Container container, String name) {
        if (container.getName() != null && container.getName().equals(name)) {
            return container;
        }

        for (Component comp : container.getComponents()) {
            if (comp.getName() != null && comp.getName().equals(name)) {
                return comp;
            }
            if (comp instanceof Container) {
                Component found = findComponentByName((Container) comp, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Find all components of a specific type.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Component> List<T> findComponentsByType(Container container, Class<T> type) {
        List<T> found = new ArrayList<>();
        findComponentsByTypeRecursive(container, type, found);
        return found;
    }

    private static <T extends Component> void findComponentsByTypeRecursive(Container container, Class<T> type,
            List<T> found) {
        if (type.isInstance(container)) {
            found.add((T) container);
        }

        for (Component comp : container.getComponents()) {
            if (type.isInstance(comp)) {
                found.add((T) comp);
            }
            if (comp instanceof Container) {
                findComponentsByTypeRecursive((Container) comp, type, found);
            }
        }
    }

    /**
     * Find a button by its text.
     */
    public static JButton findButtonByText(Container container, String text) {
        List<JButton> buttons = findComponentsByType(container, JButton.class);
        for (JButton button : buttons) {
            if (text.equals(button.getText())) {
                return button;
            }
        }
        return null;
    }

    /**
     * Find a label by its text.
     */
    public static JLabel findLabelByText(Container container, String text) {
        List<JLabel> labels = findComponentsByType(container, JLabel.class);
        for (JLabel label : labels) {
            if (text.equals(label.getText())) {
                return label;
            }
        }
        return null;
    }

    /**
     * Simulate a button click.
     */
    public static void clickButton(JButton button) {
        if (button == null) {
            throw new IllegalArgumentException("Button is null");
        }
        if (!button.isEnabled()) {
            throw new IllegalStateException("Button is disabled: " + button.getText());
        }

        // Simulate click on EDT
        SwingUtilities.invokeLater(() -> button.doClick());

        // Wait for EDT to process
        waitForEDT();
    }

    /**
     * Set text in a text field.
     */
    public static void setText(JTextField field, String text) {
        if (field == null) {
            throw new IllegalArgumentException("TextField is null");
        }

        SwingUtilities.invokeLater(() -> {
            field.setText(text);
            field.postActionEvent(); // Trigger action listeners
        });

        waitForEDT();
    }

    /**
     * Select an item in a combo box.
     */
    public static void selectComboBoxItem(JComboBox<?> comboBox, String item) {
        if (comboBox == null) {
            throw new IllegalArgumentException("ComboBox is null");
        }

        SwingUtilities.invokeLater(() -> comboBox.setSelectedItem(item));
        waitForEDT();
    }

    /**
     * Wait for the Event Dispatch Thread to process all pending events.
     */
    public static void waitForEDT() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                // Just waiting for EDT to be free
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to wait for EDT", e);
        }
    }

    /**
     * Wait for a specified duration.
     */
    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if a component is visible.
     */
    public static boolean isVisible(Component component) {
        return component != null && component.isVisible() && component.isShowing();
    }

    /**
     * Check if a component is enabled.
     */
    public static boolean isEnabled(Component component) {
        return component != null && component.isEnabled();
    }

    /**
     * Get the count of components of a specific type.
     */
    public static <T extends Component> int countComponents(Container container, Class<T> type) {
        return findComponentsByType(container, type).size();
    }
}
