package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import redmineconnector.model.Task;
import redmineconnector.model.VersionDTO;
import redmineconnector.service.DataService;
import redmineconnector.ui.UIHelper;
import redmineconnector.util.I18n;

public class VersionManagerDialog extends JDialog {
    private final DataService service;
    private final String projectId;
    private final String clientName;
    private final DefaultTableModel model;
    private final JTable table;
    private List<VersionDTO> versions;
    private final JButton btnNew, btnEdit, btnDelete, btnCloseVersion, btnShowTasks, btnEmail;
    private javax.swing.JCheckBox chkShowClosedRef;
    private final Runnable onChanged;

    public VersionManagerDialog(Window owner, String title, DataService service, String projectId, String clientName,
            Runnable onChanged) {
        super(owner, I18n.format("version.dialog.title", title), ModalityType.MODELESS);
        this.service = service;
        this.projectId = projectId;
        this.clientName = clientName;
        this.onChanged = onChanged;
        setSize(redmineconnector.util.AppConstants.DIALOG_WIDTH_LARGE,
                redmineconnector.util.AppConstants.DIALOG_HEIGHT_MEDIUM);
        setLocationRelativeTo(owner);
        UIHelper.addEscapeListener(this);
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[] {
                I18n.get("version.col.id"),
                I18n.get("version.col.name"),
                I18n.get("version.col.status"),
                I18n.get("version.col.start"),
                I18n.get("version.col.end") }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && getSelected() != null)
                    promptEdit(getSelected());
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnNew = new JButton(I18n.get("version.btn.new"));
        btnEdit = new JButton(I18n.get("version.btn.edit"));
        btnDelete = new JButton(I18n.get("version.btn.delete"));
        btnCloseVersion = new JButton(I18n.get("version.btn.close"));
        btnShowTasks = new JButton(I18n.get("version.btn.tasks"));
        btnEmail = new JButton(I18n.get("version.btn.email"));

        btnNew.addActionListener(e -> promptEdit(null));
        btnEdit.addActionListener(e -> promptEdit(getSelected()));
        btnDelete.addActionListener(e -> deleteVersion(getSelected()));
        btnCloseVersion.addActionListener(e -> closeVersion(getSelected()));
        btnShowTasks.addActionListener(e -> showVersionTasks(getSelected()));
        btnEmail.addActionListener(e -> prepareEmail(getSelected()));

        JCheckBox chkShowClosed = new JCheckBox(I18n.get("version.chk.show_closed", "Mostrar Cerradas"));
        chkShowClosed.addActionListener(e -> {
            loadData();
        });

