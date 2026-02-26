package redmineconnector.ui.components;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

/**
 * JTextArea mejorado que soporta drag & drop de archivos de texto.
 * Útil para añadir notas arrastrando archivos .txt, .md, etc.
 */
public class DragDropTextArea extends JTextArea {

    private static final Color BORDER_NORMAL = new Color(180, 180, 180);
    private static final Color BORDER_HOVER = new Color(100, 149, 237);
    private static final Color BACKGROUND_HOVER = new Color(240, 248, 255);

    private Color originalBackground;
    private boolean isDragOver = false;

    /**
     * Constructor con configuración por defecto.
     */
    public DragDropTextArea() {
        this(5, 30);
    }

    /**
     * Constructor con dimensiones personalizadas.
     */
    public DragDropTextArea(int rows, int columns) {
        super(rows, columns);
        initialize();
    }

    /**
     * Inicializa el componente y configura drag & drop.
     */
    private void initialize() {
        originalBackground = getBackground();
        setLineWrap(true);
        setWrapStyleWord(true);
        setFont(new Font("Monospaced", Font.PLAIN, 12));
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_NORMAL, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setupDragAndDrop();
        setupTooltip();
    }

    /**
     * Configura el tooltip informativo.
     */
    private void setupTooltip() {
        setToolTipText(
                "<html>" +
                        "<b>Puedes arrastrar archivos de texto aquí</b><br>" +
                        "Formatos soportados: .txt, .md, .log, .json, .xml, .properties" +
                        "</html>");
    }

    /**
     * Configura el sistema de drag & drop.
     */
    private void setupDragAndDrop() {
        new DropTarget(this, new DropTargetAdapter() {

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (isAcceptableFile(dtde)) {
                    isDragOver = true;
                    updateVisualFeedback();
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                isDragOver = false;
                updateVisualFeedback();
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                isDragOver = false;
                updateVisualFeedback();

                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable.getTransferData(
                                DataFlavor.javaFileListFlavor);

                        handleDroppedFiles(files);
                        dtde.dropComplete(true);
                    } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        // Soportar texto plano arrastrado
                        String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                        insertTextAtCursor(text);
                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    redmineconnector.util.LoggerUtil.logError("DragDropTextArea",
                            "Error processing dropped file", e);
                    dtde.rejectDrop();
                    showError("Error al procesar archivo: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Verifica si el archivo arrastrado es aceptable.
     */
    private boolean isAcceptableFile(DropTargetDragEvent dtde) {
        try {
            Transferable transferable = dtde.getTransferable();
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) transferable.getTransferData(
                        DataFlavor.javaFileListFlavor);

                // Solo aceptar archivos de texto
                for (File file : files) {
                    if (!isTextFile(file)) {
                        return false;
                    }
                }
                return true;
            }
            return transferable.isDataFlavorSupported(DataFlavor.stringFlavor);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si un archivo es de texto.
     */
    private boolean isTextFile(File file) {
        if (!file.isFile()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        return name.endsWith(".txt") ||
                name.endsWith(".md") ||
                name.endsWith(".log") ||
                name.endsWith(".json") ||
                name.endsWith(".xml") ||
                name.endsWith(".properties") ||
                name.endsWith(".yml") ||
                name.endsWith(".yaml") ||
                name.endsWith(".csv") ||
                name.endsWith(".ini");
    }

    /**
     * Maneja los archivos soltados.
     */
    private void handleDroppedFiles(List<File> files) {
        StringBuilder content = new StringBuilder();

        for (File file : files) {
            if (isTextFile(file)) {
                try {
                    // Java 8 compatible file reading
                    StringBuilder fileContent = new StringBuilder();
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.FileReader(file));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line).append("\n");
                    }
                    reader.close();

                    if (files.size() > 1) {
                        // Si hay múltiples archivos, añadir separador
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
            showSuccess(files.size() + " archivo(s) insertado(s)");
        }
    }

    /**
     * Inserta texto en la posición del cursor.
     */
    private void insertTextAtCursor(String text) {
        int caretPosition = getCaretPosition();

        try {
            // Si hay texto seleccionado, reemplazarlo
            if (getSelectedText() != null) {
                replaceSelection(text);
            } else {
                // Insertar en la posición del cursor
                getDocument().insertString(caretPosition, text, null);
            }
        } catch (Exception e) {
            // Si falla, añadir al final
            append(text);
        }
    }

    /**
     * Actualiza el feedback visual durante drag.
     */
    private void updateVisualFeedback() {
        if (isDragOver) {
            setBackground(BACKGROUND_HOVER);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_HOVER, 2),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        } else {
            setBackground(originalBackground);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_NORMAL, 1),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        }
    }

    /**
     * Muestra mensaje de éxito temporal.
     */
    private void showSuccess(String message) {
        Color originalBg = getBackground();
        setBackground(new Color(220, 255, 220));

        Timer timer = new Timer(1000, e -> setBackground(originalBg));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Muestra mensaje de error.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
