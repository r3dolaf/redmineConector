package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.JTabbedPane;
import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
// import javax.swing.JTextPane; - Removed unused

import redmineconnector.model.Attachment;
import redmineconnector.model.WikiPageDTO;
import redmineconnector.model.WikiVersionDTO;
import redmineconnector.service.DataService;
import redmineconnector.service.AsyncDataService;
import redmineconnector.ui.UIHelper;
import redmineconnector.util.AsyncUIHelper;
import redmineconnector.util.I18n;
import redmineconnector.util.TextileConverter;

public class WikiManagerDialog extends JDialog {
    private final AsyncDataService asyncService;
    private final String baseUrl;
    private final String projectId;
    private final Consumer<String> logger;

    // Data
    private List<WikiPageDTO> allWikiPages = new ArrayList<>();
    private WikiPageDTO currentPage;

    // UI Components
    private JSplitPane splitPane;
    private JList<WikiPageDTO> wikiList;
    private DefaultListModel<WikiPageDTO> listModel;
    private JTextField txtSearch;

    // Right Panel (CardLayout)
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private static final String CARD_EMPTY = "EMPTY";
    private static final String CARD_VIEW = "VIEW";
    private static final String CARD_EDIT = "EDIT";

    // View Components
    private JTabbedPane viewTabs; // New: Tabs for View/History/Attachments
    private JLabel lblViewTitle;
    private JEditorPane txtViewContent; // Changed to JEditorPane for HTML
    private JLabel lblViewInfo;

    // History Components
    private JTable historyTable;
    private DefaultTableModel historyModel;

    // Attachments
    private JList<Attachment> attachmentList;
    private DefaultListModel<Attachment> attachmentModel;

    // Edit Components
    private JTextField txtEditTitle;
    private redmineconnector.ui.components.DragDropImageTextPane txtEditContent; // Upgaded to DragDropImageTextPane
    private JEditorPane previewPane; // New: Live Preview
    private JTextField txtEditComment;
    private JSplitPane editSplitPane; // New: Split for editor/preview

    public WikiManagerDialog(Window owner, String title, DataService service, String baseUrl, String projectId,
            String clientName, Consumer<String> logger) {
        super(owner, I18n.format("wiki.window.title", title), ModalityType.MODELESS);
        this.asyncService = new AsyncDataService(service);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.projectId = projectId;
        this.logger = logger;

        setTitle(I18n.get("wiki.dialog.title") + " - " + projectId);
        setModal(true);
        setSize(redmineconnector.util.AppConstants.DIALOG_WIDTH_XL,
                redmineconnector.util.AppConstants.DIALOG_HEIGHT_LARGE);
        setLocationRelativeTo(owner);
        UIHelper.addEscapeListener(this);
        setLayout(new BorderLayout());

        initUI();
        loadWikiPages();
    }

