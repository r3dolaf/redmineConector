package redmineconnector.test.ui;

import redmineconnector.test.SimpleTestRunner;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Advanced UI tests for tables, lists, and event handling.
 */
public class TableAndListTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("JTable - Row selection", () -> {
            String[] columns = { "ID", "Name" };
            Object[][] data = { { 1, "Item 1" }, { 2, "Item 2" }, { 3, "Item 3" } };

            JTable table = new JTable(data, columns);

            table.setRowSelectionAllowed(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowSelectionInterval(1, 1); // Select row 2

            SimpleTestRunner.assertTrue(table.getSelectedRow() == 1, "Row 1 should be selected");
            SimpleTestRunner.assertTrue(table.getSelectedRowCount() == 1, "Should have 1 row selected");
        });

        runner.run("JTable - Cell value access", () -> {
            DefaultTableModel model = new DefaultTableModel();
            model.addColumn("ID");
            model.addColumn("Status");

            model.addRow(new Object[] { 100, "New" });
            model.addRow(new Object[] { 200, "In Progress" });

            JTable table = new JTable(model);

            SimpleTestRunner.assertTrue((Integer) table.getValueAt(0, 0) == 100, "First cell should be 100");
            SimpleTestRunner.assertTrue("In Progress".equals(table.getValueAt(1, 1)), "Status should match");
        });

        runner.run("JTable - Model manipulation", () -> {
            DefaultTableModel model = new DefaultTableModel(new String[] { "Col1", "Col2" }, 0);
            JTable table = new JTable(model);

            model.addRow(new Object[] { "A", "B" });
            model.addRow(new Object[] { "C", "D" });

            SimpleTestRunner.assertTrue(model.getRowCount() == 2, "Should have 2 rows");

            model.removeRow(0);

            SimpleTestRunner.assertTrue(model.getRowCount() == 1, "Should have 1 row after removal");
            SimpleTestRunner.assertTrue("C".equals(model.getValueAt(0, 0)), "First row should now be C");
        });

        runner.run("JList - Item selection", () -> {
            DefaultListModel<String> model = new DefaultListModel<>();
            model.addElement("Option 1");
            model.addElement("Option 2");
            model.addElement("Option 3");

            JList<String> list = new JList<>(model);
            list.setSelectedIndex(1);

            SimpleTestRunner.assertTrue(list.getSelectedIndex() == 1, "Index 1 should be selected");
            SimpleTestRunner.assertTrue("Option 2".equals(list.getSelectedValue()),
                    "Selected value should be Option 2");
        });

        runner.run("JList - Multiple selection", () -> {
            String[] items = { "A", "B", "C", "D" };
            JList<String> list = new JList<>(items);

            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.setSelectedIndices(new int[] { 0, 2 }); // Select A and C

            SimpleTestRunner.assertTrue(list.getSelectedIndices().length == 2, "Should have 2 selected");
            SimpleTestRunner.assertTrue("A".equals(list.getSelectedValuesList().get(0)), "First should be A");
        });

        runner.run("JTextArea - Multi-line text", () -> {
            JTextArea textArea = new JTextArea();
            textArea.setText("Line 1\nLine 2\nLine 3");

            SimpleTestRunner.assertTrue(textArea.getLineCount() == 3, "Should have 3 lines");
            SimpleTestRunner.assertTrue(textArea.getText().contains("Line 2"), "Should contain Line 2");
        });

        runner.run("JTextArea - Append and clear", () -> {
            JTextArea textArea = new JTextArea("Initial");

            textArea.append("\nAppended");
            SimpleTestRunner.assertTrue(textArea.getText().contains("Appended"), "Should contain appended text");

            textArea.setText("");
            SimpleTestRunner.assertTrue(textArea.getText().isEmpty(), "Should be empty after clear");
        });

        runner.run("JProgressBar - Value setting", () -> {
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setValue(50);

            SimpleTestRunner.assertTrue(progressBar.getValue() == 50, "Value should be 50");
            SimpleTestRunner.assertTrue(progressBar.getPercentComplete() == 0.5, "Should be 50%");
        });

        runner.run("JSlider - Range and value", () -> {
            JSlider slider = new JSlider(0, 100, 25);

            SimpleTestRunner.assertTrue(slider.getMinimum() == 0, "Min should be 0");
            SimpleTestRunner.assertTrue(slider.getMaximum() == 100, "Max should be 100");
            SimpleTestRunner.assertTrue(slider.getValue() == 25, "Value should be 25");

            slider.setValue(75);
            SimpleTestRunner.assertTrue(slider.getValue() == 75, "Value should be updated to 75");
        });

        runner.run("Action - Button action event", () -> {
            final int[] clickCount = { 0 };

            Action testAction = new AbstractAction("Test Action") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clickCount[0]++;
                }
            };

            JButton button = new JButton(testAction);

            // Simulate action
            testAction.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "test"));

            SimpleTestRunner.assertTrue(clickCount[0] == 1, "Action should be triggered once");
            SimpleTestRunner.assertTrue("Test Action".equals(button.getText()), "Button text should match action");
        });

        runner.run("JMenuBar - Menu structure", () -> {
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenuItem openItem = new JMenuItem("Open");
            JMenuItem saveItem = new JMenuItem("Save");

            fileMenu.add(openItem);
            fileMenu.add(saveItem);
            menuBar.add(fileMenu);

            SimpleTestRunner.assertTrue(menuBar.getMenuCount() == 1, "Should have 1 menu");
            SimpleTestRunner.assertTrue(fileMenu.getItemCount() == 2, "File menu should have 2 items");
            SimpleTestRunner.assertTrue("Open".equals(fileMenu.getItem(0).getText()), "First item should be Open");
        });

        runner.run("JToolBar - Tool addition", () -> {
            JToolBar toolBar = new JToolBar();
            JButton tool1 = new JButton("Tool 1");
            JButton tool2 = new JButton("Tool 2");

            toolBar.add(tool1);
            toolBar.addSeparator();
            toolBar.add(tool2);

            SimpleTestRunner.assertTrue(toolBar.getComponentCount() == 3,
                    "Should have 3 components (2 buttons + separator)");
        });

        runner.run("CardLayout - Card switching", () -> {
            JPanel cardPanel = new JPanel();
            CardLayout cardLayout = new CardLayout();
            cardPanel.setLayout(cardLayout);

            JPanel card1 = new JPanel();
            card1.setName("Card1");
            JPanel card2 = new JPanel();
            card2.setName("Card2");

            cardPanel.add(card1, "first");
            cardPanel.add(card2, "second");

            // CardLayout doesn't expose current card directly, but we can verify structure
            SimpleTestRunner.assertTrue(cardPanel.getComponentCount() == 2, "Should have 2 cards");
            SimpleTestRunner.assertTrue(cardPanel.getLayout() instanceof CardLayout, "Should use CardLayout");
        });

        runner.run("GridLayout - Grid structure", () -> {
            JPanel panel = new JPanel(new GridLayout(2, 3)); // 2 rows, 3 columns

            for (int i = 0; i < 6; i++) {
                panel.add(new JButton("Button " + i));
            }

            SimpleTestRunner.assertTrue(panel.getComponentCount() == 6, "Should have 6 components");

            GridLayout layout = (GridLayout) panel.getLayout();
            SimpleTestRunner.assertTrue(layout.getRows() == 2, "Should have 2 rows");
            SimpleTestRunner.assertTrue(layout.getColumns() == 3, "Should have 3 columns");
        });

        runner.run("BoxLayout - Component alignment", () -> {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            panel.add(new JButton("Top"));
            panel.add(Box.createVerticalGlue());
            panel.add(new JButton("Bottom"));

            SimpleTestRunner.assertTrue(panel.getComponentCount() == 3, "Should have 3 components");
            SimpleTestRunner.assertTrue(panel.getLayout() instanceof BoxLayout, "Should use BoxLayout");
        });
    }
}
