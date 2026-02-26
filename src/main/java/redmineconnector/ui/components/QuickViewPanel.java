package redmineconnector.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import redmineconnector.model.Attachment;
import redmineconnector.model.Journal;
import redmineconnector.model.Task;
import redmineconnector.service.DataService;
import redmineconnector.service.ImageCacheService;
import redmineconnector.config.StyleConfig;
import redmineconnector.ui.InstanceController;
import redmineconnector.util.I18n;
import redmineconnector.util.TextileConverter;

/**
 * A dedicated panel for displaying QuickView details (description, notes,
 * attachments).
 * Extracted from InstanceView to improve maintainability and separate concerns.
 */
public class QuickViewPanel extends JPanel {

    private JEditorPane txtQuickDesc = new JEditorPane();
    private JEditorPane txtQuickNotes = new JEditorPane();
    private DefaultListModel<Attachment> attachmentListModel = new DefaultListModel<>();
    private JList<Attachment> lstAttachments = new JList<>(attachmentListModel);
    private JLabel lblPreview = new JLabel("Seleccione una imagen", SwingConstants.CENTER);
    private JTabbedPane miniTabs;

    private JButton btnQuickToggle;
    private java.util.function.Consumer<Integer> toggleAction;

    private boolean sortNotesNewestFirst = true;
    private Task currentTask;
    private InstanceController controller;
    private DataService dataService;
    private final StyleConfig styleConfig;

    private Map<String, String> imagePlaceholders = new HashMap<>();

    public QuickViewPanel(java.util.function.Consumer<Integer> toggleAction, StyleConfig styleConfig) {
        super(new BorderLayout());
        this.toggleAction = toggleAction;
        this.styleConfig = styleConfig;

        initializeUI();
        applyStyles();
    }

