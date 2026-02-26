package redmineconnector.test.ui;

import redmineconnector.test.SimpleTestRunner;
import redmineconnector.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * UI tests for panel components and custom controls.
 */
public class ComponentTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("JPanel - Basic creation and configuration", () -> {
            JPanel panel = new JPanel();
            panel.setName("testPanel");
            panel.setLayout(new BorderLayout());

            SimpleTestRunner.assertNotNull(panel, "Panel should be created");
            SimpleTestRunner.assertTrue("testPanel".equals(panel.getName()), "Panel name should be set");
            SimpleTestRunner.assertTrue(panel.getLayout() instanceof BorderLayout, "Layout should be BorderLayout");
        });

        runner.run("JButton - Creation with action", () -> {
            final boolean[] clicked = { false };

            JButton button = new JButton("Click Me");
            button.addActionListener(e -> clicked[0] = true);

            UITestHelper.clickButton(button);
            UITestHelper.sleep(100); // Wait for action to execute

            SimpleTestRunner.assertTrue(clicked[0], "Button action should execute");
        });

        runner.run("JTextField - Text input and retrieval", () -> {
            JTextField textField = new JTextField();

            UITestHelper.setText(textField, "Test Input");
            UITestHelper.sleep(50);

            SimpleTestRunner.assertTrue("Test Input".equals(textField.getText()), "Text should be set correctly");
        });

        runner.run("JComboBox - Item selection", () -> {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Item 1");
            model.addElement("Item 2");
            model.addElement("Item 3");

            JComboBox<String> comboBox = new JComboBox<>(model);

            UITestHelper.selectComboBoxItem(comboBox, "Item 2");
            UITestHelper.sleep(50);

            SimpleTestRunner.assertTrue("Item 2".equals(comboBox.getSelectedItem()), "Item 2 should be selected");
        });

        runner.run("JCheckBox - State toggling", () -> {
            JCheckBox checkBox = new JCheckBox("Test Option");

            SimpleTestRunner.assertTrue(!checkBox.isSelected(), "Should start unchecked");

            SwingUtilities.invokeLater(() -> checkBox.setSelected(true));
            UITestHelper.waitForEDT();

            SimpleTestRunner.assertTrue(checkBox.isSelected(), "Should be checked after toggle");
        });

        runner.run("JLabel - Text and icon support", () -> {
            JLabel label = new JLabel("Test Label");
            label.setName("testLabel");

            SimpleTestRunner.assertTrue("Test Label".equals(label.getText()), "Label text should match");
            SimpleTestRunner.assertTrue("testLabel".equals(label.getName()), "Label name should match");
        });

        runner.run("JTable - Basic table creation", () -> {
            String[] columnNames = { "ID", "Subject", "Status" };
            Object[][] data = {
                    { 1, "Task 1", "New" },
                    { 2, "Task 2", "In Progress" }
            };

            JTable table = new JTable(data, columnNames);

            SimpleTestRunner.assertTrue(table.getRowCount() == 2, "Should have 2 rows");
            SimpleTestRunner.assertTrue(table.getColumnCount() == 3, "Should have 3 columns");
            SimpleTestRunner.assertTrue("Task 1".equals(table.getValueAt(0, 1)), "First row subject should match");
        });

        runner.run("JScrollPane - Viewport component", () -> {
            JTextArea textArea = new JTextArea("Test content");
            JScrollPane scrollPane = new JScrollPane(textArea);

            Component viewport = scrollPane.getViewport().getView();
            SimpleTestRunner.assertTrue(viewport instanceof JTextArea, "Viewport should contain JTextArea");
        });

        runner.run("JSplitPane - Divider location", () -> {
            JPanel leftPanel = new JPanel();
            JPanel rightPanel = new JPanel();

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
            splitPane.setDividerLocation(200);

            SimpleTestRunner.assertTrue(splitPane.getDividerLocation() == 200, "Divider should be at 200");
        });

        runner.run("JTabbedPane - Tab management", () -> {
            JTabbedPane tabbedPane = new JTabbedPane();

            tabbedPane.addTab("Tab 1", new JPanel());
            tabbedPane.addTab("Tab 2", new JPanel());
            tabbedPane.addTab("Tab 3", new JPanel());

            SimpleTestRunner.assertTrue(tabbedPane.getTabCount() == 3, "Should have 3 tabs");

            tabbedPane.setSelectedIndex(1);
            SimpleTestRunner.assertTrue(tabbedPane.getSelectedIndex() == 1, "Tab 2 should be selected");
        });

        runner.run("Color and Font - Component styling", () -> {
            JLabel label = new JLabel("Styled");
            label.setForeground(Color.RED);
            label.setBackground(Color.WHITE);

            SimpleTestRunner.assertTrue(Color.RED.equals(label.getForeground()), "Foreground should be red");
            SimpleTestRunner.assertTrue(Color.WHITE.equals(label.getBackground()), "Background should be white");
        });

        runner.run("Component - Visibility and enabling", () -> {
            JButton button = new JButton("Test");

            button.setVisible(false);
            SimpleTestRunner.assertTrue(!UITestHelper.isVisible(button), "Should not be visible");

            button.setVisible(true);
            button.setEnabled(false);
            SimpleTestRunner.assertTrue(!UITestHelper.isEnabled(button), "Should not be enabled");
        });

        runner.run("Theme - Basic theme application", () -> {
            Theme theme = Theme.LIGHT;

            SimpleTestRunner.assertNotNull(theme, "Theme should exist");
            SimpleTestRunner.assertNotNull(theme.getBackground(), "Theme should have background color");
            SimpleTestRunner.assertNotNull(theme.getText(), "Theme should have text color");
        });

        runner.run("UITestHelper - Component counting", () -> {
            JPanel panel = new JPanel();
            JButton button1 = new JButton("Button 1");
            JButton button2 = new JButton("Button 2");
            JLabel label = new JLabel("Label");

            panel.add(button1);
            panel.add(button2);
            panel.add(label);

            // Count direct children only
            int buttonCount = 0;
            for (Component comp : panel.getComponents()) {
                if (comp instanceof JButton)
                    buttonCount++;
            }

            int labelCount = 0;
            for (Component comp : panel.getComponents()) {
                if (comp instanceof JLabel)
                    labelCount++;
            }

            SimpleTestRunner.assertTrue(buttonCount == 2, "Should count 2 buttons, got " + buttonCount);
            SimpleTestRunner.assertTrue(labelCount == 1, "Should count 1 label, got " + labelCount);
        });

        runner.run("UITestHelper - Finding components by type", () -> {
            JPanel mainPanel = new JPanel();
            JPanel subPanel = new JPanel();

            JButton nestedButton = new JButton("Nested Button");
            JButton mainButton = new JButton("Main Button");

            subPanel.add(nestedButton);
            mainPanel.add(subPanel);
            mainPanel.add(mainButton);

            // UITestHelper.findComponentsByType is recursive, so it finds all buttons
            // including those in nested panels. This is expected behavior.
            java.util.List<JButton> buttons = UITestHelper.findComponentsByType(mainPanel, JButton.class);

            // Verify at least our 2 buttons are found (there may be more from Swing
            // internals)
            SimpleTestRunner.assertTrue(buttons.size() >= 2,
                    "Should find at least 2 buttons, found " + buttons.size());

            // Verify both our buttons are in the list
            boolean foundNested = false;
            boolean foundMain = false;
            for (JButton btn : buttons) {
                if (btn == nestedButton)
                    foundNested = true;
                if (btn == mainButton)
                    foundMain = true;
            }

            SimpleTestRunner.assertTrue(foundNested, "Should find nested button");
            SimpleTestRunner.assertTrue(foundMain, "Should find main button");
        });
    }
}
