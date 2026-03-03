package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import redmineconnector.config.ConfigManager;
import redmineconnector.util.I18n;

public class ClientManagerDialog extends JDialog {
    private final Properties props;
    private final DefaultListModel<String> listModel;
    private final JList<String> listClients;
    private boolean changed = false;

    public ClientManagerDialog(Frame owner) {
        super(owner, I18n.get("client.manager.title", "Multi-Client Manager"), true);
        this.props = ConfigManager.loadConfig();
        this.listModel = new DefaultListModel<>();

        loadClients();

        this.listClients = new JList<>(listModel);

        setupUI();
        setSize(redmineconnector.util.AppConstants.DIALOG_WIDTH_SMALL,
                redmineconnector.util.AppConstants.DIALOG_HEIGHT_SMALL);
        setLocationRelativeTo(owner);
    }

    private void loadClients() {
        String raw = props.getProperty("clients.list", "client1,client2");
        if (raw.trim().isEmpty()) {
            raw = "client1,client2";
        }
        for (String c : raw.split(",")) {
            if (!c.trim().isEmpty()) {
                listModel.addElement(c.trim());
            }
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        JPanel pList = new JPanel(new BorderLayout());
        pList.setBorder(new EmptyBorder(10, 10, 10, 10));
        pList.add(new JScrollPane(listClients), BorderLayout.CENTER);

        JPanel pButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAdd = new JButton(I18n.get("client.manager.add", "Add"));
        JButton btnRemove = new JButton(I18n.get("client.manager.remove", "Remove"));

        btnAdd.addActionListener(e -> addClient());
        btnRemove.addActionListener(e -> removeClient());

        pButtons.add(btnAdd);
        pButtons.add(btnRemove);

        pList.add(pButtons, BorderLayout.SOUTH);
        add(pList, BorderLayout.CENTER);

        JButton btnClose = new JButton(I18n.get("dict.close", "Close"));
        btnClose.addActionListener(e -> dispose());

        JPanel bottom = new JPanel();
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addClient() {
        String name = JOptionPane.showInputDialog(this,
                I18n.get("client.manager.enter_id", "Enter new client ID (no spaces):"));
        if (name != null && !name.trim().isEmpty()) {
            name = name.trim().replaceAll("\\s+", "_");
            if (listModel.contains(name)) {
                JOptionPane.showMessageDialog(this, I18n.get("client.manager.exists", "Client ID already exists!"));
                return;
            }
            listModel.addElement(name);
            saveClients();
            changed = true;
        }
    }

    private void removeClient() {
        String selected = listClients.getSelectedValue();
        if (selected == null)
            return;

        if (JOptionPane.showConfirmDialog(this,
                I18n.format("client.manager.confirm_delete", "Delete client configuration for {0}?", selected),
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

            listModel.removeElement(selected);
            // remove properties with prefix "selected."
            List<String> keysToRemove = new ArrayList<>();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith(selected + ".")) {
                    keysToRemove.add(key);
                }
            }
            for (String k : keysToRemove)
                props.remove(k);

            saveClients();
            changed = true;
        }
    }

    private void saveClients() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < listModel.getSize(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(listModel.getElementAt(i));
        }
        props.setProperty("clients.list", sb.toString());
        ConfigManager.saveConfig(props);
    }

    public boolean isChanged() {
        return changed;
    }
}
