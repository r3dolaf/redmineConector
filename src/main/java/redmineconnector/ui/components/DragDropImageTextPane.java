package redmineconnector.ui.components;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JTextPane mejorado con soporte para:
 * - Pegar imágenes desde el portapapeles (Ctrl+V)
 * - Mostrar imágenes inline mientras se edita
 * - Arrastrar y soltar archivos de texto
 * - Generar texto con referencias de imagen para Redmine
 */
public class DragDropImageTextPane extends JTextPane {

    private static final Color DEFAULT_BORDER_COLOR = new Color(200, 200, 200);
    private static final Color HOVER_BORDER_COLOR = new Color(100, 150, 255);
    private static final Color DROP_BORDER_COLOR = new Color(100, 200, 100);

    // Lista de imágenes pegadas (para subirlas como adjuntos)
    private final List<File> pastedImages = new ArrayList<>();

    // Handler para descargar adjuntos desde Redmine
    private ImageDownloadHandler downloadHandler;

    // Lista de adjuntos disponibles (para buscar imágenes)
    private java.util.List<redmineconnector.model.Attachment> availableAttachments;

    private File imageCacheDir;

    public void setImageCacheDir(File dir) {
        this.imageCacheDir = dir;
    }

    // Formatos de texto soportados para drag & drop
    private static final List<String> TEXT_EXTENSIONS = Arrays.asList(
            "txt", "md", "log", "json", "xml", "properties", "yml", "yaml",
            "java", "js", "py", "html", "css", "sql", "sh", "bat", "csv");

