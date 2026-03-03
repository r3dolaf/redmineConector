package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import redmineconnector.model.Task;
import redmineconnector.ui.UIHelper;
import redmineconnector.util.I18n;

public class DiffDialog extends JDialog {
    public DiffDialog(Window owner, Task local, Task remote, String nameLocal, String nameRemote) {
        super(owner, I18n.get("diff.dialog.title"), ModalityType.MODELESS);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        UIHelper.addEscapeListener(this);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 5, 2, 5);
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0.2;
        g.weightx = 0.2;
        mainPanel.add(new JLabel(I18n.get("diff.col.field")), g);
        g.gridx = 1;
        g.weightx = 0.4;
        mainPanel.add(new JLabel(I18n.format("diff.col.local", nameLocal)), g);
        g.gridx = 2;
        g.weightx = 0.4;
        mainPanel.add(new JLabel(I18n.format("diff.col.remote", nameRemote)), g);
        g.gridy++;
        g.gridwidth = 3;
        mainPanel.add(new JSeparator(), g);
        g.gridwidth = 1;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        addRow(mainPanel, I18n.get("diff.field.id"), String.valueOf(local.id), String.valueOf(remote.id), g);
        addRow(mainPanel, I18n.get("diff.field.subject"), local.subject, remote.subject, g);
        addRow(mainPanel, I18n.get("diff.field.status"), local.status, remote.status, g);
        addRow(mainPanel, I18n.get("diff.field.priority"), local.priority, remote.priority, g);
        addRow(mainPanel, I18n.get("diff.field.tracker"), local.tracker, remote.tracker, g);
        addRow(mainPanel, I18n.get("diff.field.assigned"), local.assignedTo, remote.assignedTo, g);
        addRow(mainPanel, I18n.get("diff.field.category"), local.category, remote.category, g);
        addRow(mainPanel, I18n.get("diff.field.version"), local.targetVersion, remote.targetVersion, g);
        addRow(mainPanel, I18n.get("diff.field.date"), local.createdOn != null ? sdf.format(local.createdOn) : "",
                remote.createdOn != null ? sdf.format(remote.createdOn) : "", g);

        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 3;
        mainPanel.add(new JLabel(I18n.get("diff.label.desc")), g);
        g.gridy++;
        JPanel descPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JTextArea t1 = new JTextArea(local.description);
        t1.setLineWrap(true);
        t1.setEditable(false);
        t1.setBackground(new Color(250, 250, 250));
        JTextArea t2 = new JTextArea(remote.description);
        t2.setLineWrap(true);
        t2.setEditable(false);
        t2.setBackground(new Color(250, 250, 250));
        if (!Objects.equals(local.description, remote.description)) {
            t2.setBackground(new Color(255, 230, 230));
        }
        JScrollPane s1 = new JScrollPane(t1);
        s1.setPreferredSize(new Dimension(300, 150));
        JScrollPane s2 = new JScrollPane(t2);
        s2.setPreferredSize(new Dimension(300, 150));
        descPanel.add(s1);
        descPanel.add(s2);
        mainPanel.add(descPanel, g);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton(I18n.get("diff.btn.close"));
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addRow(JPanel p, String label, String v1, String v2, GridBagConstraints g) {
        g.gridy++;
        g.gridx = 0;
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        p.add(l, g);
        boolean diff = !Objects.equals(v1, v2);
        g.gridx = 1;
        JTextField t1 = new JTextField(v1);
        t1.setEditable(false);
        p.add(t1, g);
        g.gridx = 2;
        JTextField t2 = new JTextField(v2);
        t2.setEditable(false);
        if (diff) {
            t2.setBackground(new Color(255, 220, 220));
            t2.setForeground(new Color(150, 0, 0));
        } else {
            t2.setForeground(new Color(0, 100, 0));
        }
        p.add(t2, g);
    }
}
