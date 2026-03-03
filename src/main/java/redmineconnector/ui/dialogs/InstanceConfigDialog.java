package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import redmineconnector.config.ConfigManager;
import redmineconnector.ui.UIHelper;
import redmineconnector.util.I18n;

public class InstanceConfigDialog extends JDialog {
    private final Properties props;
    private final String prefix;
    private final JTextField txtUrl = new JTextField(), txtKey = new JTextField(), txtProj = new JTextField();
    private final JTextField txtLimit = new JTextField(), txtRefresh = new JTextField();
    private final JTextField txtRefPattern = new JTextField();
    private final JTextField txtPath = new JTextField();
    private final JTextField txtClientName = new JTextField();
    private final JTextField txtUserEmail = new JTextField();
    private final JTextField txtFolderPattern = new JTextField();

    // Notification Checkboxes
    private final JCheckBox chkNotifyNew = new JCheckBox("Nueva Actividad (Tareas nuevas)");
    private final JCheckBox chkNotifyWarn = new JCheckBox("Avisos Operativos (Falta ID proyecto, etc.)");
    private final JCheckBox chkNotifyConf = new JCheckBox("Confirmaciones (Guardado, descarga, etc.)");
    private final JCheckBox chkNotifyErr = new JCheckBox("Errores (Conexión, fallos, etc.)");

    private javax.swing.JComboBox<String> cmbAttachmentFormat;
    private Runnable saveCallback;

    public InstanceConfigDialog(Frame owner, String prefix, String title) {
        super(owner, I18n.format("config.dialog.title", title), false);
        this.prefix = prefix;
        this.props = ConfigManager.loadConfig();
        setupUI();
        loadValues();
        UIHelper.addEscapeListener(this);
        setSize(redmineconnector.util.AppConstants.DIALOG_WIDTH_MEDIUM - 100,
                redmineconnector.util.AppConstants.DIALOG_HEIGHT_MEDIUM + 50);
        setLocationRelativeTo(owner);
    }

    public void onSave(Runnable r) {
        this.saveCallback = r;
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", createGeneralPanel());
        tabs.addTab("Notificaciones", createNotificationsPanel());

        add(tabs, BorderLayout.CENTER);

        JButton bSave = new JButton(I18n.get("config.dialog.btn.save"));
        bSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bSave.addActionListener(e -> {
            save();
            if (saveCallback != null)
                saveCallback.run();
            dispose();
        });
        JPanel bp = new JPanel();
        bp.add(bSave);
        add(bp, BorderLayout.SOUTH);
    }

    private JPanel createNotificationsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(10, 5, 10, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        p.add(new JLabel("Seleccione qué notificaciones desea recibir:"), g);
        g.gridy++;
        p.add(chkNotifyNew, g);
        g.gridy++;
        p.add(chkNotifyWarn, g);
        g.gridy++;
        p.add(chkNotifyConf, g);
        g.gridy++;
        p.add(chkNotifyErr, g);

        g.gridy++;
        g.weighty = 1.0;
        p.add(new JLabel(""), g); // spacer

        return p;
    }