    /**
     * Constructor con filas y columnas (similar a JTextArea).
     */
    public DragDropImageTextPane(int rows, int columns) {
        super();
        setupComponent(rows, columns);
        setupDragAndDrop();
        setupClipboardPaste();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Constructor por defecto.
     */
    public DragDropImageTextPane() {
        this(10, 40);
    }

    /**
     * Interface para descargar imágenes desde Redmine.
     */
    public interface ImageDownloadHandler {
        void downloadAttachment(redmineconnector.model.Attachment att, File destFile);
    }

    /**
     * Establece el handler para descargar imágenes.
     */
    public void setDownloadHandler(ImageDownloadHandler handler) {
        this.downloadHandler = handler;
    }

    /**
     * Establece la lista de adjuntos disponibles.
     */
    public void setAvailableAttachments(java.util.List<redmineconnector.model.Attachment> attachments) {
        this.availableAttachments = attachments;
    }

    /**
     * Configura el componente básico.
     */
    private void setupComponent(int rows, int columns) {
        setFont(new Font("Segoe UI", Font.PLAIN, 12));
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(DEFAULT_BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Initialize rows/cols for preferred scrollable size calculation
        this.rows = rows;
        this.columns = columns;
    }

    private int rows;
    private int columns;

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int width = fm.charWidth('m') * columns;
        int height = fm.getHeight() * rows;
        return new Dimension(width, height);
    }

    /**
     * Configura drag & drop para archivos de texto.
     */
    private void setupDragAndDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(HOVER_BORDER_COLOR, 2),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(DEFAULT_BORDER_COLOR, 1),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        java.util.List<File> files = (java.util.List<File>) transferable
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        handleDroppedFiles(files);
                        dtde.dropComplete(true);
                    } else {
                        dtde.dropComplete(false);
                    }
                } catch (Exception e) {
                    dtde.dropComplete(false);
                } finally {
                    setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(DEFAULT_BORDER_COLOR, 1),
                            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                }
            }
        });
    }

    /**
     * Configura el atajo Ctrl+V para pegar imágenes.
     */
    private void setupClipboardPaste() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Ctrl+V
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    if (ClipboardImageHandler.hasImageInClipboard()) {
                        e.consume(); // Prevenir el pegado normal
                        pasteImageFromClipboard();
                    }
                }
            }
        });
    }

    /**
     * Pega una imagen desde el portapapeles.
     */
    public void pasteImageFromClipboard() {
        BufferedImage image = ClipboardImageHandler.getImageFromClipboard();
        if (image == null) {
            return;
        }

        File imageFile;
        if (imageCacheDir != null) {
            imageFile = ClipboardImageHandler.saveImageToDir(image, "screenshot", imageCacheDir);
        } else {
            imageFile = ClipboardImageHandler.saveImageToTempFile(image, "screenshot");
        }
        if (imageFile == null) {
            showError("No se pudo guardar la imagen");
            return;
        }

        insertImage(imageFile);
        pastedImages.add(imageFile);
    }

    /**
     * Inserta una imagen en la posición actual del cursor.
     */
    public void insertImage(File imageFile) {
        try {
            // Cargar imagen de forma robusta
            BufferedImage img = javax.imageio.ImageIO.read(imageFile);
            if (img == null) {
                throw new Exception("No se pudo leer la imagen");
            }
            ImageIcon icon = new ImageIcon(img);

            // Escalar si es muy grande (máximo 600px de ancho)
            if (icon.getIconWidth() > 600) {
                Image scaledImage = icon.getImage().getScaledInstance(
                        600, -1, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaledImage);
            }

            // Insertar imagen en el documento
            StyledDocument doc = getStyledDocument();
            int pos = getCaretPosition();

            // Insertar salto de línea antes si no estamos al inicio de una línea
            if (pos > 0) {
                try {
                    String prevChar = doc.getText(pos - 1, 1);
                    if (!prevChar.equals("\n")) {
                        doc.insertString(pos, "\n", null);
                        pos++;
                    }
                } catch (BadLocationException e) {
                    // Ignorar
                }
            }

            // Insertar la imagen
            setCaretPosition(pos);
            insertIcon(icon);

            // Guardar referencia al archivo en el atributo del documento
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            attrs.addAttribute("image-file", imageFile.getName());
            doc.setCharacterAttributes(pos, 1, attrs, false);

            // Insertar salto de línea después
            doc.insertString(pos + 1, "\n", null);
            setCaretPosition(pos + 2);

        } catch (Exception e) {
            showError("Error al insertar imagen: " + e.getMessage());
        }
    }

    /**
     * Maneja archivos de texto soltados.
     */
    private void handleDroppedFiles(List<File> files) {
        StringBuilder content = new StringBuilder();

        for (File file : files) {
            if (isTextFile(file)) {
                try {
                    // Leer contenido del archivo (Java 8 compatible)
                    StringBuilder fileContent = new StringBuilder();
                    BufferedReader reader = new BufferedReader(
                            new java.io.FileReader(file));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line).append("\n");
                    }
                    reader.close();

                    if (files.size() > 1) {
                        content.append("\n--- ").append(file.getName()).append(" ---\n");
                    }

                    content.append(fileContent.toString());

                    if (files.size() > 1) {
                        content.append("\n");
                    }
                } catch (Exception e) {
                    showError("Error al leer " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        if (content.length() > 0) {
            insertTextAtCursor(content.toString());
        }
    }

    /**
     * Verifica si un archivo es de texto.
     */
    private boolean isTextFile(File file) {
        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < name.length() - 1) {
            String extension = name.substring(dotIndex + 1);
            return TEXT_EXTENSIONS.contains(extension);
        }
        return false;
    }

    /**
     * Inserta texto en la posición del cursor.
     */
    private void insertTextAtCursor(String text) {
        try {
            StyledDocument doc = getStyledDocument();
            doc.insertString(getCaretPosition(), text, null);
        } catch (BadLocationException e) {
            showError("Error al insertar texto: " + e.getMessage());
        }
    }

    /**
     * Obtiene el texto con referencias de imagen en formato Redmine.
     * Las imágenes se reemplazan por !filename.png!
     */
    public String getTextWithImageReferences() {
        try {
            StyledDocument doc = getStyledDocument();
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < doc.getLength(); i++) {
                Element element = doc.getCharacterElement(i);
                AttributeSet attrs = element.getAttributes();

                // Verificar si es una imagen
                Object imageName = attrs.getAttribute("image-file");
                if (imageName != null) {
                    // Insertar referencia Redmine
                    result.append("!").append(imageName.toString()).append("!");
                } else {
                    // Texto normal
                    result.append(doc.getText(i, 1));
                }
            }

            return result.toString();
        } catch (BadLocationException e) {
            return getText();
        }
    }

    /**
     * Obtiene la lista de imágenes pegadas.
     */
    public List<File> getPastedImages() {
        return new ArrayList<>(pastedImages);
    }

    /**
     * Limpia la lista de imágenes pegadas.
     */
    public void clearPastedImages() {
        pastedImages.clear();
    }

    /**
     * Muestra un mensaje de error.
     */
    private void showError(String message) {
        setToolTipText(message);
        Timer timer = new Timer(3000, e -> setToolTipText(null));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Override de setText para parsear markup de Redmine y mostrar imágenes inline.
     * Detecta patrones como !imagen.png! y los renderiza como imágenes.
     */
    @Override
    public void setText(String text) {
        if (text == null || text.isEmpty()) {
            super.setText("");
            return;
        }

        // Limpiar el documento
        try {
            StyledDocument doc = getStyledDocument();
            doc.remove(0, doc.getLength());

            // Parsear y renderizar el texto con imágenes
            parseAndRenderText(text);

        } catch (BadLocationException e) {
            // Fallback: usar setText normal
            super.setText(text);
        }
    }

    /**
     * Parsea texto con markup de Redmine y renderiza imágenes inline.
     * Formato: !nombre_imagen.png!
     */
    private void parseAndRenderText(String text) throws BadLocationException {
        StyledDocument doc = getStyledDocument();
        int pos = 0;

        // Patrón para detectar imágenes: !filename.ext! OR ![](filename.ext)
        // Group 1: Textile format (filename)
        // Group 2: Markdown format (filename) inside ![]()
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "!([^!\\n]+\\.(?:png|jpg|jpeg|gif|bmp))!|!\\[.*?\\]\\(([^)]+\\.(?:png|jpg|jpeg|gif|bmp))\\)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            // Insertar texto antes de la imagen
            if (matcher.start() > lastEnd) {
                String beforeText = text.substring(lastEnd, matcher.start());
                doc.insertString(pos, beforeText, null);
                pos += beforeText.length();
            }

            // Intentar cargar y mostrar la imagen
            // Check which group matched
            String imageName = matcher.group(1); // Textile
            if (imageName == null) {
                imageName = matcher.group(2); // Markdown
            }
            
            boolean imageInserted = false;
            if (imageName != null) {
                 imageInserted = tryInsertImageFromName(imageName, pos);
            }

            if (imageInserted) {
                pos += 1; // La imagen ocupa 1 posición en el documento
            } else {
                // Si no se puede cargar, insertar el markup original
                String markup = matcher.group(0);
                doc.insertString(pos, markup, null);
                pos += markup.length();
            }

            lastEnd = matcher.end();
        }

        // Insertar texto restante
        if (lastEnd < text.length()) {
            doc.insertString(pos, text.substring(lastEnd), null);
        }

        // Mover cursor al inicio
        setCaretPosition(0);
    }

    /**
     * Intenta insertar una imagen desde su nombre.
     * Busca en archivos temporales o adjuntos descargados.
     */
    private boolean tryInsertImageFromName(String imageName, int pos) {
        try {
            // Buscar en directorio cache o temporal
            File searchDir = imageCacheDir != null ? imageCacheDir : new File(System.getProperty("java.io.tmpdir"));
            File imageFile = new File(searchDir, imageName);

            // Si no existe, intentar descargar desde Redmine
            if (!imageFile.exists() && downloadHandler != null && availableAttachments != null) {
                imageFile = downloadImageFromAttachments(imageName);
            }

            if (imageFile != null && imageFile.exists() && imageFile.isFile()) {
                // Cargar y mostrar imagen de forma robusta
                BufferedImage img = javax.imageio.ImageIO.read(imageFile);
                if (img == null) {
                    throw new Exception("No se pudo leer la imagen");
                }
                ImageIcon icon = new ImageIcon(img);

                // Escalar si es muy grande
                if (icon.getIconWidth() > 600) {
                    Image scaledImage = icon.getImage().getScaledInstance(
                            600, -1, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaledImage);
                }

                // Insertar imagen
                StyledDocument doc = getStyledDocument();
                setCaretPosition(pos);
                insertIcon(icon);

                // Guardar referencia al archivo
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                attrs.addAttribute("image-file", imageName);
                doc.setCharacterAttributes(pos, 1, attrs, false);

                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Descarga una imagen desde los adjuntos de Redmine.
     */
    private File downloadImageFromAttachments(String imageName) {
        try {
            // Buscar el adjunto por nombre
            for (redmineconnector.model.Attachment att : availableAttachments) {
                if (att.filename != null && att.filename.equals(imageName)) {
                    // Descargar a directorio temporal o cache
                    File destDir = imageCacheDir != null ? imageCacheDir
                            : new File(System.getProperty("java.io.tmpdir"));
                    File destFile = new File(destDir, imageName);

                    // Usar el handler para descargar
                    downloadHandler.downloadAttachment(att, destFile);

                    if (destFile.exists()) {
                        return destFile;
                    }
                }
            }
        } catch (Exception e) {
            // Error al descargar, continuar sin imagen
        }
        return null;
    }
    /**
     * Re-escanea el documento buscando markup de imágenes (!img! o ![]())
     * que no se pudieron renderizar previamente, e intenta cargarlas de nuevo.
     * Útil cuando las imágenes se descargan asíncronamente.
     */
    public void refreshImages() {
       String currentText = getText(); // Obtiene el texto plano
       int caret = getCaretPosition();
       
       // Re-set el texto fuerza el re-renderizado de todo
       setText(currentText);
       
       try {
           setCaretPosition(Math.min(caret, getDocument().getLength()));
       } catch (Exception e) {}
    }
}
