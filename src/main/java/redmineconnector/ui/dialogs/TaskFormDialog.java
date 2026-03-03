package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.event.HyperlinkEvent;

import redmineconnector.model.Attachment;
import redmineconnector.model.Changeset;
import redmineconnector.model.CustomField;
import redmineconnector.model.CustomFieldDefinition;
import redmineconnector.model.Journal;
import redmineconnector.model.SimpleEntity;
import redmineconnector.model.Task;
import redmineconnector.model.UploadToken;
import redmineconnector.ui.UIHelper;
import redmineconnector.ui.components.SearchableComboBox;
// import redmineconnector.ui.components.DragDropTextArea; - Removed
import redmineconnector.ui.components.DragDropImageTextPane;
import redmineconnector.ui.components.DragDropFilePanel;
import redmineconnector.util.I18n;
import redmineconnector.util.LoggerUtil;
import redmineconnector.ui.components.AutocompleteHelper;
import java.util.stream.Collectors;

public class TaskFormDialog extends JDialog {
    public interface TimeLogger {
        void log(String date, double hours, int userId, int activityId, String comment) throws Exception;
    }

    public interface UploadHandler {
        UploadToken upload(File f) throws Exception;
    }

    public interface DownloadHandler {
        void download(Attachment att);
    }

    JTextField txtSubject = new JTextField(20); // Set columns to prevent unlimited expansion
    DragDropImageTextPane txtDesc = new DragDropImageTextPane(10, 30);
    JEditorPane txtHistory = new JEditorPane();
    DragDropImageTextPane txtComment = new DragDropImageTextPane(5, 30);
    DefaultListModel<Object> attachmentModel = new DefaultListModel<>();
    JList<Object> listAttachments = new JList<>(attachmentModel);
    JEditorPane txtRevisions = new JEditorPane();
    DragDropFilePanel dragDropFilePanel;

    JComboBox<SimpleEntity> cbTracker = new JComboBox<>();
    JComboBox<SimpleEntity> cbStatus = new JComboBox<>();
    JComboBox<SimpleEntity> cbPriority = new JComboBox<>();
    JComboBox<SimpleEntity> cbAssigned = new JComboBox<>();
    JComboBox<SimpleEntity> cbCategory = new JComboBox<>();
    JComboBox<SimpleEntity> cbVersion = new JComboBox<>();
    SearchableComboBox cbParent = new SearchableComboBox(I18n.get("task.form.label.parent"));
    JComboBox<String> cbDoneRatio = new JComboBox<>();

    JLabel lblAuthor = new JLabel(" ");
    JLabel lblAuthorEmail = new JLabel(" ");

    JTabbedPane tabs = new JTabbedPane();
    JComboBox<SimpleEntity> cbTimeUser = new JComboBox<>();
    JComboBox<SimpleEntity> cbActivity = new JComboBox<>();
    JTextField txtTimeDate = new JTextField(10);
    JTextField txtTimeHours = new JTextField(5);
    JTextField txtTimeComment = new JTextField(20);
    JButton btnLogTime = new JButton(I18n.get("task.form.btn.log_time"));

    // Custom Fields support
    JPanel pCustomFields = new JPanel(new GridBagLayout());
    java.util.Map<Integer, JComponent> customFieldInputs = new java.util.HashMap<>();
    List<CustomFieldDefinition> customFieldDefs;

    Consumer<Task> saveListener;
    TimeLogger timeListener;
    UploadHandler uploadHandler;
    DownloadHandler downloadHandler;
    private redmineconnector.service.DataService dataService; // For downloading images
    Task loadedTask = new Task();
    private redmineconnector.service.AsyncDataService asyncService;
    File uploadRoot = new File(System.getProperty("user.home"));
    private String contextUrl;
    private SimpleEntity contextProject;
    // Use Set for O(1) lookups
    private java.util.Set<Integer> allowedCustomFieldIds = new java.util.HashSet<>();
    private boolean isContextRestrictionActive = false;
    private SimpleEntity defaultTimeUser;
    private boolean sortNotesNewestFirst = true; // Sort order for notes
    private JComboBox<String> cbNotesSort = new JComboBox<>(
            new String[] { I18n.get("task.form.notes.sort.newest"), I18n.get("task.form.notes.sort.oldest") });

    private boolean isEditMode = false;
    private List<SimpleEntity> allStatuses;
    private java.util.Map<Integer, List<SimpleEntity>> statusHeuristic;

    public void setStatusHeuristic(java.util.Map<Integer, List<SimpleEntity>> heuristic) {
        this.statusHeuristic = heuristic;
    }

    private redmineconnector.service.DataService sourceDataService;
    public void setSourceDataService(redmineconnector.service.DataService sourceDataService) {
        this.sourceDataService = sourceDataService;
    }

