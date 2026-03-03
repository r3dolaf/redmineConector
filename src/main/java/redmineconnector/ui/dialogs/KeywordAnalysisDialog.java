package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import redmineconnector.model.Journal;
import redmineconnector.model.Task;
import redmineconnector.ui.InstanceController;
import redmineconnector.ui.UIHelper;
import redmineconnector.util.I18n;

public class KeywordAnalysisDialog extends JDialog {
    private final DefaultTableModel tableModel;
    private final JTextArea inputWords;
    private final List<Task> tasks;
    private final InstanceController controller;

    public KeywordAnalysisDialog(Window owner, String title, List<Task> tasks, InstanceController controller) {
        super(owner, I18n.format("keyword.dialog.title", title), ModalityType.MODELESS);
        this.tasks = tasks;
        this.controller = controller;
        setSize(650, 700);
        setLocationRelativeTo(owner);
        UIHelper.addEscapeListener(this);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.add(new JLabel(
                I18n.get("keyword.label.instruction")),
                BorderLayout.NORTH);
        inputWords = new JTextArea(5, 50);
        topPanel.add(new JScrollPane(inputWords), BorderLayout.CENTER);
        JButton btnAnalyze = new JButton(I18n.get("keyword.btn.analyze"));
        btnAnalyze.addActionListener(e -> analyze());
        topPanel.add(btnAnalyze, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        String[] cols = {
                I18n.get("keyword.col.use"),
                I18n.get("keyword.col.keyword"),
                I18n.get("keyword.col.matches"),
                I18n.get("keyword.col.examples") };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
        };
        JTable table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(350);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnApply = new JButton(I18n.get("keyword.btn.apply"));
        btnApply.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnApply.setBackground(new Color(220, 240, 255));
        btnApply.addActionListener(e -> {
            List<String> selectedWords = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
                if (Boolean.TRUE.equals(isSelected)) {
                    selectedWords.add(tableModel.getValueAt(i, 1).toString());
                }
            }
            if (selectedWords.isEmpty()) {
                JOptionPane.showMessageDialog(this, I18n.get("keyword.msg.select"));
            } else {
                String regex = String.join("|", selectedWords);
                controller.performKeywordSearch(regex);
                dispose();
            }
        });
        bottomPanel.add(btnApply);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private String normalize(String input) {
        if (input == null)
            return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }

    private void analyze() {
        tableModel.setRowCount(0);
        String text = inputWords.getText().trim();
        if (text.isEmpty())
            return;
        String[] rawWords = text.split("[\\n,;]+");
        Map<String, String> wordMapping = new LinkedHashMap<>();
        for (String w : rawWords) {
            String trim = w.trim();
            if (!trim.isEmpty()) {
                wordMapping.put(trim, normalize(trim));
            }
        }
        Map<String, Set<Integer>> hits = new LinkedHashMap<>();
        for (String orig : wordMapping.keySet()) {
            hits.put(orig, new HashSet<>());
        }
        for (Task t : tasks) {
            StringBuilder sb = new StringBuilder();
            sb.append(t.subject != null ? t.subject : "").append(" ");
            sb.append(t.description != null ? t.description : "").append(" ");
            if (t.journals != null) {
                for (Journal j : t.journals) {
                    sb.append(j.notes != null ? j.notes : "").append(" ");
                }
            }
            String fullContentNorm = normalize(sb.toString());
            for (Map.Entry<String, String> entry : wordMapping.entrySet()) {
                String original = entry.getKey();
                String normalizedTarget = entry.getValue();
                if (fullContentNorm.contains(normalizedTarget)) {
                    hits.get(original).add(t.id);
                }
            }
        }
        hits.forEach((word, ids) -> {
            if (!ids.isEmpty()) {
                List<Integer> sortedIds = new ArrayList<>(ids);
                Collections.sort(sortedIds);
                String idStr = sortedIds.stream().limit(10).map(String::valueOf).collect(Collectors.joining(", "));
                if (sortedIds.size() > 10)
                    idStr += "...";
                tableModel.addRow(new Object[] { true, word, sortedIds.size(), idStr });
            }
        });
    }
}