        toolbar.add(btnNew);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnCloseVersion);
        toolbar.add(btnShowTasks);
        toolbar.add(btnEmail);
        toolbar.add(chkShowClosed);

        // Store reference for loadData
        this.chkShowClosedRef = chkShowClosed;

        add(toolbar, BorderLayout.NORTH);

        loadData();
    }

    private VersionDTO getSelected() {
        int r = table.getSelectedRow();
        if (r == -1)
            return null;
        int id = (int) model.getValueAt(r, 0);
        return versions.stream().filter(v -> v.id == id).findFirst().orElse(null);
    }

    private void loadData() {
        new SwingWorker<List<VersionDTO>, Void>() {
            @Override
            protected List<VersionDTO> doInBackground() throws Exception {
                return service.fetchVersionsFull(projectId);
            }

            @Override
            protected void done() {
                try {
                    versions = get();
                    versions.sort(Comparator.comparing((VersionDTO v) -> v.dueDate == null ? "9999-99-99" : v.dueDate)
                            .reversed());
                    model.setRowCount(0);
                    boolean showClosed = chkShowClosedRef != null && chkShowClosedRef.isSelected();

                    for (VersionDTO v : versions) {
                        if (!showClosed && "closed".equalsIgnoreCase(v.status)) {
                            continue;
                        }
                        model.addRow(new Object[] { v.id, v.name, v.status, formatDisplayDate(v.startDate),
                                formatDisplayDate(v.dueDate) });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(VersionManagerDialog.this,
                            I18n.format("version.error.loading", e.getMessage()));
                }
            }
        }.execute();
    }

    // API Date (yyyy-MM-dd) -> Display (dd/MM/yyyy)
    private String formatDisplayDate(String ymd) {
        if (ymd == null || ymd.length() < 10)
            return "";
        try {
            return new SimpleDateFormat("dd/MM/yyyy")
                    .format(new SimpleDateFormat("yyyy-MM-dd").parse(ymd.substring(0, 10)));
        } catch (Exception e) {
            return ymd;
        }
    }

    // Display (dd/MM/yyyy) -> API (yyyy-MM-dd)
    private String parseDateToAPI(String d) {
        if (d == null || d.trim().isEmpty())
            return "";
        try {
            return new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("dd/MM/yyyy").parse(d));
        } catch (ParseException e) {
            return "";
        }
    }

    private void closeVersion(VersionDTO v) {
        if (v == null)
            return;
        if (JOptionPane.showConfirmDialog(this, I18n.format("version.msg.close_confirm", v.name),
                I18n.get("version.title.close_confirm"),
                JOptionPane.YES_NO_OPTION) == 0) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    service.updateVersion(v.id, v.name, "closed", v.startDate, v.dueDate);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadData();
                        if (onChanged != null)
                            onChanged.run();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(VersionManagerDialog.this,
                                I18n.format("version.error.close", e.getMessage()));
                    }
                }
            }.execute();
        }
    }

    private void promptEdit(VersionDTO v) {
        JDialog d = new JDialog(this, v == null ? I18n.get("version.title.new") : I18n.get("version.title.edit"), true);
        d.setLayout(new GridLayout(5, 2, 5, 5));
        UIHelper.addEscapeListener(d);

        JTextField txtName = new JTextField(v != null ? v.name : "");
        JComboBox<String> cbStatus = new JComboBox<>(new String[] { "open", "locked", "closed" });
        if (v != null)
            cbStatus.setSelectedItem(v.status);

        JTextField txtStart = new JTextField(v != null ? formatDisplayDate(v.startDate) : "");
        JPanel pStart = createDatePanel(txtStart, false);

        JTextField txtEnd = new JTextField(v != null ? formatDisplayDate(v.dueDate) : "");
        JPanel pEnd = createDatePanel(txtEnd, true);

        d.add(new JLabel(I18n.get("version.label.name")));
        d.add(txtName);
        d.add(new JLabel(I18n.get("version.label.status")));
        d.add(cbStatus);
        d.add(new JLabel(I18n.get("version.label.start")));
        d.add(pStart);
        d.add(new JLabel(I18n.get("version.label.end")));
        d.add(pEnd);

        JButton btnOk = new JButton(I18n.get("version.btn.save"));
        btnOk.addActionListener(ev -> {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String apiStart = parseDateToAPI(txtStart.getText());
                    String apiEnd = parseDateToAPI(txtEnd.getText());

                    if (v == null)
                        service.createVersion(projectId, txtName.getText(), (String) cbStatus.getSelectedItem(),
                                apiStart, apiEnd);
                    else
                        service.updateVersion(v.id, txtName.getText(), (String) cbStatus.getSelectedItem(), apiStart,
                                apiEnd);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        d.dispose();
                        loadData();
                        if (onChanged != null)
                            onChanged.run();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(d, I18n.format("version.error.save", e.getMessage()));
                    }
                }
            }.execute();
        });
        d.add(btnOk);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private JPanel createDatePanel(JTextField txt, boolean editable) {
        JPanel p = new JPanel(new BorderLayout());
        txt.setEditable(editable);
        p.add(txt, BorderLayout.CENTER);

        if (editable) {
            JButton btn = new JButton("📅");
            btn.setMargin(new Insets(1, 4, 1, 4));
            btn.addActionListener(e -> {
                DatePickerPopup popup = new DatePickerPopup(d -> txt.setText(d));
                popup.show(btn, 0, btn.getHeight());
            });
            p.add(btn, BorderLayout.EAST);
        }
        return p;
    }

    private void deleteVersion(VersionDTO v) {
        if (v == null)
            return;
        if (JOptionPane.showConfirmDialog(this, I18n.format("version.msg.delete_confirm", v.name),
                I18n.get("version.title.delete_confirm"),
                JOptionPane.YES_NO_OPTION) == 0) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    service.deleteVersion(v.id);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadData();
                        if (onChanged != null)
                            onChanged.run();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(VersionManagerDialog.this,
                                I18n.format("version.error.delete", e.getMessage()));
                    }
                }
            }.execute();
        }
    }

    private void showVersionTasks(VersionDTO v) {
        if (v == null)
            return;
        JDialog d = new JDialog(this, I18n.format("version.dialog.tasks_title", v.name), true);
        d.setSize(redmineconnector.util.AppConstants.DIALOG_WIDTH_MEDIUM,
                redmineconnector.util.AppConstants.DIALOG_HEIGHT_SMALL);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());
        UIHelper.addEscapeListener(d);

        JTextArea txt = new JTextArea();
        txt.setEditable(false);
        txt.setFont(new Font("Monospaced", Font.PLAIN, 12));
        d.add(new JScrollPane(txt), BorderLayout.CENTER);

        JButton btnCopy = new JButton(I18n.get("version.btn.copy"));
        btnCopy.addActionListener(e -> {
            txt.selectAll();
            txt.copy();
            JOptionPane.showMessageDialog(d, I18n.get("version.msg.copied"));
        });
        d.add(btnCopy, BorderLayout.SOUTH);

        txt.setText(I18n.get("version.msg.loading"));
        new SwingWorker<List<Task>, Void>() {
            @Override
            protected List<Task> doInBackground() throws Exception {
                return service.fetchTasksByVersion(projectId, v.id);
            }

            @Override
            protected void done() {
                try {
                    List<Task> tasks = get();
                    StringBuilder sb = new StringBuilder();
                    sb.append(I18n.format("version.report.header", v.name)).append("\n");
                    sb.append("==================================================\n");
                    if (tasks.isEmpty()) {
                        sb.append(I18n.get("version.report.no_tasks"));
                    } else {
                        for (Task t : tasks) {
                            sb.append(String.format("#%-5d [%-10s] %s (%s)\n", t.id, (t.status != null ? t.status : ""),
                                    t.subject,
                                    (t.assignedTo != null ? t.assignedTo : I18n.get("status.user.unassigned"))));
                        }
                    }
                    txt.setText(sb.toString());
                    txt.setCaretPosition(0);
                } catch (Exception ex) {
                    txt.setText(I18n.format("version.error.tasks", ex.getMessage()));
                }
            }
        }.execute();
        d.setVisible(true);
    }

    private void prepareEmail(VersionDTO v) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<Task>, Void>() {
            @Override
            protected List<Task> doInBackground() throws Exception {
                return service.fetchTasksByVersion(projectId, v.id);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    List<Task> tasks = get();
                    String subject = I18n.format("version.email.subject", clientName, v.name);
                    StringBuilder body = new StringBuilder();
                    body.append(I18n.format("version.email.body", v.name)).append("\n");
                    for (Task t : tasks) {
                        body.append(String.format("- #%d: %s (%s)\n", t.id, t.subject, t.status));
                    }

                    String encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8.toString()).replace("+",
                            "%20");
                    String encodedBody = URLEncoder.encode(body.toString(), StandardCharsets.UTF_8.toString())
                            .replace("+", "%20");
                    String uriStr = "mailto:?subject=" + encodedSubject + "&body=" + encodedBody;

                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().mail(new URI(uriStr));
                    } else {
                        JOptionPane.showMessageDialog(VersionManagerDialog.this,
                                I18n.get("version.error.email_support"));
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(VersionManagerDialog.this,
                            I18n.format("version.error.email_prepare", e.getMessage()));
                }
            }
        }.execute();
    }
}
