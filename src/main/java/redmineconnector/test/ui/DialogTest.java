package redmineconnector.test.ui;

import redmineconnector.test.SimpleTestRunner;

import javax.swing.*;
import java.awt.*;

/**
 * UI tests for dialog components.
 * Tests basic dialog functionality without requiring specific dialog classes.
 */
public class DialogTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("JDialog - Creation and basic properties", () -> {
            JFrame parentFrame = new JFrame();

            try {
                JDialog dialog = new JDialog(parentFrame, "Test Dialog", true);

                SimpleTestRunner.assertNotNull(dialog, "Dialog should be created");
                SimpleTestRunner.assertTrue("Test Dialog".equals(dialog.getTitle()), "Title should match");
                SimpleTestRunner.assertTrue(dialog.isModal(), "Should be modal");

            } finally {
                parentFrame.dispose();
            }
        });

        runner.run("JDialog - Component addition", () -> {
            JFrame parentFrame = new JFrame();

            try {
                JDialog dialog = new JDialog(parentFrame);
                JPanel panel = new JPanel();
                JButton okButton = new JButton("OK");
                JButton cancelButton = new JButton("Cancel");

                panel.add(okButton);
                panel.add(cancelButton);
                dialog.setContentPane(panel);

                // Count only direct children of our panel, not Swing internals
                int buttonCount = 0;
                for (Component comp : panel.getComponents()) {
                    if (comp instanceof JButton)
                        buttonCount++;
                }

                SimpleTestRunner.assertTrue(buttonCount == 2, "Should have 2 buttons, got " + buttonCount);

            } finally {
                parentFrame.dispose();
            }
        });

        runner.run("JDialog - Layout management", () -> {
            JFrame parentFrame = new JFrame();

            try {
                JDialog dialog = new JDialog(parentFrame);
                JPanel panel = new JPanel(new BorderLayout());

                panel.add(new JLabel("Title"), BorderLayout.NORTH);
                panel.add(new JTextArea("Content"), BorderLayout.CENTER);
                panel.add(new JButton("OK"), BorderLayout.SOUTH);

                dialog.setContentPane(panel);

                SimpleTestRunner.assertTrue(panel.getLayout() instanceof BorderLayout, "Should have BorderLayout");
                SimpleTestRunner.assertTrue(panel.getComponentCount() == 3, "Should have 3 components");

            } finally {
                parentFrame.dispose();
            }
        });

        runner.run("JOptionPane - Message dialog", () -> {
            // Test JOptionPane components
            JOptionPane pane = new JOptionPane("Test Message", JOptionPane.INFORMATION_MESSAGE);

            SimpleTestRunner.assertNotNull(pane, "OptionPane should be created");
            SimpleTestRunner.assertTrue("Test Message".equals(pane.getMessage()), "Message should match");
        });

        runner.run("Dialog - Component hierarchy navigation", () -> {
            JFrame parentFrame = new JFrame();

            try {
                JDialog dialog = new JDialog(parentFrame);
                JPanel panel = new JPanel();
                JButton button = new JButton("Test");
                button.setName("testButton");

                panel.add(button);
                dialog.setContentPane(panel);

                Component found = UITestHelper.findComponentByName(dialog, "testButton");
                SimpleTestRunner.assertNotNull(found, "Should find component by name");
                SimpleTestRunner.assertTrue(found instanceof JButton, "Found component should be JButton");

            } finally {
                parentFrame.dispose();
            }
        });
    }
}