    private JPanel createGeneralPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridx = 0;
        g.gridy = 0;
        p.add(new JLabel(I18n.get("config.dialog.label.url")), g);
        g.gridx = 1;
        p.add(txtUrl, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("config.dialog.label.apikey")), g);
        g.gridx = 1;
        p.add(txtKey, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("config.dialog.label.projectid")), g);
        g.gridx = 1;
        p.add(txtProj, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("config.dialog.label.clientname")), g);
        g.gridx = 1;
        p.add(txtClientName, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("config.dialog.label.useremail", "User Email:")), g);
        g.gridx = 1;
        p.add(txtUserEmail, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JSeparator(), g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("config.dialog.label.limit")), g);
        g.gridx = 1;
        p.add(txtLimit, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("config.dialog.label.refresh")), g);
        g.gridx = 1;
        p.add(txtRefresh, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JSeparator(), g);
        JPanel patP = new JPanel(new BorderLayout());
        JLabel lblHelp = new JLabel(I18n.get("config.dialog.help.pattern"));
        lblHelp.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblHelp.setForeground(Color.GRAY);
        patP.add(txtRefPattern, BorderLayout.CENTER);
        patP.add(lblHelp, BorderLayout.SOUTH);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("config.dialog.label.pattern")), g);
        g.gridx = 1;
        p.add(patP, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("config.dialog.label.path")), g);
        JPanel pathP = new JPanel(new BorderLayout());
        pathP.add(txtPath, BorderLayout.CENTER);
        JButton btnPath = new JButton("...");
        btnPath.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                txtPath.setText(fc.getSelectedFile().getAbsolutePath());
        });
        pathP.add(btnPath, BorderLayout.EAST);
        g.gridx = 1;
        p.add(pathP, g);
        g.gridx = 0;
        g.gridy++;

        p.add(new JLabel("Patrón Carpeta:"), g);
        JPanel fPatP = new JPanel(new BorderLayout());
        JLabel lblFHelp = new JLabel("Variables: {id}, {subject}, {tracker}, {priority}");
        lblFHelp.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblFHelp.setForeground(Color.GRAY);
        fPatP.add(txtFolderPattern, BorderLayout.CENTER);
        fPatP.add(lblFHelp, BorderLayout.SOUTH);
        g.gridx = 1;
        p.add(fPatP, g);
        g.gridx = 0;
        g.gridy++;

        p.add(new JLabel("Formato Adjuntos:"), g);
        g.gridx = 1;
        javax.swing.JComboBox<String> cmbFormat = new javax.swing.JComboBox<>(new String[] { "Textile", "Markdown" });
        if ("markdown".equalsIgnoreCase(props.getProperty(prefix + ".attachmentFormat", "textile"))) {
            cmbFormat.setSelectedItem("Markdown");
        } else {
            cmbFormat.setSelectedItem("Textile");
        }
        // Save logic hook
        this.cmbAttachmentFormat = cmbFormat; // Store reference
        p.add(cmbFormat, g);

        return p;
    }

    private void loadValues() {
        txtUrl.setText(props.getProperty(prefix + ".url", ""));
        txtKey.setText(props.getProperty(prefix + ".key", ""));
        txtProj.setText(props.getProperty(prefix + ".project", ""));
        txtLimit.setText(props.getProperty(prefix + ".limit", "100"));
        txtRefresh.setText(props.getProperty(prefix + ".refresh", "5"));
        txtRefPattern.setText(props.getProperty(prefix + ".pattern", "[Ref #{id}]"));
        txtPath.setText(props.getProperty(prefix + ".downloadPath", ""));
        txtClientName.setText(props.getProperty(prefix + ".clientName", ""));
        txtUserEmail.setText(props.getProperty(prefix + ".userEmail", ""));

        chkNotifyNew.setSelected("true".equalsIgnoreCase(props.getProperty(prefix + ".notify.new", "true")));
        chkNotifyWarn.setSelected("true".equalsIgnoreCase(props.getProperty(prefix + ".notify.warn", "true")));
        chkNotifyConf.setSelected("true".equalsIgnoreCase(props.getProperty(prefix + ".notify.conf", "true")));
        chkNotifyErr.setSelected("true".equalsIgnoreCase(props.getProperty(prefix + ".notify.err", "true")));
    }

    private void save() {
        props.setProperty(prefix + ".url", txtUrl.getText());
        props.setProperty(prefix + ".key", txtKey.getText());
        props.setProperty(prefix + ".project", txtProj.getText());
        props.setProperty(prefix + ".limit", txtLimit.getText());
        props.setProperty(prefix + ".refresh", txtRefresh.getText());
        props.setProperty(prefix + ".pattern", txtRefPattern.getText());
        props.setProperty(prefix + ".downloadPath", txtPath.getText());
        props.setProperty(prefix + ".clientName", txtClientName.getText());
        props.setProperty(prefix + ".userEmail", txtUserEmail.getText());

        props.setProperty(prefix + ".notify.new", String.valueOf(chkNotifyNew.isSelected()));
        props.setProperty(prefix + ".notify.warn", String.valueOf(chkNotifyWarn.isSelected()));
        props.setProperty(prefix + ".notify.conf", String.valueOf(chkNotifyConf.isSelected()));
        props.setProperty(prefix + ".notify.err", String.valueOf(chkNotifyErr.isSelected()));

        if (cmbAttachmentFormat != null) {
            props.setProperty(prefix + ".attachmentFormat",
                    cmbAttachmentFormat.getSelectedItem().toString().toLowerCase());
        }
        ConfigManager.saveConfig(props);
    }
}