    private void initUI() {
        // Master Panel (Left)
        JPanel leftPanel = createLeftPanel();

        // Detail Panel (Right)
        JPanel rightPanel = createRightPanel();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(redmineconnector.util.AppConstants.SPLIT_PANE_DIVIDER_LOCATION);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        // Search Bar
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", I18n.get("wiki.search.placeholder"));
        searchPanel.add(new JLabel("🔍"), BorderLayout.WEST);
        searchPanel.add(txtSearch, BorderLayout.CENTER);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        // List
        listModel = new DefaultListModel<>();
        wikiList = new JList<>(listModel);
        wikiList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        wikiList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(new EmptyBorder(5, 10, 5, 10));
                return this;
            }
        });
        wikiList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                WikiPageDTO selected = wikiList.getSelectedValue();
                if (selected != null) {
                    loadPageDetails(selected);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(wikiList);
        scrollPane.setBorder(null);

        // Toolbar (Bottom)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnAdd = new JButton(I18n.get("wiki.btn.new"));
        JButton btnRefresh = new JButton(I18n.get("wiki.btn.refresh"));

        btnAdd.addActionListener(e -> switchToEditMode(null));
        btnRefresh.addActionListener(e -> loadWikiPages());

        bottomPanel.add(btnAdd);
        bottomPanel.add(btnRefresh);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRightPanel() {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        cardPanel.add(createEmptyPanel(), CARD_EMPTY);
        cardPanel.add(createViewPanel(), CARD_VIEW);
        cardPanel.add(createEditPanel(), CARD_EDIT);

        return cardPanel;
    }

    private JPanel createEmptyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(I18n.get("wiki.empty.message"), JLabel.CENTER);
        label.setForeground(Color.GRAY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Header (Shared)
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        header.setBackground(new Color(250, 250, 250));

        lblViewTitle = new JLabel();
        lblViewTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));

        lblViewInfo = new JLabel();
        lblViewInfo.setForeground(Color.GRAY);

        header.add(lblViewTitle, BorderLayout.CENTER);
        header.add(lblViewInfo, BorderLayout.SOUTH);

        // Tabs
        viewTabs = new JTabbedPane();
        viewTabs.addTab(I18n.get("wiki.tab.content"), createPageContentPanel());
        viewTabs.addTab(I18n.get("wiki.tab.history"), createHistoryPanel());
        viewTabs.addTab(I18n.get("wiki.tab.attachments"), createAttachmentsPanel());

        // Listener to load history when tab selected
        viewTabs.addChangeListener(e -> {
            if (viewTabs.getSelectedIndex() == 1 && currentPage != null) {
                loadHistory(currentPage);
            }
        });

        // Toolbar (Global for view)
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton btnEdit = new JButton(I18n.get("wiki.btn.edit"));
        JButton btnDelete = new JButton(I18n.get("wiki.btn.delete"));
        JButton btnBrowser = new JButton(I18n.get("wiki.btn.open"));
        JButton btnExport = new JButton(I18n.get("wiki.btn.export"));

        btnEdit.addActionListener(e -> switchToEditMode(currentPage));
        btnDelete.addActionListener(e -> deleteCurrentPage());
        btnBrowser.addActionListener(e -> openInBrowser(currentPage));
        btnExport.addActionListener(e -> exportToHtml());

        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnBrowser);
        toolbar.add(btnExport);

        panel.add(header, BorderLayout.NORTH);
        panel.add(viewTabs, BorderLayout.CENTER);
        panel.add(toolbar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPageContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        txtViewContent = new JEditorPane();
        txtViewContent.setEditable(false);
        txtViewContent.setContentType("text/html");
        txtViewContent.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        txtViewContent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtViewContent.setBorder(new EmptyBorder(10, 20, 10, 20));
        panel.add(new JScrollPane(txtViewContent), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        historyModel = new DefaultTableModel(
                new Object[] { I18n.get("wiki.history.col.version"), I18n.get("wiki.history.col.date"),
                        I18n.get("wiki.history.col.author"), I18n.get("wiki.history.col.comment") },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(25);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(50); // Ver
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Date

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRevert = new JButton(I18n.get("wiki.btn.revert"));
        btnRevert.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row != -1) {
                int version = (int) historyModel.getValueAt(row, 0);
                revertPage(version);
            }
        });
        btnPanel.add(btnRevert);

        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAttachmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        attachmentModel = new DefaultListModel<>();
        attachmentList = new JList<>(attachmentModel);
        attachmentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        attachmentList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Attachment sel = attachmentList.getSelectedValue();
                    if (sel != null)
                        downloadAttachment(sel);
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnUpload = new JButton(I18n.get("wiki.btn.upload"));
        JButton btnDownload = new JButton(I18n.get("wiki.btn.download"));

        btnUpload.addActionListener(e -> uploadAttachment());
        btnDownload.addActionListener(e -> {
            Attachment sel = attachmentList.getSelectedValue();
            if (sel != null)
                downloadAttachment(sel);
        });

        topPanel.add(btnUpload);
        topPanel.add(btnDownload);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(attachmentList), BorderLayout.CENTER);
        return panel;
    }

    private void uploadAttachment() {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // Chain operations: Upload File -> Upload Attachment -> Fetch Updated Content
            java.util.concurrent.CompletableFuture<WikiPageDTO> uploadFlow = asyncService
                    .uploadFileAsync(readFile(file), getContentType(file))
                    .thenCompose(token -> {
                        String text = currentPage != null ? currentPage.text : "";
                        int version = currentPage != null ? Integer.parseInt(currentPage.version) : 0;
                        return asyncService.uploadWikiAttachmentAsync(projectId, currentPage.title, token,
                                file.getName(),
                                getContentType(file), text, version);
                    })
                    .thenCompose(v -> asyncService.fetchWikiPageContentAsync(projectId, currentPage.title));

            AsyncUIHelper.executeAsync(
                    uploadFlow,
                    updatedPage -> {
                        logger.accept(I18n.format("wiki.info.upload.success", file.getName()));
                        // Update current page and view
                        this.currentPage = updatedPage;
                        showViewMode(updatedPage);
                        setCursor(Cursor.getDefaultCursor());
                    },
                    error -> {
                        logger.accept(I18n.format("wiki.error.upload", error.getMessage()));
                        JOptionPane.showMessageDialog(this, I18n.format("wiki.error.upload", error.getMessage()),
                                I18n.get("wiki.error.generic.title"), JOptionPane.ERROR_MESSAGE);
                        setCursor(Cursor.getDefaultCursor());
                    });
        }
    }

    private byte[] readFile(java.io.File file) {
        try {
            return java.nio.file.Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo archivo", e);
        }
    }

    private String getContentType(java.io.File file) {
        try {
            String type = java.nio.file.Files.probeContentType(file.toPath());
            return type != null ? type : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    private void downloadAttachment(Attachment att) {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setSelectedFile(new java.io.File(att.filename));
        if (fileChooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            AsyncUIHelper.executeAsync(
                    asyncService.downloadAttachmentAsync(att),
                    data -> {
                        try {
                            java.nio.file.Files.write(fileChooser.getSelectedFile().toPath(), data);
                            JOptionPane.showMessageDialog(this, I18n.get("wiki.msg.file.saved"));
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(this, I18n.format("wiki.error.file.save", e.getMessage()));
                        }
                        setCursor(Cursor.getDefaultCursor());
                    },
                    err -> {
                        JOptionPane.showMessageDialog(this, I18n.format("wiki.error.file.download", err.getMessage()));
                        setCursor(Cursor.getDefaultCursor());
                    });
        }
    }

    private JPanel createEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout(5, 5));
        header.setBorder(new EmptyBorder(10, 10, 10, 10));

        txtEditTitle = new JTextField();
        txtEditTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtEditTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        header.add(new JLabel(I18n.get("wiki.label.title")), BorderLayout.NORTH);
        header.add(txtEditTitle, BorderLayout.CENTER);

        // Content Area (Split Pane: Edit | Preview)
        txtEditContent = new redmineconnector.ui.components.DragDropImageTextPane();
        txtEditContent.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtEditContent.setImageCacheDir(redmineconnector.service.ImageCacheService.getInstance().getCacheDir());
        // Simple highlighting setup (placeholder for future advanced highlighter)

        previewPane = new JEditorPane();
        previewPane.setEditable(false);
        previewPane.setContentType("text/html");

        editSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(txtEditContent),
                new JScrollPane(previewPane));
        editSplitPane.setResizeWeight(0.5);

        // Update preview on type
        txtEditContent.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
            }

            public void removeUpdate(DocumentEvent e) {
                updatePreview();
            }

            public void changedUpdate(DocumentEvent e) {
                updatePreview();
            }
        });

        // Format Toolbar
        JToolBar formatBar = new JToolBar();
        formatBar.setFloatable(false);
        addFormatButton(formatBar, "B", "**", "**");
        addFormatButton(formatBar, "I", "_", "_");
        addFormatButton(formatBar, "H1", "h1. ", "");
        addFormatButton(formatBar, "H2", "h2. ", "");
        addFormatButton(formatBar, "Code", "<pre>", "</pre>");
        addFormatButton(formatBar, "Link", "[[", "]]");

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(formatBar, BorderLayout.NORTH);
        centerPanel.add(editSplitPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout(5, 5));
        footer.setBorder(new EmptyBorder(10, 10, 10, 10));

        txtEditComment = new JTextField();
        JButton btnSave = new JButton(I18n.get("wiki.btn.save"));
        JButton btnCancel = new JButton(I18n.get("wiki.btn.cancel"));

        btnSave.addActionListener(e -> savePage());
        btnCancel.addActionListener(e -> {
            if (currentPage != null) {
                // Return to view
                cardLayout.show(cardPanel, CARD_VIEW);
            } else {
                cardLayout.show(cardPanel, CARD_EMPTY);
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        footer.add(new JLabel(I18n.get("wiki.label.comment")), BorderLayout.NORTH);
        footer.add(txtEditComment, BorderLayout.CENTER);
        footer.add(btnPanel, BorderLayout.SOUTH);

        panel.add(header, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private void updatePreview() {
        String text = txtEditContent.getText();
        String html = TextileConverter.convertToHtml(text);
        previewPane.setText(html);
    }

    private void addFormatButton(JToolBar bar, String label, String prefix, String suffix) {
        JButton btn = new JButton(label);
        btn.setFocusable(false);
        btn.addActionListener(e -> {
            String selected = txtEditContent.getSelectedText();
            if (selected == null)
                selected = "";
            txtEditContent.replaceSelection(prefix + selected + suffix);
            txtEditContent.requestFocusInWindow();
        });
        bar.add(btn);
    }

    // Logic

    private void loadWikiPages() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        AsyncUIHelper.executeAsync(
                asyncService.fetchWikiPagesAsync(projectId),
                pages -> {
                    allWikiPages.clear();
                    allWikiPages.addAll(pages);
                    allWikiPages.sort(Comparator.comparing(p -> p.title, String.CASE_INSENSITIVE_ORDER));
                    applyFilter();
                    setCursor(Cursor.getDefaultCursor());
                },
                error -> {
                    JOptionPane.showMessageDialog(this, I18n.format("wiki.error.load.pages", error.getMessage()));
                    setCursor(Cursor.getDefaultCursor());
                });
    }

    private void applyFilter() {
        String query = txtSearch.getText().toLowerCase();
        WikiPageDTO selected = wikiList.getSelectedValue();
        listModel.clear();
        for (WikiPageDTO p : allWikiPages) {
            if (p.title.toLowerCase().contains(query)) {
                listModel.addElement(p);
            }
        }
        if (selected != null && listModel.contains(selected)) {
            wikiList.setSelectedValue(selected, true);
        }
    }

    private void loadPageDetails(WikiPageDTO stub) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        AsyncUIHelper.executeAsync(
                asyncService.fetchWikiPageContentAsync(projectId, stub.title),
                fullPage -> {
                    this.currentPage = fullPage;
                    showViewMode(fullPage);
                    setCursor(Cursor.getDefaultCursor());
                },
                error -> {
                    // If 404/not found, it might be new? But this is loadFromList.
                    JOptionPane.showMessageDialog(this, I18n.format("wiki.error.load.content", error.getMessage()));
                    setCursor(Cursor.getDefaultCursor());
                });
    }

    private void showViewMode(WikiPageDTO page) {
        lblViewTitle.setText(page.title);
        lblViewInfo.setText(String.format("Versión: %s | Autor: %s | %s",
                page.version, page.author, page.updatedOn));

        // 1. Initial Render (Text + Cached Images)
        java.util.Set<String> imageNames = TextileConverter.extractImageNames(page.text);
        java.util.Map<String, String> resolvedImages = new java.util.HashMap<>();
        java.util.Map<String, Attachment> toDownload = new java.util.HashMap<>();

        redmineconnector.service.ImageCacheService cache = redmineconnector.service.ImageCacheService.getInstance();

        if (imageNames != null && !imageNames.isEmpty()) {
            StringBuilder availableFiles = new StringBuilder();
            if (page.attachments != null) {
                for (Attachment a : page.attachments)
                    availableFiles.append(a.filename).append(",");
            }
            String debugList = availableFiles.toString();

            for (String imgName : imageNames) {
                // Try to find attachment
                Attachment att = findAttachment(page.attachments, imgName);
                if (att != null) {
                    java.io.File cached = cache.getCachedImage(att.filename);
                    if (cached != null) {
                        resolvedImages.put(imgName, cached.getAbsolutePath());
                    } else {
                        // Mark as downloading or normal missing
                        toDownload.put(imgName, att);
                    }
                } else {
                    // Debug: Image not found in attachments
                    resolvedImages.put(imgName,
                            "DEBUG: Not Found. Available: " + (debugList.isEmpty() ? "None" : debugList));
                }
            }
        }

        // Render with what we have
        String html = TextileConverter.convertToHtml(page.text, resolvedImages);
        txtViewContent.setText(html);
        txtViewContent.setCaretPosition(0);

        // 2. Background Download for missing images
        if (!toDownload.isEmpty()) {
            downloadMissingImages(page, toDownload, resolvedImages);
        }

        // Populate Attachments
        if (attachmentModel != null) {
            attachmentModel.clear();
            if (page.attachments != null) {
                for (Attachment a : page.attachments) {
                    attachmentModel.addElement(a);
                }
            }
        }

        // Reset tabs
        viewTabs.setSelectedIndex(0);

        cardLayout.show(cardPanel, CARD_VIEW);
    }

    private Attachment findAttachment(List<Attachment> attachments, String name) {
        if (attachments == null || name == null)
            return null;
        for (Attachment a : attachments) {
            if (a.filename.equalsIgnoreCase(name))
                return a;
        }
        return null;
    }

    private void downloadMissingImages(WikiPageDTO page, java.util.Map<String, Attachment> toDownload,
            java.util.Map<String, String> currentResolved) {

        // We use a separate async flow that doesn't block UI
        java.util.concurrent.CompletableFuture<Void> allDownloads = java.util.concurrent.CompletableFuture
                .completedFuture(null);

        redmineconnector.service.ImageCacheService cache = redmineconnector.service.ImageCacheService.getInstance();

        for (java.util.Map.Entry<String, Attachment> entry : toDownload.entrySet()) {
            String imgNameKey = entry.getKey();
            Attachment att = entry.getValue();

            allDownloads = allDownloads.thenCompose(v -> asyncService.downloadAttachmentAsync(att)
                    .thenAccept(data -> {
                        if (data != null) {
                            java.io.File saved = cache.saveImage(att.filename, data);
                            if (saved != null) {
                                synchronized (currentResolved) {
                                    currentResolved.put(imgNameKey, saved.getAbsolutePath());
                                    currentResolved.put(att.filename, saved.getAbsolutePath());
                                }
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        // Log and show error in UI
                        logger.accept("Error downloading image " + att.filename + ": " + ex.getMessage());
                        synchronized (currentResolved) {
                            currentResolved.put(imgNameKey, "ERROR: " + ex.getMessage());
                        }
                        return null;
                    }));
        }

        // When all done, refresh view
        allDownloads.thenRun(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                // Only update if we are still viewing the same page
                if (currentPage != null && currentPage.title.equals(page.title)) {
                    String newHtml = TextileConverter.convertToHtml(page.text, currentResolved);
                    txtViewContent.setText(newHtml);
                    // Preserve scroll position? JEditorPane resets on setText usually.
                    // For now, simple refresh.
                }
            });
        });
    }

    private void switchToEditMode(WikiPageDTO page) {
        if (page == null) {
            // New Page
            currentPage = null;
            txtEditTitle.setText("");
            txtEditTitle.setEditable(true);
            txtEditContent.setText("");
            txtEditComment.setText("Creación inicial");
            txtEditContent.setAvailableAttachments(null);
        } else {
            // Edit Existing
            txtEditTitle.setText(page.title);
            txtEditTitle.setEditable(false); // Title usually immutable in Redmine wikis once created (renames are
                                             // special)
            txtEditContent.setAvailableAttachments(page.attachments);
            txtEditContent.setText(page.text);
            txtEditContent.setCaretPosition(0);
            txtEditComment.setText("");
        }

        // Configure download handler for inline images
        txtEditContent.setDownloadHandler((att, file) -> {
            AsyncUIHelper.executeAsync(
                    asyncService.downloadAttachmentAsync(att),
                    data -> {
                        try {
                            java.nio.file.Files.write(file.toPath(), data);
                        } catch (Exception e) {
                            logger.accept("Error downloading inline image: " + e.getMessage());
                        }
                    },
                    error -> logger.accept("Error downloading inline image: " + error.getMessage()));
        });

        cardLayout.show(cardPanel, CARD_EDIT);
    }

    private void savePage() {
        String title = txtEditTitle.getText().trim();
        String content = txtEditContent.getTextWithImageReferences(); // Modified to get text with !img! tags
        String comment = txtEditComment.getText();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.get("wiki.error.title.required"));
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // 1. Save/Update Page First (ensure it exists) AND fetch to get version
        java.util.concurrent.CompletableFuture<WikiPageDTO> saveFlow = asyncService
                .createOrUpdateWikiPageAsync(projectId, title, content, comment)
                .thenCompose(v -> asyncService.fetchWikiPageContentAsync(projectId, title));

        // 2. Helper function to chain uploads
        // We need to use the title variable as currentPage might be null for new pages
        java.util.concurrent.CompletableFuture<WikiPageDTO> flowWithUploads = saveFlow.thenCompose(savedPage -> {
            List<java.io.File> imagesToUpload = txtEditContent.getPastedImages();
            if (imagesToUpload.isEmpty()) {
                return java.util.concurrent.CompletableFuture.completedFuture(savedPage);
            }

            // Chain uploads
            List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
            for (java.io.File file : imagesToUpload) {
                futures.add(asyncService.uploadFileAsync(readFile(file), getContentType(file))
                        .thenCompose(token -> asyncService.uploadWikiAttachmentAsync(
                                projectId,
                                title, // Use the title we just saved
                                token,
                                file.getName(),
                                getContentType(file),
                                null, // We don't want to replace text here, just attach
                                Integer.parseInt(savedPage.version) // Use the version from the saved page
                )));
            }

            // Return the saved page after uploads
            return java.util.concurrent.CompletableFuture.allOf(
                    futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .thenApply(v -> savedPage);
        });

        // 3. Finally fetch fresh content
        java.util.concurrent.CompletableFuture<WikiPageDTO> finalFlow = flowWithUploads
                .thenCompose(v -> asyncService.fetchWikiPageContentAsync(projectId, title));

        AsyncUIHelper.executeAsync(
                finalFlow,
                savedPage -> {
                    JOptionPane.showMessageDialog(this, I18n.get("wiki.msg.save.success"));
                    this.currentPage = savedPage;
                    txtEditContent.clearPastedImages();
                    showViewMode(savedPage);
                    loadWikiPages(); // Refresh list
                    setCursor(Cursor.getDefaultCursor());
                },
                error -> {
                    JOptionPane.showMessageDialog(this, I18n.format("wiki.error.save", error.getMessage()));
                    setCursor(Cursor.getDefaultCursor());
                });
    }

    private void deleteCurrentPage() {
        if (currentPage == null)
            return;

        int confirm = JOptionPane.showConfirmDialog(this,
                I18n.format("wiki.msg.confirm.delete", currentPage.title),
                I18n.get("wiki.msg.confirm.title"), JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            AsyncUIHelper.executeAsync(
                    asyncService.deleteWikiPageAsync(projectId, currentPage.title),
                    v -> {
                        JOptionPane.showMessageDialog(this, I18n.get("wiki.msg.delete.success"));
                        currentPage = null;
                        cardLayout.show(cardPanel, CARD_EMPTY);
                        loadWikiPages();
                        setCursor(Cursor.getDefaultCursor());
                    },
                    error -> {
                        JOptionPane.showMessageDialog(this, I18n.format("wiki.error.delete", error.getMessage()));
                        setCursor(Cursor.getDefaultCursor());
                    });
        }
    }

    private void openInBrowser(WikiPageDTO page) {
        if (page == null)
            return;
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                String encodedTitle = java.net.URLEncoder.encode(page.title, "UTF-8")
                        .replace("+", "%20");
                String webUrl = String.format("%s/projects/%s/wiki/%s", baseUrl,
                        projectId, encodedTitle);
                java.awt.Desktop.getDesktop().browse(new java.net.URI(webUrl));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, I18n.format("wiki.error.browser", ex.getMessage()));
        }
    }

    // Logic Methods for Advanced Features

    private void loadHistory(WikiPageDTO page) {
        historyModel.setRowCount(0); // clear
        AsyncUIHelper.executeAsync(
                asyncService.fetchWikiHistoryAsync(projectId, page.title),
                versions -> {
                    for (WikiVersionDTO v : versions) {
                        historyModel.addRow(new Object[] { v.version, v.updatedOn, v.author, v.comments });
                    }
                },
                error -> {
                    // Fail silently or log, history might not be available
                    logger.accept(I18n.format("wiki.error.history", error.getMessage()));
                });
    }

    private void revertPage(int version) {
        int confirm = JOptionPane.showConfirmDialog(this,
                I18n.format("wiki.msg.confirm.revert", version),
                I18n.get("wiki.msg.revert.title"), JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            AsyncUIHelper.executeAsync(
                    asyncService.revertWikiPageAsync(projectId, currentPage.title, version),
                    v -> {
                        JOptionPane.showMessageDialog(this, I18n.get("wiki.msg.revert.success"));
                        loadPageDetails(currentPage); // reload to get new latest version
                        setCursor(Cursor.getDefaultCursor());
                    },
                    error -> {
                        JOptionPane.showMessageDialog(this, I18n.format("wiki.error.revert", error.getMessage()));
                        setCursor(Cursor.getDefaultCursor());
                    });
        }
    }

    private void exportToHtml() {
        if (currentPage == null)
            return;

        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle(I18n.get("wiki.dialog.export.title"));
        fileChooser.setSelectedFile(new java.io.File(currentPage.title + ".html"));

        if (fileChooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                String html = TextileConverter.convertToHtml(currentPage.text);
                // Add simple header
                String fullHtml = "<html><head><title>" + currentPage.title + "</title>"
                        + "<style>body{font-family:sans-serif; max-width:800px; margin:20px auto; padding:20px; border:1px solid #eee;}</style>"
                        + "</head><body>"
                        + "<h1>" + currentPage.title + "</h1>"
                        + "<p style='color:gray'>Version " + currentPage.version + " - " + currentPage.updatedOn
                        + " by " + currentPage.author + "</p>"
                        + "<hr>"
                        + html
                        + "</body></html>";

                java.io.FileWriter writer = new java.io.FileWriter(file);
                writer.write(fullHtml);
                writer.close();
                JOptionPane.showMessageDialog(this, I18n.get("wiki.msg.export.success"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, I18n.format("wiki.error.export", ex.getMessage()));
            }
        }
    }
}