    public void applyStyles() {
        setBorder(BorderFactory.createLineBorder(styleConfig.border));
        setBackground(styleConfig.bgPanel);

        JPanel header = (JPanel) getComponent(0); // Assuming header is first
        header.setBackground(styleConfig.bgHeader);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, styleConfig.border));

        txtQuickDesc.setBackground(styleConfig.bgMain);
        txtQuickDesc.setForeground(styleConfig.textPrimary);
        txtQuickNotes.setBackground(styleConfig.bgMain); // Check if JEditorPane respects this or needs HTML body style
        txtQuickNotes.setForeground(styleConfig.textPrimary);

        lblPreview.setBorder(BorderFactory.createLineBorder(styleConfig.border));
        lblPreview.setForeground(styleConfig.textPrimary);

        // Update HTML content if exists to reflect new colors (like dark background for
        // notes)
        if (currentTask != null) {
            renderNotes(currentTask);
        }
    }

    public void setController(InstanceController controller) {
        this.controller = controller;
    }

    public void setDataService(DataService service) {
        this.dataService = service;
    }

    private void initializeUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        // Colors moved to applyStyles

        JLabel lblTitle = new JLabel("  Vista Rápida");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.add(lblTitle, BorderLayout.WEST);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightHeader.setOpaque(false);

        JButton btnCollapse = new JButton("▼");
        btnCollapse.setToolTipText("Contraer");
        btnCollapse.setMargin(new Insets(1, 2, 1, 2));
        btnCollapse.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCollapse.setPreferredSize(new Dimension(30, 26));
        btnCollapse.addActionListener(e -> {
            if (toggleAction != null)
                toggleAction.accept(-1);
        });

        JButton btnExpand = new JButton("▲");
        btnExpand.setToolTipText("Expandir");
        btnExpand.setMargin(new Insets(1, 2, 1, 2));
        btnExpand.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExpand.setPreferredSize(new Dimension(30, 26));
        btnExpand.addActionListener(e -> {
            if (toggleAction != null)
                toggleAction.accept(1);
        });

        rightHeader.add(btnCollapse);
        rightHeader.add(btnExpand);
        header.add(rightHeader, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        // Since I can't change constructor signature easily without breaking
        // InstanceView immediately
        // (well I can if I update both), I will modify the field definition first.

        // Let's modify the fields area first in a separate call? No, I can do it here
        // if I replace the whole file? No, that's expensive.
        // I'll proceed with replacing the button creation block, referencing new fields
        // I'll add.

        // Wait, I should add the new fields first or do it all in one go.
        // Let's use `multi_replace` to do it cleanly.

        header.add(rightHeader, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Content
        txtQuickDesc.setEditable(false);
        txtQuickDesc.setContentType("text/html");
        txtQuickDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        // Colors moved to applyStyles

        txtQuickNotes.setContentType("text/html");
        txtQuickNotes.setEditable(false);
        txtQuickNotes.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        miniTabs = new JTabbedPane(JTabbedPane.BOTTOM);
        miniTabs.addTab("Descripción", new JScrollPane(txtQuickDesc));

        // Notes Tab
        JPanel pQuickNotes = new JPanel(new BorderLayout());
        JPanel pQuickNotesHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pQuickNotesHeader.add(new JLabel("Ordenar:"));
        JComboBox<String> cbQuickNotesSort = new JComboBox<>(
                new String[] { "Más recientes primero", "Más antiguas primero" });
        cbQuickNotesSort.setSelectedIndex(0);
        cbQuickNotesSort.addActionListener(e -> {
            sortNotesNewestFirst = cbQuickNotesSort.getSelectedIndex() == 0;
            if (currentTask != null) {
                renderNotes(currentTask);
            }
        });
        pQuickNotesHeader.add(cbQuickNotesSort);
        pQuickNotes.add(pQuickNotesHeader, BorderLayout.NORTH);
        pQuickNotes.add(new JScrollPane(txtQuickNotes), BorderLayout.CENTER);
        miniTabs.addTab("Notas", pQuickNotes);

        // Attachments Tab
        JPanel pAdjuntos = new JPanel(new BorderLayout());
        lstAttachments.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstAttachments.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateAttachmentPreview();
            }
        });

        lstAttachments.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Attachment att = lstAttachments.getSelectedValue();
                    if (att != null && att.contentUrl != null) {
                        try {
                            Desktop.getDesktop().browse(new URI(att.contentUrl));
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        });

        lblPreview.setPreferredSize(new Dimension(300, 200));
        // Border moved to applyStyles

        JScrollPane spList = new JScrollPane(lstAttachments);
        spList.setPreferredSize(new Dimension(150, 0));
        JScrollPane spPrev = new JScrollPane(lblPreview);

        pAdjuntos.add(spList, BorderLayout.WEST);
        pAdjuntos.add(spPrev, BorderLayout.CENTER);
        miniTabs.addTab("Adjuntos", pAdjuntos);

        add(miniTabs, BorderLayout.CENTER);
    }

    public void setToggleIcon(String text) {
        if (btnQuickToggle != null)
            btnQuickToggle.setText(text);
    }

    public void addCustomTab(String title, javax.swing.JComponent component) {
        if (miniTabs != null) {
            miniTabs.addTab(title, component);
        }
    }

    public java.awt.Component getCustomTab(String title) {
        if (miniTabs != null) {
            int idx = miniTabs.indexOfTab(title);
            if (idx != -1) {
                return miniTabs.getComponentAt(idx);
            }
        }
        return null;
    }

    public void cycleTab(int direction) {
        if (miniTabs == null || miniTabs.getTabCount() == 0)
            return;
        int current = miniTabs.getSelectedIndex();
        int next = (current + direction) % miniTabs.getTabCount();
        if (next < 0)
            next += miniTabs.getTabCount();
        miniTabs.setSelectedIndex(next);
    }

    public void clear() {
        currentTask = null;
        txtQuickDesc.setText("");
        txtQuickNotes.setText("");
        attachmentListModel.clear();
        lblPreview.setIcon(null);
        lblPreview.setText("Seleccione una tarea");
    }

    public void updateTask(Task t) {
        this.currentTask = t;
        if (t == null) {
            clear();
            return;
        }

        renderDescription(t);
        txtQuickDesc.setCaretPosition(0);

        imagePlaceholders.clear();

        // 1. Initial Render
        renderNotes(t);

        // 2. Pre-download images
        if (t.attachments != null && dataService != null) {
            preDownloadImages(t.attachments);
        }

        // 3. Update Attachments List
        attachmentListModel.clear();
        if (t.attachments != null) {
            for (Attachment a : t.attachments) {
                attachmentListModel.addElement(a);
            }
        }
        lblPreview.setIcon(null);
        lblPreview.setText(attachmentListModel.isEmpty() ? "Sin adjuntos" : "Seleccione un adjunto");
    }

    public void refreshNotes() {
        if (currentTask != null) {
            renderNotes(currentTask);
        }
    }

    private void renderNotes(Task t) {
        String bgColor = toHex(styleConfig.bgMain);
        String textColor = toHex(styleConfig.textPrimary);
        StringBuilder sb = new StringBuilder(
                "<html><body style='font-family:Segoe UI; font-size:10px; background-color:")
                .append(bgColor).append("; color:").append(textColor).append(";'>");
        if (t.journals != null && !t.journals.isEmpty()) {
            int count = 0;
            if (sortNotesNewestFirst) {
                for (int i = t.journals.size() - 1; i >= 0 && count < 3; i--) {
                    appendJournalHtml(sb, t.journals.get(i));
                    count++;
                }
            } else {
                int startIdx = Math.max(0, t.journals.size() - 3);
                for (int i = startIdx; i < t.journals.size() && count < 3; i++) {
                    appendJournalHtml(sb, t.journals.get(i));
                    count++;
                }
            }
        } else {
            sb.append("Sin notas.");
        }
        sb.append("</body></html>");

        txtQuickNotes.setText(sb.toString());
        SwingUtilities.invokeLater(() -> txtQuickNotes.setCaretPosition(0));
    }

    private void renderDescription(Task t) {
        if (t.description == null || t.description.isEmpty()) {
            txtQuickDesc.setText("");
            return;
        }

        String bgColor = toHex(styleConfig.bgMain);
        String textColor = toHex(styleConfig.textPrimary);
        StringBuilder sb = new StringBuilder(
                "<html><body style='font-family:Segoe UI; font-size:12px; background-color:")
                .append(bgColor).append("; color:").append(textColor).append(";'>");

        String descWithPlaceholders = replaceRedmineImageMarkup(t.description);
        // Use TextileConverter.convertToHtml (it handles basic markup). 
        // If content is Markdown, it might look slightly off or raw, 
        // but images will be preserved by placeholders.
        String formattedDesc = TextileConverter.convertToHtml(descWithPlaceholders);
        String bodyContent = extractBody(formattedDesc);

        bodyContent = replacePlaceholdersWithImages(bodyContent);
        bodyContent = replaceImageReferencesWithHtmlTags(bodyContent);

        sb.append(bodyContent);
        sb.append("</body></html>");

        txtQuickDesc.setText(sb.toString());
        SwingUtilities.invokeLater(() -> txtQuickDesc.setCaretPosition(0));
    }

    private void appendJournalHtml(StringBuilder sb, Journal j) {
        if (j.notes != null && !j.notes.trim().isEmpty()) {
            String headerColor = toHex(styleConfig.bgHeader); // Use theme header color
            sb.append("<div style='background-color:").append(headerColor).append("; padding:2px;'><b>")
                    .append(j.user).append("</b> (").append(j.createdOn).append("):</div>");

            String notesWithPlaceholders = replaceRedmineImageMarkup(j.notes);
            String formattedNotes = TextileConverter.convertToHtml(notesWithPlaceholders);
            String bodyContent = extractBody(formattedNotes);

            bodyContent = replacePlaceholdersWithImages(bodyContent);
            bodyContent = replaceImageReferencesWithHtmlTags(bodyContent);

            sb.append("<div style='padding:2px 5px;'>").append(bodyContent).append("</div><hr>");
        }
    }

    private String extractBody(String html) {
        if (html.contains("<body")) {
            int bodyStart = html.indexOf(">", html.indexOf("<body")) + 1;
            int bodyEnd = html.lastIndexOf("</body>");
            if (bodyStart > 0 && bodyEnd > bodyStart) {
                return html.substring(bodyStart, bodyEnd);
            }
        }
        return html;
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // --- Image Handling helper methods (Same logic as before, now using Cache) ---

    private String replaceRedmineImageMarkup(String text) {
        if (text == null || text.isEmpty())
            return text;
        String tempDir = System.getProperty("java.io.tmpdir");
        // Support both Textile (!img!) and Markdown (![] (img))
        Pattern pattern = Pattern.compile("!([^!\\n]+\\.(?:png|jpg|jpeg|gif|bmp))!|!\\[.*?\\]\\(([^)]+\\.(?:png|jpg|jpeg|gif|bmp))\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String filename = matcher.group(1);
            if (filename == null) filename = matcher.group(2); // Markdown group
            
            java.io.File imageFile = new java.io.File(tempDir, filename);

            if (imageFile.exists()) {
                addPlaceholder(sb, matcher, imageFile);
                continue;
            }

            java.io.File cached = ImageCacheService.getInstance().getCachedImage(filename);
            if (cached != null) {
                addPlaceholder(sb, matcher, cached);
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void addPlaceholder(StringBuffer sb, Matcher matcher, java.io.File file) {
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String placeholder = "XXIMGPLACEHOLDERXX" + uuid + "XX";
        String imgTag = "<img src=\"file:///" + file.getAbsolutePath().replace("\\", "/")
                + "\" style='max-width:600px; height:auto;' />";
        imagePlaceholders.put(placeholder, imgTag);
        matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
    }

    private String replacePlaceholdersWithImages(String html) {
        if (html == null || html.isEmpty() || imagePlaceholders.isEmpty())
            return html;
        String result = html;
        for (Map.Entry<String, String> entry : imagePlaceholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String replaceImageReferencesWithHtmlTags(String html) {
        // Fallback cleaning
        if (html == null || html.isEmpty())
            return html;
        String tempDir = System.getProperty("java.io.tmpdir");
        // Support both Textile (!img!) and Markdown (![] (img))
        Pattern pattern = Pattern.compile("!([^!\\n]+\\.(?:png|jpg|jpeg|gif|bmp))!|!\\[.*?\\]\\(([^)]+\\.(?:png|jpg|jpeg|gif|bmp))\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String filename = matcher.group(1);
            if (filename == null) filename = matcher.group(2); // Markdown group
            
            java.io.File imageFile = new java.io.File(tempDir, filename);
            if (imageFile.exists()) {
                String imgTag = "<img src=\"file:///" + imageFile.getAbsolutePath().replace("\\", "/")
                        + "\" style='max-width:600px; height:auto;' />";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(imgTag));
                continue;
            }
            java.io.File cached = ImageCacheService.getInstance().getCachedImage(filename);
            if (cached != null) {
                String imgTag = "<img src=\"file:///" + cached.getAbsolutePath().replace("\\", "/")
                        + "\" style='max-width:600px; height:auto;' />";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(imgTag));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".bmp");
    }

    private void preDownloadImages(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty() || dataService == null)
            return;
        List<Attachment> toDownload = new ArrayList<>();
        ImageCacheService cache = ImageCacheService.getInstance();

        for (Attachment att : attachments) {
            if (att.filename != null && isImageFile(att.filename)) {
                if (cache.getCachedImage(att.filename) == null) {
                    toDownload.add(att);
                }
            }
        }

        if (toDownload.isEmpty())
            return;

        new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                boolean any = false;
                for (Attachment att : toDownload) {
                    try {
                        // Double check cache
                        if (cache.getCachedImage(att.filename) != null)
                            continue;

                        byte[] data = dataService.downloadAttachment(att);
                        if (data != null && data.length > 0) {
                            cache.saveImage(att.filename, data);
                            any = true;
                        }
                    } catch (Exception ignored) {
                    }
                }
                return any;
            }

            @Override
            protected void done() {
                try {
                    if (get() == Boolean.TRUE) {
                        refreshNotes();
                        renderDescription(currentTask);
                    }
                } catch (Exception e) {
                }
            }
        }.execute();
    }

    private void updateAttachmentPreview() {
        Attachment att = lstAttachments.getSelectedValue();
        if (att == null) {
            lblPreview.setIcon(null);
            lblPreview.setText("Seleccione un adjunto");
            return;
        }

        String fname = att.filename != null ? att.filename.toLowerCase() : "";
        boolean isPossibleImg = (att.contentType != null && att.contentType.startsWith("image/")) || isImageFile(fname);

        if (isPossibleImg && !fname.endsWith(".docx") && !fname.endsWith(".pdf") && !fname.endsWith(".zip")) {
            // Try cache first
            java.io.File cached = ImageCacheService.getInstance().getCachedImage(att.filename);
            if (cached != null) {
                try {
                    BufferedImage img = ImageIO.read(cached);
                    setPreviewImage(img);
                    return;
                } catch (Exception e) {
                }
            }

            // Download if not cached (and save to cache)
            lblPreview.setText("Cargando preview...");
            lblPreview.setIcon(null);
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    if (controller == null)
                        return null;
                    byte[] data = controller.downloadAttachment(att);
                    if (data == null || data.length == 0)
                        throw new Exception("Sin datos");

                    // Save to cache!
                    ImageCacheService.getInstance().saveImage(att.filename, data);

                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                    return scaleImage(img);
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
                        lblPreview.setText("Fallo: " + e.getMessage());
                    }
                }
            }.execute();

        } else {
            lblPreview.setIcon(null);
            String label = "Vista previa no disponible para ."
                    + (fname.contains(".") ? fname.substring(fname.lastIndexOf(".") + 1) : "archivos");
            lblPreview.setText("<html><center>" + label + "<br><br><b>Doble-clic para abrir</b></center></html>");
        }
    }

    private ImageIcon scaleImage(BufferedImage img) {
        if (img == null)
            return null;
        int w = lblPreview.getWidth() > 0 ? lblPreview.getWidth() : 300;
        int h = lblPreview.getHeight() > 0 ? lblPreview.getHeight() : 200;

        double scale = Math.min((double) w / img.getWidth(), (double) h / img.getHeight());
        if (scale < 1.0) {
            Image scaled = img.getScaledInstance((int) (img.getWidth() * scale), (int) (img.getHeight() * scale),
                    Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return new ImageIcon(img);
    }

    // Auxiliary for synchronous setting
    private void setPreviewImage(BufferedImage img) {
        ImageIcon icon = scaleImage(img);
        if (icon != null) {
            lblPreview.setIcon(icon);
            lblPreview.setText("");
        }
    }
}