    public TaskFormDialog(Window w, String t, List<SimpleEntity> u, List<SimpleEntity> tr, List<SimpleEntity> p,
            List<SimpleEntity> s, List<SimpleEntity> c, List<SimpleEntity> v, List<SimpleEntity> act,
            List<SimpleEntity> parents, String contextUrl, SimpleEntity contextProject, boolean isEditMode) {
        super(w, t, ModalityType.APPLICATION_MODAL);
        this.isEditMode = isEditMode;
        this.allStatuses = s; // Save for fallback
        UIHelper.addEscapeListener(this);
        setLayout(new BorderLayout());
        this.contextUrl = contextUrl;
        this.contextProject = contextProject;

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBorder(new EmptyBorder(5, 20, 0, 20));
        lblAuthor.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblAuthor.setForeground(Color.DARK_GRAY);
        lblAuthorEmail.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblAuthorEmail.setForeground(Color.GRAY);
        headerPanel.add(lblAuthor);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(lblAuthorEmail);
        add(headerPanel, BorderLayout.NORTH);

        fillCombo(cbTracker, tr);
        cbTracker.addActionListener(e -> {
            SimpleEntity sel = (SimpleEntity) cbTracker.getSelectedItem();
            if (sel != null && asyncService != null) {
                updateContext(sel.id);
            }
        });
        fillCombo(cbStatus, s);
        fillCombo(cbPriority, p);
        cbAssigned.addItem(new SimpleEntity(0, " "));
        cbAssigned.addItem(new SimpleEntity(-2, I18n.get("dialog.assignment.author"))); // Add "Author" option
        fillCombo(cbAssigned, u);
        cbCategory.addItem(new SimpleEntity(0, " "));
        fillCombo(cbCategory, c);
        cbVersion.addItem(new SimpleEntity(0, I18n.get("version.combo.empty")));
        fillCombo(cbVersion, v);
        cbParent.setItems(parents);

        // Populate Done Ratio
        for (int i = 0; i <= 100; i += 10) {
            cbDoneRatio.addItem(i + " %");
        }

        cbTimeUser.addItem(new SimpleEntity(0, I18n.get("task.form.combo.select_user")));
        fillCombo(cbTimeUser, u);
        fillCombo(cbActivity, act);
        // Set strict prototype values to prevent ComboBoxes from expanding the dialog
        // width
        // Set strict prototype values to prevent ComboBoxes from expanding the dialog
        // width
        // Modified for 2-column layout:
        // High-density grid (2 cols) means each column has ~310px available.
        // 50 chars "W" is too wide (~500px). Reducing to 25 chars (~250px) to fit
        // side-by-side.
        String protoWide = "WWWWWWWWWWWWWWWWWWWWWWWWW"; // 25 chars
        String protoSmall = "WWWWWWWWWWWW"; // 12 chars

        cbTracker.setPrototypeDisplayValue(new SimpleEntity(0, protoSmall));
        cbStatus.setPrototypeDisplayValue(new SimpleEntity(0, protoSmall));
        cbPriority.setPrototypeDisplayValue(new SimpleEntity(0, protoSmall));
        cbAssigned.setPrototypeDisplayValue(new SimpleEntity(0, protoWide));
        cbCategory.setPrototypeDisplayValue(new SimpleEntity(0, protoWide));
        cbVersion.setPrototypeDisplayValue(new SimpleEntity(0, protoWide));
        // cbParent is SearchableComboBox (JButton based), handled by updateLabel
        // truncation
        cbParent.setMaxWidthChars(100); // Allow full title visibility in full-width row
        cbTimeUser.setPrototypeDisplayValue(new SimpleEntity(0, protoWide));
        cbActivity.setPrototypeDisplayValue(new SimpleEntity(0, protoWide));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 0, 10, 0));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // --- ROW 0: SUBJECT (Full Width) ---
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.subject")), g);

        g.gridx = 1;
        g.gridwidth = 3; // Span across Field1, Label2, Field2 columns
        g.weightx = 1.0;
        form.add(txtSubject, g);

        // --- ROW 1: Tracker | Status ---
        g.gridy++;
        g.gridwidth = 1; // Reset gridwidth

        // Column 1
        g.gridx = 0;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.tracker")), g);

        g.gridx = 1;
        g.weightx = 0.5;
        // Listener re-attaching logic is handled inside buildCustomFieldsUI or
        // externally
        cbTracker.addActionListener(e -> {
            SimpleEntity val = (SimpleEntity) cbTracker.getSelectedItem();
            if (val != null) {
                isContextRestrictionActive = false;
                updateContext(val.id);
                if (customFieldDefs != null) {
                    buildCustomFieldsUI();
                    if (loadedTask != null) {
                        fillCustomFields(loadedTask);
                    }
                }
            }
        });
        form.add(cbTracker, g);

        // Column 2
        g.gridx = 2;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.status")), g);

        g.gridx = 3;
        g.weightx = 0.5;
        form.add(cbStatus, g);

        // --- ROW 2: Priority | Assigned ---
        g.gridy++;

        // Column 1
        g.gridx = 0;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.priority")), g);

        g.gridx = 1;
        g.weightx = 0.5;
        form.add(cbPriority, g);

        // Column 2
        g.gridx = 2;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.assigned")), g);

        g.gridx = 3;
        g.weightx = 0.5;
        form.add(cbAssigned, g);

        // --- ROW 3: Category | Version ---
        g.gridy++;

        // Column 1
        g.gridx = 0;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.category")), g);

        g.gridx = 1;
        g.weightx = 0.5;
        form.add(cbCategory, g);

        // Column 2
        g.gridx = 2;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.version")), g);

        g.gridx = 3;
        g.weightx = 0.5;
        form.add(cbVersion, g);

        // --- ROW 4: Parent (Full Width for visibility) ---
        g.gridy++;

        g.gridx = 0;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.parent")), g);

        g.gridx = 1;
        g.gridwidth = 3; // Span across to end
        g.weightx = 1.0;
        form.add(cbParent, g);

        // --- ROW 5: Done Ratio ---
        g.gridy++;
        g.gridwidth = 1; // Reset gridwidth

        g.gridx = 0;
        g.weightx = 0.0;
        form.add(new JLabel(I18n.get("task.form.label.ratio")), g);

        g.gridx = 1;
        g.weightx = 0.5;
        form.add(cbDoneRatio, g);

        // --- Custom Fields (Full Width Wrapper) ---
        g.gridx = 0;
        g.gridy++;
        g.gridwidth = 4; // Span all 4 columns
        g.weightx = 1.0;
        form.add(pCustomFields, g);

        cbStatus.addActionListener(e -> {
            SimpleEntity selStatus = (SimpleEntity) cbStatus.getSelectedItem();
            if (selStatus != null) {
                String lower = selStatus.name.toLowerCase();
                if (lower.contains("cerrad") || lower.contains("close") || lower.contains("resuel")
                        || lower.contains("soluc") || lower.contains("reject") || lower.contains("rechaz")) {
                    cbDoneRatio.setSelectedItem("100 %");
                }
            }
        });

        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtDesc.setToolTipText(
                "<html><b>Puedes arrastrar archivos de texto aquí</b><br>Formatos: .txt, .md, .log, .json, etc.</html>");
        txtDesc.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(redmineconnector.util.AppConstants.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 25))); // Right padding to match History width

        JPanel pDesc = new JPanel(new BorderLayout());
        pDesc.add(redmineconnector.ui.components.EditorToolbarHelper.createToolbar(txtDesc), BorderLayout.NORTH);
        JScrollPane scrollDesc = new JScrollPane(txtDesc);
        // CRITICAL FIX: Set minimal preferred size to prevent content (images/long
        // text) from pushing outer dialog width.
        // This forces the ScrollPane to use its own scrollbars instead of expanding the
        // parent.
        scrollDesc.setPreferredSize(new Dimension(10, 100));
        pDesc.add(scrollDesc, BorderLayout.CENTER);
        tabs.addTab(I18n.get("task.form.tab.desc"), pDesc);

        JPanel pNotes = new JPanel(new BorderLayout());

        // Header with sort combo box
        JPanel pNotesHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pNotesHeader.add(new JLabel(I18n.get("task.form.notes.sort.label")));
        cbNotesSort.setSelectedIndex(0); // Default: newest first
        cbNotesSort.addActionListener(e -> {
            sortNotesNewestFirst = cbNotesSort.getSelectedIndex() == 0;
            if (loadedTask != null) {
                fill(loadedTask); // Rebuild history with new sort order
            }
        });
        pNotesHeader.add(cbNotesSort);
        pNotes.add(pNotesHeader, BorderLayout.NORTH);

        txtHistory.setContentType("text/html");
        txtHistory.setEditable(false);
        JScrollPane scrollHistory = new JScrollPane(txtHistory);
        // CRITICAL FIX: Prevent HTML history (especially pre-blocks) from expanding
        // dialog
        scrollHistory.setPreferredSize(new Dimension(10, 100));
        pNotes.add(scrollHistory, BorderLayout.CENTER);
        JPanel pInput = new JPanel(new BorderLayout());
        pInput.setBorder(BorderFactory.createTitledBorder(I18n.get("task.form.border.new_note")));
        pInput.setPreferredSize(new Dimension(100, 150)); // Increased height for toolbar
        txtComment.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtComment.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtComment.setToolTipText(
                "<html><b>Puedes pegar imágenes aquí (Ctrl+V)</b><br>También puedes arrastrar archivos de texto<br>Formatos: .txt, .md, .log, .json, etc.</html>");
        txtComment.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(redmineconnector.util.AppConstants.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 25))); // Right padding to match History width

        pInput.add(redmineconnector.ui.components.EditorToolbarHelper.createToolbar(txtComment), BorderLayout.NORTH);
        JScrollPane scrollComment = new JScrollPane(txtComment);
        // CRITICAL FIX: Prevent comment box expansion
        scrollComment.setPreferredSize(new Dimension(10, 60));
        pInput.add(scrollComment, BorderLayout.CENTER);
        pNotes.add(pInput, BorderLayout.SOUTH);
        tabs.addTab("Notas", pNotes);

        tabs.addTab(I18n.get("task.form.tab.attachments"), createAttachmentsPanel());
        tabs.addTab(I18n.get("task.form.tab.time"), createTimePanel());

        txtRevisions.setContentType("text/html");
        txtRevisions.setEditable(false);
        txtRevisions.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                }
            }
        });
        tabs.addTab(I18n.get("task.form.tab.revisions"), new JScrollPane(txtRevisions));

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBorder(new EmptyBorder(0, 20, 10, 20));
        centerContainer.add(form, BorderLayout.NORTH);
        centerContainer.add(tabs, BorderLayout.CENTER);

        JButton b = new JButton(I18n.get("task.form.btn.save_changes"));
        b.setPreferredSize(new Dimension(130, 30));
        b.addActionListener(e -> {
            b.setEnabled(false); // Prevent double click
            String subject = txtSubject.getText().trim();
            if (subject.isEmpty()) {
                b.setEnabled(true); // Re-enable if validation fails
                JOptionPane.showMessageDialog(this, I18n.get("task.form.error.subject_empty"),
                        I18n.get("notification.title.warn"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            loadedTask.subject = subject;
            loadedTask.description = txtDesc.getText();
            String rawComment = txtComment.getText();
            // Normalize line endings to prevent duplication on Windows
            if (rawComment != null) {
                loadedTask.comment = rawComment.replace("\r\n", "\n").replace("\r", "\n");
                redmineconnector.util.LoggerUtil.logDebug("TaskFormDialog",
                        "Comment normalized. Length: " + loadedTask.comment.length());
            } else {
                loadedTask.comment = "";
            }
            SimpleEntity trSel = (SimpleEntity) cbTracker.getSelectedItem();
            if (trSel != null) {
                loadedTask.trackerId = trSel.id;
                loadedTask.tracker = trSel.name;
            }
            SimpleEntity stSel = (SimpleEntity) cbStatus.getSelectedItem();
            if (stSel != null) {
                loadedTask.statusId = stSel.id;
                loadedTask.status = stSel.name;
            }
            SimpleEntity prSel = (SimpleEntity) cbPriority.getSelectedItem();
            if (prSel != null) {
                loadedTask.priorityId = prSel.id;
                loadedTask.priority = prSel.name;
            }
            SimpleEntity asSel = (SimpleEntity) cbAssigned.getSelectedItem();
            if (asSel != null) {
                if (asSel.id == -2 && loadedTask.authorId > 0) {
                     // Assign to Author
                     loadedTask.assignedToId = loadedTask.authorId;
                     loadedTask.assignedTo = loadedTask.author;
                } else {
                    loadedTask.assignedToId = asSel.id;
                    loadedTask.assignedTo = asSel.name;
                }
            }
            SimpleEntity catSel = (SimpleEntity) cbCategory.getSelectedItem();
            if (catSel != null) {
                loadedTask.categoryId = catSel.id;
                loadedTask.category = catSel.name;
            }
            SimpleEntity verSel = (SimpleEntity) cbVersion.getSelectedItem();
            if (verSel != null) {
                loadedTask.targetVersionId = verSel.id;
                loadedTask.targetVersion = verSel.name;
            }
            SimpleEntity parSel = (SimpleEntity) cbParent.getSelectedItem();
            if (parSel != null) {
                loadedTask.parentId = parSel.id;
                loadedTask.parentName = parSel.name;
            }
            String ratioStr = (String) cbDoneRatio.getSelectedItem();
            if (ratioStr != null) {
                try {
                    loadedTask.doneRatio = Integer.parseInt(ratioStr.replace(" %", "").trim());
                } catch (NumberFormatException ignored) {
                }
            }

            saveCustomFields(loadedTask);

            if (saveListener != null)
                saveListener.accept(loadedTask);
            dispose();
        });
        JPanel bottomP = new JPanel();
        bottomP.add(b);

        // Wrap center container in ScrollPane to ensure buttons are aways visible even
        // on small screens
        JScrollPane mainScroll = new JScrollPane(centerContainer);
        mainScroll.setBorder(null); // Align cleanly
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);

        add(mainScroll, BorderLayout.CENTER);
        add(bottomP, BorderLayout.SOUTH);

        // Setup keyboard shortcuts for tab navigation (like QuickView)
        setupKeyboardShortcuts();

        setSize(redmineconnector.util.AppConstants.DIALOG_WIDTH_LARGE,
                redmineconnector.util.AppConstants.DIALOG_HEIGHT_LARGE);
        setLocationRelativeTo(w);

        setupAutocomplete(u, parents);
    }

    private void setupAutocomplete(List<SimpleEntity> users, List<SimpleEntity> tasks) {
        if (users == null)
            users = new ArrayList<>();
        if (tasks == null)
            tasks = new ArrayList<>();

        final List<SimpleEntity> finalUsers = users;
        final List<SimpleEntity> finalTasks = tasks;

        AutocompleteHelper.CompletionProvider userProvider = query -> {
            String q = query.toLowerCase();
            return finalUsers.stream()
                    .filter(item -> item.name.toLowerCase().contains(q))
                    .limit(10)
                    .map(item -> new AutocompleteHelper.CompletionItem(item.name, item.name))
                    .collect(Collectors.toList());
        };

        AutocompleteHelper.CompletionProvider taskProvider = query -> {
            String q = query.toLowerCase();
            return finalTasks.stream()
                    .filter(item -> (item.id + "").contains(q) || item.name.toLowerCase().contains(q))
                    .limit(10)
                    .map(item -> new AutocompleteHelper.CompletionItem("#" + item.id + " - " + item.name,
                            "#" + item.id))
                    .collect(Collectors.toList());
        };

        AutocompleteHelper.attach(txtDesc, '@', userProvider);
        AutocompleteHelper.attach(txtDesc, '#', taskProvider);
        AutocompleteHelper.attach(txtComment, '@', userProvider);
        AutocompleteHelper.attach(txtComment, '#', taskProvider);
    }

    private String stringToHex(String s) {
        if (s == null)
            return "null";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(String.format("%02X ", (int) c));
        }
        return sb.toString();
    }

    private JPanel createAttachmentsPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel superior: Drag & Drop para nuevos archivos
        dragDropFilePanel = new DragDropFilePanel(files -> {
            // Cuando se añaden archivos, subirlos automáticamente
            if (uploadHandler == null) {
                LoggerUtil.logError("TaskFormDialog", "Upload handler is null, cannot upload files", null);
                JOptionPane.showMessageDialog(this,
                        I18n.get("task.form.config.error"),
                        I18n.get("task.form.config.title"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (File f : files) {
                try {
                    LoggerUtil.logDebug("TaskFormDialog", "Uploading file: " + f.getName());
                    UploadToken token = uploadHandler.upload(f);
                    if (token != null) {
                        loadedTask.pendingUploads.add(token);
                        attachmentModel.addElement(token);
                        LoggerUtil.logDebug("TaskFormDialog", "File uploaded successfully: " + f.getName());
                    } else {
                        LoggerUtil.logError("TaskFormDialog",
                                "Upload handler returned null token for file: " + f.getName(), null);
                    }
                } catch (Exception ex) {
                    LoggerUtil.logError("TaskFormDialog", "Error uploading file: " + f.getName(), ex);
                    String errorMsg = ex.getMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = ex.getClass().getSimpleName();
                    }
                    JOptionPane.showMessageDialog(this,
                            I18n.format("task.form.msg.upload.error", f.getName(), errorMsg));
                }
            }
            // Limpiar el panel después de subir (sin notificar para evitar recursión)
            dragDropFilePanel.clearSilently();
        });

        // Limitar altura del drag&drop para dar más espacio a los archivos existentes
        dragDropFilePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        dragDropFilePanel.setPreferredSize(new Dimension(600, 120));

        // Panel inferior: Lista de archivos adjuntos existentes CON PREVIEW
        JPanel existingPanel = new JPanel(new BorderLayout());
        existingPanel.setBorder(BorderFactory.createTitledBorder(I18n.get("task.form.attach.existing")));
        existingPanel.setPreferredSize(new Dimension(600, 400)); // ✅ Más espacio para archivos existentes

        // Preview label (like QuickView)
        JLabel lblAttachmentPreview = new JLabel(I18n.get("task.form.attach.preview"), SwingConstants.CENTER);
        lblAttachmentPreview.setPreferredSize(new Dimension(400, 350)); // ✅ Preview más grande
        lblAttachmentPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // List selection listener to update preview
        listAttachments.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateAttachmentPreview(lblAttachmentPreview);
            }
        });

        listAttachments.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && downloadHandler != null) {
                    Object selected = listAttachments.getSelectedValue();
                    if (selected instanceof Attachment) {
                        downloadHandler.download((Attachment) selected);
                    }
                }
                if (SwingUtilities.isRightMouseButton(e) && downloadHandler != null) {
                    listAttachments.setSelectedIndex(listAttachments.locationToIndex(e.getPoint()));
                    Object selected = listAttachments.getSelectedValue();
                    if (selected instanceof Attachment) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem miOpen = new JMenuItem(I18n.get("task.form.menu.open"));
                        miOpen.addActionListener(ev -> downloadHandler.download((Attachment) selected));
                        menu.add(miOpen);
                        if (((Attachment) selected).contentUrl != null) {
                            JMenuItem miWeb = new JMenuItem(I18n.get("task.form.menu.copy_link"));
                            miWeb.addActionListener(ev -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                                    new StringSelection(((Attachment) selected).contentUrl), null));
                            menu.add(miWeb);
                        }
                        menu.show(listAttachments, e.getX(), e.getY());
                    }
                }
            }
        });

        // Layout similar to QuickView: List on left, Preview on right
        JScrollPane spList = new JScrollPane(listAttachments);
        spList.setPreferredSize(new Dimension(200, 0));
        JScrollPane spPreview = new JScrollPane(lblAttachmentPreview);

        existingPanel.add(spList, BorderLayout.WEST);
        existingPanel.add(spPreview, BorderLayout.CENTER);

        // Layout: Drag & Drop arriba, archivos existentes abajo
        p.add(dragDropFilePanel, BorderLayout.NORTH);
        p.add(existingPanel, BorderLayout.CENTER);

        return p;
    }

    private JPanel createTimePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        p.add(new JLabel(I18n.get("task.form.label.time_user")), g);
        g.gridx = 1;
        p.add(cbTimeUser, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("task.form.label.time_date")), g);
        JPanel dateP = new JPanel(new BorderLayout());
        txtTimeDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        JButton btnPick = new JButton("📅");
        btnPick.addActionListener(e -> {
            DatePickerPopup popup = new DatePickerPopup(d -> txtTimeDate.setText(d));
            popup.show(btnPick, 0, btnPick.getHeight());
        });
        dateP.add(txtTimeDate, BorderLayout.CENTER);
        dateP.add(btnPick, BorderLayout.EAST);
        g.gridx = 1;
        p.add(dateP, g);
        g.gridx = 0;
        g.gridy++;
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("task.form.label.time_hours")), g);
        JPanel hoursPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hoursPanel.add(txtTimeHours);
        hoursPanel.add(Box.createHorizontalStrut(5));
        JButton btnPlusHalf = new JButton("+0.5");
        btnPlusHalf.setMargin(new Insets(2, 4, 2, 4));
        btnPlusHalf.addActionListener(e -> {
            try {
                String t = txtTimeHours.getText().trim();
                if (t.isEmpty())
                    t = "0";
                double val = Double.parseDouble(t.replace(",", "."));
                txtTimeHours.setText(String.valueOf(val + 0.5));
            } catch (Exception ex) {
                txtTimeHours.setText("0.5");
            }
        });
        JButton btn7 = new JButton("7h");
        btn7.setMargin(new Insets(2, 4, 2, 4));
        btn7.addActionListener(e -> txtTimeHours.setText("7"));
        JButton btn85 = new JButton("8.5h");
        btn85.setMargin(new Insets(2, 4, 2, 4));
        btn85.addActionListener(e -> txtTimeHours.setText("8.5"));
        hoursPanel.add(btnPlusHalf);
        hoursPanel.add(Box.createHorizontalStrut(3));
        hoursPanel.add(btn7);
        hoursPanel.add(Box.createHorizontalStrut(3));
        hoursPanel.add(btn85);

        g.gridx = 1;
        p.add(hoursPanel, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("task.form.label.time_activity")), g);
        g.gridx = 1;
        p.add(cbActivity, g);
        g.gridx = 0;
        g.gridy++;
        p.add(new JLabel(I18n.get("task.form.label.time_comment")), g);
        g.gridx = 1;
        p.add(txtTimeComment, g);
        g.gridx = 0;
        g.gridy++;
        g.gridwidth = 2;
        btnLogTime.setBackground(new Color(220, 240, 220));
        btnLogTime.addActionListener(e -> {
            try {
                double h = Double.parseDouble(txtTimeHours.getText().replace(",", "."));
                String dStr = txtTimeDate.getText().trim();
                Date d = new SimpleDateFormat("dd/MM/yyyy").parse(dStr);
                String isoDate = new SimpleDateFormat("yyyy-MM-dd").format(d);
                int uid = ((SimpleEntity) cbTimeUser.getSelectedItem()).id;
                int actId = 0;
                if (cbActivity.getSelectedItem() != null)
                    actId = ((SimpleEntity) cbActivity.getSelectedItem()).id;
                String comment = txtTimeComment.getText();
                if (timeListener != null)
                    timeListener.log(isoDate, h, uid, actId, comment);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, I18n.get("task.form.error.time_format"),
                        I18n.get("notification.title.error"),
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, I18n.format("task.form.error.data", ex.getMessage()),
                        I18n.get("notification.title.error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        p.add(btnLogTime, g);
        
        wrapper.add(p, BorderLayout.NORTH);
        return wrapper;
    }

    public void appendSubjectSuffix(String suffix) {
        txtSubject.setText(txtSubject.getText() + suffix);
    }

    public void prependSubjectPrefix(String prefix) {
        txtSubject.setText(prefix + txtSubject.getText());
    }

    private void fillCombo(JComboBox<SimpleEntity> cb, List<SimpleEntity> l) {
        if (l != null)
            l.forEach(cb::addItem);
    }

    public void onSave(Consumer<Task> l) {
        // Wrap the listener to automatically handle pasted images
        saveListener = task -> {
            LoggerUtil.logDebug("TaskFormDialog", "onSave wrapper called");

            // Get pasted images from comment area
            java.util.List<File> pastedImages = getPastedImages();
            LoggerUtil.logDebug("TaskFormDialog", "Pasted images count: " + pastedImages.size());

            // Upload pasted images as attachments
            if (!pastedImages.isEmpty() && uploadHandler != null) {
                LoggerUtil.logDebug("TaskFormDialog", "Uploading " + pastedImages.size() + " pasted images");
                for (File imageFile : pastedImages) {
                    try {
                        LoggerUtil.logDebug("TaskFormDialog", "Uploading image: " + imageFile.getName());
                        // Upload the image
                        UploadToken token = uploadHandler.upload(imageFile);
                        LoggerUtil.logDebug("TaskFormDialog", "Upload successful, token: " + token.token);

                        // Add to task's pending uploads
                        if (task.pendingUploads == null) {
                            task.pendingUploads = new java.util.ArrayList<>();
                        }
                        task.pendingUploads.add(token);
                        LoggerUtil.logDebug("TaskFormDialog",
                                "Added to pending uploads. Total: " + task.pendingUploads.size());

                    } catch (Exception ex) {
                        // Log error but continue with other images
                        LoggerUtil.logError("TaskFormDialog",
                                "Error uploading pasted image: " + imageFile.getName() + " - " + ex.getMessage(), ex);
                    }
                }
            } else {
                if (pastedImages.isEmpty()) {
                    LoggerUtil.logDebug("TaskFormDialog", "No pasted images to upload");
                }
                if (uploadHandler == null) {
                    LoggerUtil.logWarning("TaskFormDialog", "uploadHandler is null");
                }
            }

            // Get comment text with Redmine image references
            String commentWithImages = getCommentTextWithImageReferences();
            LoggerUtil.logDebug("TaskFormDialog", "Comment with images markup: " + commentWithImages);

            // Update comment if present
            if (!commentWithImages.isEmpty()) {
                task.comment = commentWithImages;
            }

            // Update description with image references (for new tasks or editing)
            String descWithImages = getDescriptionTextWithImageReferences();
            LoggerUtil.logDebug("TaskFormDialog", "Description with images markup: " + descWithImages);
            if (descWithImages != null) {
                task.description = descWithImages;
            }

            LoggerUtil.logDebug("TaskFormDialog", "Final task comment: " + task.comment);

            // Clear pasted images after successful processing
            clearPastedImages();
            LoggerUtil.logDebug("TaskFormDialog", "Cleared pasted images");

            // Call the original listener
            LoggerUtil.logDebug("TaskFormDialog", "Calling original save listener");
            if (l != null) {
                l.accept(task);
            }
            LoggerUtil.logDebug("TaskFormDialog", "onSave wrapper complete");
        };
    }

    public void onLogTime(TimeLogger l) {
        timeListener = l;
    }

    public void onUpload(UploadHandler h) {
        this.uploadHandler = h;
    }

    public void onDownload(DownloadHandler h) {
        this.downloadHandler = h;
    }

    public void setAsyncDataService(redmineconnector.service.AsyncDataService service) {
        this.asyncService = service;
        // Trigger update if we have a tracker selected
        SimpleEntity tracker = (SimpleEntity) cbTracker.getSelectedItem();
        if (tracker != null) {
            updateContext(tracker.id);
        }
    }

    private void updateContext(int trackerId) {
        LoggerUtil.logDebug("TaskFormDialog", "updateContext called for trackerId: " + trackerId);

        if (asyncService == null) {
            return;
        }

        String pid = contextProject != null ? String.valueOf(contextProject.id) : "";
        int effectiveIssueId = 0;
        if (isEditMode && loadedTask != null && loadedTask.trackerId == trackerId) {
            effectiveIssueId = loadedTask.id;
        }

        cbStatus.setEnabled(false);

        // Fetch strict context metadata (Statuses AND Custom Fields)
        asyncService.fetchContextMetadataAsync(pid, trackerId, effectiveIssueId)
                .thenAccept(metadata -> SwingUtilities.invokeLater(() -> {
                    // 1. Update Statuses
                    if (metadata.allowedStatuses != null && !metadata.allowedStatuses.isEmpty()) {
                        cbStatus.removeAllItems();
                        for (SimpleEntity s : metadata.allowedStatuses) {
                            cbStatus.addItem(s);
                        }
                        if (cbStatus.getItemCount() > 0)
                            cbStatus.setSelectedIndex(0);
                    } else {
                        // If no statuses returned (rare), maybe fallback or just leave as is
                        if (cbStatus.getItemCount() == 0 && allStatuses != null) {
                            for (SimpleEntity s : allStatuses)
                                cbStatus.addItem(s);
                        }
                    }
                    cbStatus.setEnabled(true);

                    // 2. Update Custom Fields Constraints
                    allowedCustomFieldIds.clear();
                    if (metadata.availableCustomFieldIds != null) {
                        allowedCustomFieldIds.addAll(metadata.availableCustomFieldIds);
                        isContextRestrictionActive = true;
                        LoggerUtil.logDebug("TaskFormDialog",
                                "Context updated. Allowed CF IDs: " + allowedCustomFieldIds);
                    } else {
                        // Even if null, we got a response, so restrict to empty?
                        // Or if null means 'not provided'?
                        // Redmine /issues/new always returns custom_fields if any are available.
                        // If none available, it might match empty list.
                        // Let's assume successful metadata fetch implies strictness if list is present
                        // (even empty)
                        if (metadata.availableCustomFieldIds != null) {
                            isContextRestrictionActive = true;
                        }
                    }

                    // CRITICAL FIX: Update definitions if provided by metadata.
                    // This ensures we have the correct Possible Values for the current context,
                    // preventing fields from incorrectly rendering as TextFields.
                    if (metadata.definitions != null && !metadata.definitions.isEmpty()) {
                        this.customFieldDefs = metadata.definitions;
                        LoggerUtil.logDebug("TaskFormDialog", "Updated customFieldDefs from ContextMetadata ("
                                + metadata.definitions.size() + " items)");
                    }

                    // 3. Rebuild Custom Fields UI with new constraints
                    buildCustomFieldsUI();

                    // 3.5. APPLY SMART DEFAULTS (Essential for New Tasks)
                    resetCustomFields();

                    // 4. Restore values if editing an existing task
                    if (loadedTask != null) {
                        fillCustomFields(loadedTask);
                    }

                })).exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        if (ex instanceof Exception) {
                            LoggerUtil.logError("TaskFormDialog", "Error fetching context metadata", (Exception) ex);
                        } else {
                            LoggerUtil.logError("TaskFormDialog", "Error fetching context metadata: " + ex.getMessage(),
                                    null);
                        }
                        cbStatus.setEnabled(true);
                        // In case of error, maybe clear strict filter so user can see everything?
                        // allowedCustomFieldIds.clear(); // Empty means "strict filter active but
                        // matching nothing" OR "not loaded"?
                        // Let's decide: Empty allowedCustomFieldIds means "Strict mode enabled, naught
                        // allowed"
                        // BUT if fetch fails, maybe we should let them see all?
                        // For now, let's keep it safe. If fetch fails, we might miss validations.
                    });
                    return null;
                });
    }

    private void applyFallback(int trackerId, List<SimpleEntity> backupItems, SimpleEntity currentSelection) {
        cbStatus.removeAllItems();
        List<SimpleEntity> fallback = null;

        // 1. Heuristic
        if (statusHeuristic != null && statusHeuristic.containsKey(trackerId)) {
            fallback = statusHeuristic.get(trackerId);
        }

        // 2. New Only
        if ((fallback == null || fallback.isEmpty()) && allStatuses != null) {
            List<SimpleEntity> newOnly = new ArrayList<>();
            for (SimpleEntity s : allStatuses) {
                String sn = (s.name != null) ? s.name.trim() : "";
                if (sn.equalsIgnoreCase("Nueva") || sn.equalsIgnoreCase("New") || sn.equalsIgnoreCase("Nuevo")
                        || sn.equalsIgnoreCase("Inicial")) {
                    newOnly.add(s);
                }
            }
            if (!newOnly.isEmpty())
                fallback = newOnly;
        }

        // 3. Backup
        if (fallback == null && !backupItems.isEmpty())
            fallback = backupItems;

        // 4. All Statuses
        if (fallback == null || fallback.isEmpty())
            fallback = allStatuses;

        if (fallback != null) {
            for (SimpleEntity s : fallback) {
                cbStatus.addItem(s);
            }
        }

        if (currentSelection != null) {
            setCombo(cbStatus, currentSelection.id);
        } else if (cbStatus.getItemCount() > 0) {
            cbStatus.setSelectedIndex(0);
        }
    }

    public void setDataService(redmineconnector.service.DataService service) {
        LoggerUtil.logDebug("TaskFormDialog", "setDataService() called");
        LoggerUtil.logDebug("TaskFormDialog", "Service received: " + service);
        this.dataService = service;
        LoggerUtil.logDebug("TaskFormDialog", "dataService configured: " + this.dataService);

        LoggerUtil.logDebug("TaskFormDialog", "dataService configured: " + this.dataService);

        // Fetch custom fields definitions
        SwingWorker<List<CustomFieldDefinition>, Void> worker = new SwingWorker<List<CustomFieldDefinition>, Void>() {
            @Override
            protected List<CustomFieldDefinition> doInBackground() throws Exception {
                return dataService.fetchCustomFieldDefinitions();
            }

            @Override
            protected void done() {
                try {
                    customFieldDefs = get();
                    LoggerUtil.logDebug("TaskFormDialog",
                            "Fetched " + (customFieldDefs != null ? customFieldDefs.size() : "null")
                                    + " custom field definitions");
                    buildCustomFieldsUI();
                    if (loadedTask != null) {
                        fillCustomFields(loadedTask);
                    }
                } catch (Exception e) {
                    LoggerUtil.logError("TaskFormDialog", "Error fetching custom fields", e);

                    // Fallback for non-admin users:
                    // 1. Try to load from local cache (learned from other tasks) for THIS CONTEXT
                    List<CustomFieldDefinition> cached = redmineconnector.service.CustomFieldsCache
                            .getDefinitions(contextUrl);
                    if (!cached.isEmpty()) {
                        LoggerUtil.logDebug("TaskFormDialog",
                                "Using cached custom field definitions (" + cached.size() + ") from " + contextUrl);
                        customFieldDefs = cached;
                    } else {
                        // 2. If locally empty, build from current task (last resort)
                        if (loadedTask != null && loadedTask.customFields != null
                                && !loadedTask.customFields.isEmpty()) {
                            LoggerUtil.logDebug("TaskFormDialog", "Falling back to local custom fields from task");
                            customFieldDefs = new ArrayList<>();
                            for (CustomField cf : loadedTask.customFields) {
                                // Assume string type and optional as we don't have metadata
                                CustomFieldDefinition def = new CustomFieldDefinition(cf.id, cf.name, "string", false);
                                if (loadedTask.trackerId > 0) {
                                    def.trackerIds.add(loadedTask.trackerId);
                                }
                                customFieldDefs.add(def);
                            }
                        }
                    }

                    // Always show warning if we are in restricted mode (no direct API access)
                    if (customFieldDefs == null)
                        customFieldDefs = new ArrayList<>();

                    // Add a special definition to signal the UI to show a warning
                    CustomFieldDefinition warningDef = new CustomFieldDefinition(-1, "WARNING_403", "warning_label",
                            false);
                    customFieldDefs.add(0, warningDef);

                    buildCustomFieldsUI();
                    fillCustomFields(loadedTask);
                }
            }
        };
        worker.execute();

        // Si ya se cargó una tarea (fill() se llamó antes), pre-descargar imágenes
        // ahora
        if (loadedTask != null && loadedTask.attachments != null && !loadedTask.attachments.isEmpty()) {
            LoggerUtil.logDebug("TaskFormDialog", "Tarea ya cargada, pre-descargando imágenes ahora...");
            txtComment.setAvailableAttachments(loadedTask.attachments);
            preDownloadImages(loadedTask.attachments);
        }
    }

    public void setUploadRoot(File f) {
        if (f != null && f.exists())
            this.uploadRoot = f;
    }

    public void setDefaultTimeUser(SimpleEntity u) {
        this.defaultTimeUser = u;
    }

    public void fill(Task t) {
        LoggerUtil.logDebug("TaskFormDialog", "fill() called");
        LoggerUtil.logDebug("TaskFormDialog", "Task ID: " + t.id);
        LoggerUtil.logDebug("TaskFormDialog", "Incoming Parent ID: " + t.parentId);
        LoggerUtil.logDebug("TaskFormDialog",
                "Attachments: " + (t.attachments != null ? t.attachments.size() : "null"));

        loadedTask = t;
        boolean isNew = (t.id == 0);
        tabs.setEnabledAt(3, !isNew);
        tabs.setToolTipTextAt(3, isNew ? I18n.get("task.form.tooltip.save_first") : null);
        txtSubject.setText(t.subject);
        // Moved txtDesc.setText to after attachments setup
        txtComment.setText(""); // Explicitly clear comment field
        setCombo(cbTracker, t.trackerId);
        setCombo(cbStatus, t.statusId);
        setCombo(cbPriority, t.priorityId);
        setCombo(cbAssigned, t.assignedToId);
        setCombo(cbCategory, t.categoryId);
        setCombo(cbVersion, t.targetVersionId);
        if (t.parentId > 0) {
            cbParent.setSelectedById(t.parentId);
        } else {
            cbParent.clearSelection();
            LoggerUtil.logDebug("TaskFormDialog", "Parent ID is 0, clearing selection");
        }

        if (defaultTimeUser != null) {
            setCombo(cbTimeUser, defaultTimeUser.id);
        } else if (t.assignedToId > 0) {
            setCombo(cbTimeUser, t.assignedToId);
        }
        cbDoneRatio.setSelectedItem(t.doneRatio + " %");

        // CRITICAL FIX: Reset and Fill Custom Fields
        // 1. Reset cleans up previous state and sets "Smart Default" (Index 0) for new
        // tasks.
        resetCustomFields();
        // 2. Fill populates with actual task values if they exist.
        fillCustomFields(t);
        if (t.doneRatio >= 0) {
            cbDoneRatio.setSelectedItem(t.doneRatio + " %");
        }

        attachmentModel.clear();
        // Detect Cloning Scenario: Source Service present + New Task + Has Attachments
        if (sourceDataService != null && t.id == 0 && t.attachments != null && !t.attachments.isEmpty()) {
            // We are cloning. Migrate attachments.
            // COPY the list because we might clear t.attachments to avoid showing them as "remote"
            // (or we show them as pending uploads after migration).
            // Actually, if we leave them in t.attachments, they might be shown as existing remote attachments
            // which is wrong because they don't exist in TARGET yet.
            List<Attachment> toMigrate = new java.util.ArrayList<>(t.attachments);
            t.attachments.clear(); // Clear remote refs so they don't appear in "Existing Attachments" list incorrectly
            
            migrateAttachments(toMigrate);
        } else if (t.attachments != null) {
            for (Attachment a : t.attachments) {
                attachmentModel.addElement(a);
            }
        }
        if (t.pendingUploads != null) {
            for (UploadToken ut : t.pendingUploads)
                attachmentModel.addElement(ut);
        }

        // Configurar download handler para el editor de comentarios
        LoggerUtil.logDebug("TaskFormDialog", "Configuring download handler");
        LoggerUtil.logDebug("TaskFormDialog", "dataService != null: " + (dataService != null));
        LoggerUtil.logDebug("TaskFormDialog", "t.attachments != null: " + (t.attachments != null));

        if (t.attachments != null) {
            // Configurar ambos editores con los adjuntos disponibles
            txtComment.setAvailableAttachments(t.attachments);
            txtDesc.setAvailableAttachments(t.attachments);

            DragDropImageTextPane.ImageDownloadHandler handler = (att, destFile) -> {
                if (dataService != null) {
                    try {
                        byte[] data = dataService.downloadAttachment(att);
                        if (data != null && data.length > 0) {
                            java.nio.file.Files.write(destFile.toPath(), data);
                        }
                    } catch (Exception e) {
                        LoggerUtil.logError("TaskFormDialog", "Error downloading image for editor", e);
                    }
                }
            };

            txtComment.setDownloadHandler(handler);
            txtDesc.setDownloadHandler(handler);

            // Pre-descargar todas las imágenes en background SI tenemos dataService
            if (dataService != null) {
                LoggerUtil.logDebug("TaskFormDialog", "Calling preDownloadImages()");
                preDownloadImages(t.attachments);
            } else {
                LoggerUtil.logDebug("TaskFormDialog", "NO se llamará a preDownloadImages - dataService es null");
            }
        } else {
            LoggerUtil.logDebug("TaskFormDialog", "No attachments");
        }

        // Set description AFTER configuring attachments to allow inline image
        // resolution
        txtDesc.setText(t.description);

        // Limpiar placeholders de imágenes anteriores
        imagePlaceholders.clear();

        refreshHistory(t);

        StringBuilder revHtml = new StringBuilder("<html><body style='font-family:Segoe UI; font-size:12px;'>");
        if (t.changesets != null && !t.changesets.isEmpty()) {
            revHtml.append(I18n.get("task.form.history.revisions"));
            String baseRepoUrl = t.webUrl != null ? t.webUrl.replace("/issues/" + t.id, "/projects") : "";
            for (Changeset c : t.changesets) {
                revHtml.append(I18n.format("task.form.history.revision_item", c.revision, c.user, c.committedOn));
                String revUrl = "#";
                boolean urlGenerated = false;
                if (contextUrl != null && !contextUrl.isEmpty() && contextProject != null) {
                    revUrl = String.format("%s/projects/%d/repository/revisions/%s", contextUrl, contextProject.id,
                            c.revision);
                    urlGenerated = true;
                } else if (baseRepoUrl.length() > 0) {
                    revUrl = baseRepoUrl + "/repository/revisions/" + c.revision;
                    urlGenerated = true;
                }
                if (urlGenerated) {
                    revHtml.append("<a href='").append(revUrl).append("'>")
                            .append(I18n.format("task.form.history.open_revision", c.revision))
                            .append("</a>");
                }
                revHtml.append("<br><i>").append(c.comments != null ? c.comments.replace("\n", "<br>") : "")
                        .append("</i></li><br>");
            }
            revHtml.append("</ul>");
        } else {
            revHtml.append(I18n.get("task.form.history.no_revisions"));
        }
        revHtml.append("</body></html>");
        txtRevisions.setText(revHtml.toString());
        txtRevisions.setCaretPosition(0);

        if (t.author != null && !t.author.isEmpty()) {
            lblAuthor.setText(I18n.format("task.form.label.created_by", t.author));
            lblAuthor.setVisible(true);
            if (t.authorEmail != null && !t.authorEmail.isEmpty()) {
                lblAuthorEmail.setText("(" + t.authorEmail + ")");
            } else {
                lblAuthorEmail.setText("");
            }
        } else {
            lblAuthor.setVisible(false);
            lblAuthorEmail.setVisible(false);
        }
    }

    public void setDescription(String s) {
        txtDesc.setText(s);
    }

    private void setCombo(JComboBox<SimpleEntity> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++)
            if (cb.getItemAt(i).id == id) {
                cb.setSelectedIndex(i);
                return;
            }
    }

    /**
     * Obtiene el texto del comentario con referencias de imagen en formato Redmine.
     * Las imágenes pegadas se reemplazan por !filename.png!
     * 
     * @return texto del comentario con markup de Redmine
     */
    public String getCommentTextWithImageReferences() {
        return txtComment.getTextWithImageReferences();
    }

    public String getDescriptionTextWithImageReferences() {
        return txtDesc.getTextWithImageReferences();
    }

    /**
     * Obtiene la lista de imágenes pegadas en la descripción y comentario.
     * Estas imágenes deben ser subidas como adjuntos.
     * 
     * @return lista de archivos de imagen
     */
    public java.util.List<File> getPastedImages() {
        java.util.List<File> allImages = new java.util.ArrayList<>();
        allImages.addAll(txtDesc.getPastedImages());
        allImages.addAll(txtComment.getPastedImages());
        return allImages;
    }

    /**
     * Limpia la lista de imágenes pegadas después de guardar.
     */
    public void clearPastedImages() {
        txtDesc.clearPastedImages();
        txtComment.clearPastedImages();
    }

    /**
     * Pre-descarga todas las imágenes de los adjuntos en background.
     * Esto permite que se muestren inline cuando se carga el comentario.
     */
    private void preDownloadImages(java.util.List<Attachment> attachments) {
        LoggerUtil.logDebug("TaskFormDialog", "preDownloadImages() started");
        LoggerUtil.logDebug("TaskFormDialog",
                "Attachments received: " + (attachments != null ? attachments.size() : "null"));

        if (attachments == null || attachments.isEmpty()) {
            LoggerUtil.logDebug("TaskFormDialog", "No attachments, exiting");
            return;
        }

        // Filtrar solo imágenes
        java.util.List<Attachment> imageAttachments = new java.util.ArrayList<>();
        for (Attachment att : attachments) {
            if (att.filename != null && isImageFile(att.filename)) {
                imageAttachments.add(att);
            }
        }

        if (imageAttachments.isEmpty()) {
            return;
        }

        // Descargar en background
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                String tempDir = System.getProperty("java.io.tmpdir");

                for (Attachment att : imageAttachments) {
                    try {
                        File destFile = new File(tempDir, att.filename);

                        // Solo descargar si no existe
                        if (!destFile.exists()) {
                            publish("Descargando: " + att.filename);

                            // Usar el downloadHandler para obtener los datos
                            // Esto es un workaround - idealmente tendríamos acceso directo al DataService
                            byte[] data = downloadAttachmentBlocking(att);

                            if (data != null && data.length > 0) {
                                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile)) {
                                    fos.write(data);
                                }
                                publish("✓ Descargado: " + att.filename);
                            }
                        }
                    } catch (Exception e) {
                        publish("✗ Error: " + att.filename + " - " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                // Opcional: mostrar progreso en consola
                for (String msg : chunks) {
                    LoggerUtil.logDebug("TaskFormDialog", "[Image Download] " + msg);
                }
            }

            @Override
            protected void done() {
                // Las imágenes ya están descargadas en temp
                LoggerUtil.logDebug("TaskFormDialog", "Image pre-download completed");

                // ✅ NUEVO: Refrescar el historial para que las imágenes se muestren inline
                if (loadedTask != null && loadedTask.journals != null && !loadedTask.journals.isEmpty()) {
                    LoggerUtil.logDebug("TaskFormDialog", "Refreshing history to show downloaded images");
                    SwingUtilities.invokeLater(() -> {
                        // Regenerar solo el HTML del historial (no todo el formulario)
                        refreshHistory(loadedTask);
                    });
                }
                // Also refresh editor panes to show inline images
                txtDesc.refreshImages();
                txtComment.refreshImages();
            }
        };

        worker.execute();
    }

    /**
     * Verifica si un archivo es una imagen por su extensión.
     */
    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") || lower.endsWith(".gif") ||
                lower.endsWith(".bmp");
    }

    /**
     * Descarga un adjunto de forma bloqueante usando DataService.
     */
    private byte[] downloadAttachmentBlocking(Attachment att) throws Exception {
        if (dataService == null) {
            throw new IllegalStateException("DataService not configured");
        }

        // Descargar directamente usando el DataService
        // Descargar directamente usando el DataService
        return dataService.downloadAttachment(att);
    }

    private void migrateAttachments(List<Attachment> sourceAttachments) {
        if (sourceAttachments == null || sourceAttachments.isEmpty() || sourceDataService == null)
             return;

        if (uploadHandler == null) {
            redmineconnector.util.LoggerUtil.logError("TaskFormDialog", "Cannot migrate attachments: UploadHandler is null", null);
            return;
        }

        redmineconnector.util.LoggerUtil.logDebug("TaskFormDialog", "Starting migration of " + sourceAttachments.size() + " attachments...");

        SwingUtilities.invokeLater(() -> {
            // Show loading dialog if needed, or just let the user see the progress bars appearing
            // Ideally we should block or show an overlay, but for now we'll do it async
            
            // We use a SwingWorker to download and re-upload to avoid freezing UI
            new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String tempDir = System.getProperty("java.io.tmpdir");
                    for (Attachment att : sourceAttachments) {
                         try {
                              publish("Migrando: " + att.filename + "...");
                              redmineconnector.util.LoggerUtil.logDebug("TaskFormDialog", "Downloading source attachment: " + att.filename);
                              
                              byte[] data = sourceDataService.downloadAttachment(att);
                              if(data == null || data.length == 0) continue;
                              
                              File tempFile = new File(tempDir, att.filename);
                              try(java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                                  fos.write(data);
                              }
                              
                              redmineconnector.util.LoggerUtil.logDebug("TaskFormDialog", "Uploading to target: " + att.filename);
                              
                              // We need to call uploadHandler on EDT if it touches UI, BUT standard uploadHandler
                              // might be network blocking. The interface says "throws Exception", implying blocking.
                              // Let's call it here. IF it updates UI, it should handle EDT internally or be thread-safe.
                              // The implementation in DialogManager (lines 229-241) updates UI (controller.log) which is fine.
                              
                              UploadToken token = uploadHandler.upload(tempFile);
                              if(token != null) {
                                  // Modifying model must be on EDT
                                  SwingUtilities.invokeLater(() -> {
                                      loadedTask.pendingUploads.add(token);
                                      attachmentModel.addElement(token);
                                      // Force refresh of description if it contains inline images
                                      txtDesc.refreshImages();
                                  });
                              }
                              
                              // Clean up
                              tempFile.delete();
                              
                         } catch (Exception e) {
                             redmineconnector.util.LoggerUtil.logError("TaskFormDialog", "Error migrating " + att.filename, e);
                         }
                    }
                    return null;
                }
                
                @Override
                protected void process(List<String> chunks) {
                    // Could update a status label if we had one
                }
            }.execute();
        });
    }

    // Map to store image placeholders and their corresponding HTML tags
    private java.util.Map<String, String> imagePlaceholders = new java.util.HashMap<>();

    /**
     * Reemplaza markup de imágenes de Redmine (!imagen.png!) con placeholders
     * únicos
     * que no serán escapados por Textile.
     * Los placeholders se reemplazan con tags <img> DESPUÉS de la conversión
     * Textile.
     */
    private String replaceRedmineImageMarkup(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String tempDir = System.getProperty("java.io.tmpdir");

        // Buscar !filename.ext! y reemplazar con placeholders
        // Support both Textile (!img!) and Markdown (![] (img))
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "!([^!\\n]+\\.(?:png|jpg|jpeg|gif|bmp))!|!\\[.*?\\]\\(([^)]+\\.(?:png|jpg|jpeg|gif|bmp))\\)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            // Check which group matched
            String filename = matcher.group(1); // Textile
            if (filename == null) {
                filename = matcher.group(2); // Markdown
            }
            File imageFile = new File(tempDir, filename);

            LoggerUtil.logDebug("TaskFormDialog", "[Image Markup] Searching: " + filename);
            LoggerUtil.logDebug("TaskFormDialog", "[Image Markup] Path: " + imageFile.getAbsolutePath());
            LoggerUtil.logDebug("TaskFormDialog", "[Image Markup] Exists: " + imageFile.exists());

            if (imageFile.exists()) {
                // Crear un placeholder único que Textile no modificará
                // Usamos un formato UUID-like con solo caracteres alfanuméricos
                String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
                String placeholder = "XXIMGPLACEHOLDERXX" + uuid + "XX";

                // Guardar el tag HTML correspondiente
                String imgTag = "<img src=\"file:///" +
                        imageFile.getAbsolutePath().replace("\\", "/") +
                        "\" style='max-width:600px; height:auto;' />";
                imagePlaceholders.put(placeholder, imgTag);

                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(placeholder));
                LoggerUtil.logDebug("TaskFormDialog", "[Image Markup] Replaced with placeholder: " + placeholder);
            } else {
                // Dejar el markup original si no existe la imagen
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                LoggerUtil.logDebug("TaskFormDialog", "[Image Markup] Image not found, leaving original markup");
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Reemplaza los placeholders de imágenes con los tags <img> reales.
     * Debe llamarse DESPUÉS de la conversión Textile.
     */
    private String replacePlaceholdersWithImages(String html) {
        if (html == null || html.isEmpty() || imagePlaceholders.isEmpty()) {
            return html;
        }

        String result = html;
        LoggerUtil.logDebug("TaskFormDialog",
                "[Image Placeholder] Replacing " + imagePlaceholders.size() + " placeholders");
        for (java.util.Map.Entry<String, String> entry : imagePlaceholders.entrySet()) {
            String before = result;
            result = result.replace(entry.getKey(), entry.getValue());
            if (!before.equals(result)) {
                LoggerUtil.logDebug("TaskFormDialog", "[Image Placeholder] ✓ Reemplazado: " + entry.getKey());
            } else {
                LoggerUtil.logDebug("TaskFormDialog", "[Image Placeholder] ✗ NO encontrado: " + entry.getKey());
                LoggerUtil.logDebug("TaskFormDialog", "[Image Placeholder] Searching in HTML (first 500 chars): "
                        + html.substring(0, Math.min(500, html.length())));
            }
        }

        return result;
    }

    /**
     * Reemplaza referencias de imágenes en HTML con tags <img> que apuntan a
     * archivos locales.
     * Busca patrones como <img src="imagen.png" /> o !imagen.png! y los reemplaza.
     */
    private String replaceImageReferencesWithHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        String tempDir = System.getProperty("java.io.tmpdir");
        String result = html;

        // Patrón 1: <img src="filename.ext" /> (generado por TextileConverter)
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
                "<img\\s+src=\"([^\"]+\\.(png|jpg|jpeg|gif|bmp))\"[^>]*/?>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher imgMatcher = imgPattern.matcher(result);
        StringBuffer sb1 = new StringBuffer();
        while (imgMatcher.find()) {
            String filename = imgMatcher.group(1);
            // Extraer solo el nombre del archivo si es una ruta
            if (filename.contains("/")) {
                filename = filename.substring(filename.lastIndexOf("/") + 1);
            }
            File imageFile = new File(tempDir, filename);
            if (imageFile.exists()) {
                String replacement = "<img src=\"file:///" +
                        imageFile.getAbsolutePath().replace("\\", "/") +
                        "\" style='max-width:600px; height:auto;' />";
                imgMatcher.appendReplacement(sb1, java.util.regex.Matcher.quoteReplacement(replacement));
            }
        }
        imgMatcher.appendTail(sb1);
        result = sb1.toString();

        // Patrón 2: !filename.ext! OR ![](filename.ext) (markup de Redmine/Markdown)
        java.util.regex.Pattern redminePattern = java.util.regex.Pattern.compile(
                "!([^!\\n]+\\.(?:png|jpg|jpeg|gif|bmp))!|!\\[.*?\\]\\(([^)]+\\.(?:png|jpg|jpeg|gif|bmp))\\)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher redmineMatcher = redminePattern.matcher(result);
        StringBuffer sb2 = new StringBuffer();
        while (redmineMatcher.find()) {
            String filename = redmineMatcher.group(1);
            if (filename == null) filename = redmineMatcher.group(2);
            
            File imageFile = new File(tempDir, filename);
            if (imageFile.exists()) {
                String replacement = "<img src=\"file:///" +
                        imageFile.getAbsolutePath().replace("\\", "/") +
                        "\" style='max-width:600px; height:auto;' />";
                redmineMatcher.appendReplacement(sb2, java.util.regex.Matcher.quoteReplacement(replacement));
            } else {
                // Si no existe, dejar el markup original
                redmineMatcher.appendReplacement(sb2,
                        java.util.regex.Matcher.quoteReplacement(redmineMatcher.group(0)));
            }
        }
        redmineMatcher.appendTail(sb2);

        return sb2.toString();
    }

    /**
     * Updates the preview of the selected attachment (similar to QuickView)
     */
    private void updateAttachmentPreview(JLabel lblPreview) {
        Object selected = listAttachments.getSelectedValue();
        if (selected == null) {
            lblPreview.setIcon(null);
            lblPreview.setText("Seleccione un adjunto");
            return;
        }

        Attachment att = null;
        if (selected instanceof Attachment) {
            att = (Attachment) selected;
        } else if (selected instanceof UploadToken) {
            // UploadToken doesn't have preview
            lblPreview.setIcon(null);
            lblPreview.setText("<html><center>Archivo subido<br><br><b>" +
                    ((UploadToken) selected).filename + "</b></center></html>");
            return;
        }

        if (att == null)
            return;

        String fname = att.filename != null ? att.filename.toLowerCase() : "";
        boolean isImageFile = fname.endsWith(".png") || fname.endsWith(".jpg") ||
                fname.endsWith(".jpeg") || fname.endsWith(".gif") ||
                fname.endsWith(".bmp");
        boolean isPossibleImg = (att.contentType != null && att.contentType.startsWith("image/")) || isImageFile;

        if (isPossibleImg && !fname.endsWith(".docx") && !fname.endsWith(".pdf") && !fname.endsWith(".zip")) {
            // Try to load image
            lblPreview.setText("Cargando preview...");
            lblPreview.setIcon(null);

            Attachment finalAtt = att;
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    if (dataService == null)
                        return null;

                    // Download image bytes using dataService
                    byte[] data = dataService.downloadAttachment(finalAtt);
                    if (data == null || data.length == 0) {
                        throw new Exception("Sin datos");
                    }

                    // Load image from bytes
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                    if (img == null)
                        return null;

                    // Scale image to fit preview area
                    int maxW = 400;
                    int maxH = 350;
                    double scale = Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight());
                    if (scale < 1.0) {
                        Image scaled = img.getScaledInstance(
                                (int) (img.getWidth() * scale),
                                (int) (img.getHeight() * scale),
                                Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                    return new ImageIcon(img);
                }

                @Override
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        if (icon != null) {
                            lblPreview.setIcon(icon);
                            lblPreview.setText("");
                        } else {
                            lblPreview.setText("Error al cargar imagen");
                        }
                    } catch (Exception e) {
                        String msg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
                        lblPreview.setText("<html><center>Fallo al cargar<br>" + msg + "</center></html>");
                    }
                }
            }.execute();
        } else {
            lblPreview.setIcon(null);
            String extension = fname.contains(".") ? fname.substring(fname.lastIndexOf(".") + 1) : "archivo";
            lblPreview.setText("<html><center>Vista previa no disponible para ." + extension +
                    "<br><br><b>Doble-clic para abrir</b></center></html>");
        }
    }

    /**
     * Configura atajos de teclado para navegación de pestañas (como QuickView)
     */
    private void setupKeyboardShortcuts() {
        // Ctrl+E = Siguiente tab
        KeyStroke nextTabKey = KeyStroke.getKeyStroke("control E");
        tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(nextTabKey, "next_tab");
        tabs.getActionMap().put("next_tab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int current = tabs.getSelectedIndex();
                int next = (current + 1) % tabs.getTabCount();
                tabs.setSelectedIndex(next);
            }
        });

        // Ctrl+W = Tab anterior
        KeyStroke prevTabKey = KeyStroke.getKeyStroke("control W");
        tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(prevTabKey, "prev_tab");
        tabs.getActionMap().put("prev_tab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int current = tabs.getSelectedIndex();
                int prev = (current - 1 + tabs.getTabCount()) % tabs.getTabCount();
                tabs.setSelectedIndex(prev);
            }
        });
    }

    /**
     * Regenera el HTML del historial de notas.
     * Este método se usa para refrescar el historial después de descargar imágenes.
     */
    private void refreshHistory(Task t) {
        StringBuilder sb = new StringBuilder(
                "<html><body style='font-family:Segoe UI; font-size:11px;'><div style='width:720px; max-width:720px; overflow-wrap:anywhere;'>");
        if (t.journals != null && !t.journals.isEmpty()) {
            // Iterate in correct order based on sort preference
            if (sortNotesNewestFirst) {
                // Newest first: iterate in reverse
                for (int i = t.journals.size() - 1; i >= 0; i--) {
                    Journal j = t.journals.get(i);
                    if (j.notes != null && !j.notes.trim().isEmpty()) {
                        sb.append(
                                "<div style='background-color:#EFEFEF; padding:4px; border-bottom:1px solid #CCC;'><b>")
                                .append(j.user != null ? j.user : I18n.get("task.form.history.user"))
                                .append("</b> <span style='color:#666;'>(")
                                .append(j.createdOn).append(")</span></div>");

                        // PRIMERO: Reemplazar markup de imágenes de Redmine con placeholders
                        String notesWithPlaceholders = replaceRedmineImageMarkup(j.notes);

                        // SEGUNDO: Apply Textile conversion to notes (los placeholders sobreviven)
                        String formattedNotes = redmineconnector.util.TextileConverter
                                .convertToHtml(notesWithPlaceholders);

                        // Extract body content (remove outer html/body tags from converter)
                        String bodyContent = formattedNotes;
                        if (formattedNotes.contains("<body")) {
                            int bodyStart = formattedNotes.indexOf(">", formattedNotes.indexOf("<body")) + 1;
                            int bodyEnd = formattedNotes.lastIndexOf("</body>");
                            if (bodyStart > 0 && bodyEnd > bodyStart) {
                                bodyContent = formattedNotes.substring(bodyStart, bodyEnd);
                            }
                        }

                        // TERCERO: Reemplazar placeholders con tags <img> reales
                        bodyContent = replacePlaceholdersWithImages(bodyContent);

                        // CUARTO: Reemplazar cualquier referencia que quedó con rutas locales
                        bodyContent = replaceImageReferencesWithHtmlTags(bodyContent);

                        sb.append("<div style='padding:5px 5px 10px 5px;'>").append(bodyContent)
                                .append("</div>");
                    }
                }
            } else {
                // Oldest first: iterate normally
                for (Journal j : t.journals) {
                    if (j.notes != null && !j.notes.trim().isEmpty()) {
                        sb.append(
                                "<div style='background-color:#EFEFEF; padding:4px; border-bottom:1px solid #CCC;'><b>")
                                .append(j.user != null ? j.user : I18n.get("task.form.history.user"))
                                .append("</b> <span style='color:#666;'>(")
                                .append(j.createdOn).append(")</span></div>");

                        // PRIMERO: Reemplazar markup de imágenes de Redmine con placeholders
                        String notesWithPlaceholders = replaceRedmineImageMarkup(j.notes);

                        // SEGUNDO: Apply Textile conversion to notes (los placeholders sobreviven)
                        String formattedNotes = redmineconnector.util.TextileConverter
                                .convertToHtml(notesWithPlaceholders);

                        // Extract body content (remove outer html/body tags from converter)
                        String bodyContent = formattedNotes;
                        if (formattedNotes.contains("<body")) {
                            int bodyStart = formattedNotes.indexOf(">", formattedNotes.indexOf("<body")) + 1;
                            int bodyEnd = formattedNotes.lastIndexOf("</body>");
                            if (bodyStart > 0 && bodyEnd > bodyStart) {
                                bodyContent = formattedNotes.substring(bodyStart, bodyEnd);
                            }
                        }

                        // TERCERO: Reemplazar placeholders con tags <img> reales
                        bodyContent = replacePlaceholdersWithImages(bodyContent);

                        // CUARTO: Reemplazar cualquier referencia que quedó con rutas locales
                        bodyContent = replaceImageReferencesWithHtmlTags(bodyContent);

                        sb.append("<div style='padding:5px 5px 10px 5px;'>").append(bodyContent)
                                .append("</div>");
                    }
                }
            }
        } else {
            sb.append(I18n.get("task.form.history.no_notes"));
        }
        sb.append("</div></body></html>");
        txtHistory.setText(sb.toString());
        txtHistory.setCaretPosition(0);
    }

    private void buildCustomFieldsUI() {
        LoggerUtil.logDebug("TaskFormDialog", "Building Custom Fields UI...");
        pCustomFields.removeAll();
        customFieldInputs.clear();

        if (customFieldDefs == null || customFieldDefs.isEmpty()) {
            pCustomFields.revalidate();
            pCustomFields.repaint();
            return;
        }

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;

        // Group fields into 2 columns
        int layoutIndex = 0;
        SimpleEntity currentTracker = (SimpleEntity) cbTracker.getSelectedItem();
        int currentTrackerId = currentTracker != null ? currentTracker.id : 0;

        for (int i = 0; i < customFieldDefs.size(); i++) {
            CustomFieldDefinition def = customFieldDefs.get(i);

            // 1. Strict Context Filter (Server-side validation from /issues/new)
            // This is the gold standard: Redmine tells us exactly what IDs are valid for
            // this P+T.
            // If we have this data, we ignore local heuristics.
            if (def.id != -1 && !allowedCustomFieldIds.isEmpty()) {
                if (!allowedCustomFieldIds.contains(def.id)) {
                    // LoggerUtil.logDebug("TaskFormDialog", "Strict Filter: Hiding " + def.name + "
                    // (" + def.id + ")");
                    continue;
                }
            } else {
                // 2. Fallback Filter (Local definition checks)
                // Used only during initial load or if context fetch failed.

                // Filter by tracker (if defined locally)
                if (def.id != -1 && !def.trackerIds.isEmpty() && currentTrackerId > 0
                        && !def.trackerIds.contains(currentTrackerId)) {
                    continue;
                }

                // Filter by Project (Critical Fix for "Mixed Clients" issue)
                // If a field is restricted to specific projects, and we are not in one of them,
                // hide it.
                if (def.id != -1 && !def.projectIds.isEmpty() && contextProject != null && contextProject.id > 0
                        && !def.projectIds.contains(contextProject.id)) {
                    continue;
                }

                // Filter by Project (Client) - Legacy logic (often fails if API hides
                // 'projects' list)
                if (isContextRestrictionActive) {
                    // Strict Mode: If ID not in allowed list, HIDE IT.
                    // This handles the case where allowedCustomFieldIds is empty (project has NO
                    // fields).
                    if (!allowedCustomFieldIds.contains(def.id)) {
                        continue;
                    }
                } else if (def.id != -1 && !def.projectIds.isEmpty()) {
                    // Fallback Mode (Legacy)
                    if (contextProject == null) {
                        continue; // Hide by default if strict mode failed/loading
                    }
                    if (!def.projectIds.contains(contextProject.id)) {
                        continue;
                    }
                }
            }

            LoggerUtil.logDebug("TaskFormDialog", "Rendering field: " + def.name + " (" + def.type + ")");

            if (def.id == -1 && "warning_label".equals(def.type)) {
                JLabel warn = new JLabel("⚠️ " + I18n.get("task.form.custom_fields.restricted_mode",
                        "Modo restringido: Listas no disponibles (falta permiso global)"));
                warn.setForeground(Color.RED);
                warn.setFont(warn.getFont().deriveFont(Font.BOLD));

                // Add to a container to ensure padding/visibility
                JPanel warnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                warnPanel.add(warn);
                warnPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.RED));

                g.gridx = 0;
                g.gridwidth = 4; // Span full width (label+input * 2 columns)
                pCustomFields.add(warnPanel, g);
                g.gridy++;
                g.gridwidth = 1; // Reset
                continue;
            }

            JLabel label = new JLabel(def.name + ":");
            JComponent input;

            if ("date".equals(def.type)) {
                JPanel dateP = new JPanel(new BorderLayout());
                JTextField txtDate = new JTextField(10);
                JButton btnPick = new JButton("📅");
                btnPick.addActionListener(e -> {
                    DatePickerPopup popup = new DatePickerPopup(d -> txtDate.setText(d));
                    popup.show(btnPick, 0, btnPick.getHeight());
                });
                dateP.add(txtDate, BorderLayout.CENTER);
                dateP.add(btnPick, BorderLayout.EAST);
                input = dateP;

                // Store the text field as the input component for value extraction
                customFieldInputs.put(def.id, txtDate);

                // Add special logic for layout
                g.gridx = (layoutIndex % 2) * 2;
                pCustomFields.add(label, g);
                g.gridx++;
                pCustomFields.add(input, g); // Add JPanel

                if (layoutIndex % 2 == 1) {
                    g.gridy++;
                }
                layoutIndex++;
                continue;
            } else if ("bool".equals(def.type)) {
                JComboBox<String> cb = new JComboBox<>(new String[] { "", "Sí", "No" });
                input = cb;
            } else if (def.possibleValues != null && !def.possibleValues.isEmpty()
                    && !def.name.toLowerCase().contains("tracker")) {
                // Ensure 'Tracker externo' is NEVER rendered as a ComboBox even if it has
                // learned values
                JComboBox<String> cb = new JComboBox<>();
                // Allow editable to support adding new values (Heuristic learning)
                cb.setEditable(true);
                // Constraint the width - 15 chars to match main form strict layout
                cb.setPrototypeDisplayValue("WWWWWWWWWWWWWWW"); // 15 chars

                if (!def.isRequired) {
                    cb.addItem("");
                }
                for (String val : def.possibleValues) {
                    cb.addItem(val);
                }

                // Smart Pre-fill for Required Fields
                if (def.isRequired && cb.getItemCount() > 0) {
                    cb.setSelectedIndex(0);
                    // If editable, this sets the editor text too
                }

                input = cb;
            } else {
                JTextField txt = new JTextField();
                input = txt;
            }

            customFieldInputs.put(def.id, input);

            g.gridx = (layoutIndex % 2) * 2;
            pCustomFields.add(label, g);

            g.gridx++;
            g.weightx = 0.5;
            pCustomFields.add(input, g);
            g.weightx = 0;

            if (layoutIndex % 2 == 1) { // New row every 2 items
                g.gridy++;
            }
            layoutIndex++;
        }

        pCustomFields.revalidate();
        pCustomFields.repaint();
    }

    private void resetCustomFields() {
        if (customFieldDefs == null)
            return;

        for (CustomFieldDefinition def : customFieldDefs) {
            JComponent input = customFieldInputs.get(def.id);
            if (input == null)
                continue;

            if (input instanceof JTextField) {
                ((JTextField) input).setText("");
            } else if (input instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) input;
                if (cb.getItemCount() > 0) {
                    // Smart Pre-fill:
                    // 1. If we have items, default to the first one.
                    cb.setSelectedIndex(0);
                    LoggerUtil.logDebug("TaskFormDialog",
                            "resetCustomFields [" + def.name + "]: Reset to index 0 ('" + cb.getItemAt(0) + "')");

                    // 2. If the first item is EMPTY (e.g. optional field or Restricted Mode
                    // fallback)
                    // AND we have a second item (real value), prefer the real value.
                    // This helps when "Restricted Mode" incorrectly flags mandatory fields as
                    // optional.
                    // EXCEPTION: User requested "Tracker externo" to remain empty by default.
                    Object item0 = cb.getItemAt(0);
                    if (item0 != null && "".equals(item0.toString()) && cb.getItemCount() > 1) {
                        if (def.name != null && !"Tracker externo".equalsIgnoreCase(def.name)) {
                            // Smart Default: Always select first real value if default is empty.
                            // RESTRICTED MODE FIX: We ignore def.isRequired because it might be false or
                            // unknown in restricted mode
                            // while the field is actually mandatory.
                            cb.setSelectedIndex(1);
                            LoggerUtil.logDebug("TaskFormDialog", "resetCustomFields [" + def.name
                                    + "]: Smart Default applied (Index 1: '" + cb.getItemAt(1) + "')");
                        } else {
                            LoggerUtil.logDebug("TaskFormDialog", "resetCustomFields [" + def.name
                                    + "]: Smart Default SKIPPED (Excluded via loop/name)");
                        }
                    }
                }
            }
        }
    }

    private void fillCustomFields(Task t) {
        if (customFieldDefs == null || t.customFields == null)
            return;

        // Opportunistic Learning:
        // If we see fields in this task that we don't know about, save them to cache.
        if (t.customFields != null && !t.customFields.isEmpty() && contextUrl != null) {
            redmineconnector.service.CustomFieldsCache.learnFromTasks(contextUrl,
                    java.util.Collections.singletonList(t));
        }

        LoggerUtil.logDebug("TaskFormDialog",
                "fillCustomFields: Task has " + (t.customFields != null ? t.customFields.size() : "null")
                        + " custom fields. Inputs map size: " + customFieldInputs.size());

        for (CustomField cf : t.customFields) {
            JComponent input = customFieldInputs.get(cf.id);
            if (input == null) {
                continue;
            }

            if (input instanceof JTextField) {
                ((JTextField) input).setText(cf.value);
            } else if (input instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) input;
                if (cf.value != null) {
                    if (cb.getItemCount() > 0 && cb.getItemAt(0) instanceof String) {
                        String val = cf.value;
                        if ("1".equals(val) && "bool".equals(getDefType(cf.id)))
                            val = "Sí";
                        if ("0".equals(val) && "bool".equals(getDefType(cf.id)))
                            val = "No";

                        // CRITICAL FIX: Only select the value if it exists in the model.
                        // This prevents overwriting the "Smart Pre-fill" (index 0) of mandatory fields
                        // with an empty string from a new task.
                        boolean exists = false;
                        for (int i = 0; i < cb.getItemCount(); i++) {
                            Object item = cb.getItemAt(i);
                            if (val.equals(item)) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            LoggerUtil.logDebug("TaskFormDialog",
                                    "fillCustomFields [" + cf.name + "]: Value '" + val
                                            + "' NOT FOUND in Combo. Listing items:");
                            for (int i = 0; i < cb.getItemCount(); i++) {
                                LoggerUtil.logDebug("TaskFormDialog", " - Item " + i + ": '" + cb.getItemAt(i) + "'");
                            }
                        }

                        if (exists) {
                            // Prevent overwriting Smart Default with empty value during Creation/Clone
                            // unless it's an explicit edit of an existing task.
                            CustomFieldDefinition def = getCustomFieldDefinition(cf.id); // Need def for name
                            boolean isTrackerExterno = (def != null && "Tracker externo".equalsIgnoreCase(def.name));

                            if (!isEditMode && (val == null || val.trim().isEmpty())) {
                                LoggerUtil.logDebug("TaskFormDialog", "fillCustomFields: Ignoring empty value for "
                                        + cf.name + " (Preserving Smart Default)");
                                continue;
                            }

                            // Removed strict clearing of 'Tracker externo' to allow Auto-Fill from
                            // TaskOperations
                            // if (!isEditMode && isTrackerExterno) { ... }

                            cb.setSelectedItem(val);
                            LoggerUtil.logDebug("TaskFormDialog", "fillCustomFields: Set " + cf.name + " to " + val);
                        }
                    }
                }
            }
        }

        // Final Pass: Ensure Smart Defaults are applied for New/Clone tasks
        // This acts as a safety net if data was empty or reset failed.
        if (!isEditMode) {
            for (java.util.Map.Entry<Integer, JComponent> entry : customFieldInputs.entrySet()) {
                if (entry.getValue() instanceof JComboBox) {
                    JComboBox<?> cb = (JComboBox<?>) entry.getValue();
                    CustomFieldDefinition def = getCustomFieldDefinition(entry.getKey());

                    // Skip 'Tracker externo' (should be handled by auto-fill or remain empty)
                    if (def != null && def.name != null && def.name.toLowerCase().contains("tracker")) {
                        continue;
                    }

                    // If selection is empty/index 0 (which might be blank), try to select index 1
                    if (cb.getItemCount() > 1 && (cb.getSelectedIndex() == -1 || cb.getSelectedIndex() == 0)) {
                        Object item0 = cb.getItemAt(0);
                        if (item0 == null || "".equals(item0.toString())) {
                            cb.setSelectedIndex(1);
                            LoggerUtil.logDebug("TaskFormDialog",
                                    "fillCustomFields (Final Pass): Forced Smart Default for "
                                            + (def != null ? def.name : "?"));
                        }
                    }
                }
            }
        }
    }

    private void saveCustomFields(Task t) {
        if (customFieldDefs == null)
            return;
        List<CustomField> newFields = new ArrayList<>();

        for (CustomFieldDefinition def : customFieldDefs) {
            JComponent input = customFieldInputs.get(def.id);
            if (input == null)
                continue;

            String value = "";
            if (input instanceof JTextField) {
                value = ((JTextField) input).getText();
                if ("date".equals(def.type) && value != null && !value.trim().isEmpty()) {
                    try {
                        java.util.Date d = new java.text.SimpleDateFormat("dd/MM/yyyy").parse(value);
                        value = new java.text.SimpleDateFormat("yyyy-MM-dd").format(d);
                    } catch (Exception e) {
                        // Ignore parse error, send as is (user might have entered invalid format)
                    }
                }
            } else if (input instanceof JPanel) {
                // Handle the date panel specifically if input logic changes,
                // but currently date input puts JTextField in customFieldInputs map (line 1723)
                // so the instanceof JTextField check above covers it.
            } else if (input instanceof JComboBox) {
                Object sel = ((JComboBox<?>) input).getSelectedItem();
                if (sel != null) {
                    String s = sel.toString();
                    if ("bool".equals(def.type)) {
                        if ("Sí".equals(s))
                            s = "1";
                        else if ("No".equals(s))
                            s = "0";
                        else
                            s = "";
                    }
                    value = s;
                }
            }

            newFields.add(new CustomField(def.id, def.name, value));
        }
        t.customFields = newFields;
    }

    private String getDefType(int id) {
        if (customFieldDefs == null)
            return "";
        for (CustomFieldDefinition def : customFieldDefs) {
            if (def.id == id)
                return def.type;
        }
        return "";
    }

    private redmineconnector.model.CustomFieldDefinition getCustomFieldDefinition(int id) {
        if (customFieldDefs == null)
            return null;
        for (redmineconnector.model.CustomFieldDefinition def : customFieldDefs) {
            if (def.id == id)
                return def;
        }
        return null;
    }
}
