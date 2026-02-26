package redmineconnector.ui.components;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel que permite arrastrar y soltar archivos.
 * Muestra una zona visual de drop y lista de archivos seleccionados.
 */
public class DragDropFilePanel extends JPanel {

    private final List<File> selectedFiles = new ArrayList<>();
    private final DefaultListModel<String> fileListModel = new DefaultListModel<>();
    private final JList<String> fileList = new JList<>(fileListModel);
    private JLabel dropZoneLabel;
    private final Consumer<List<File>> onFilesChanged;

    private static final Color DROP_ZONE_NORMAL = new Color(240, 248, 255);
    private static final Color DROP_ZONE_HOVER = new Color(220, 240, 255);
    private static final Color DROP_ZONE_BORDER = new Color(100, 149, 237);

    /**
     * Constructor del panel de drag & drop.
     * 
     * @param onFilesChanged Callback que se ejecuta cuando cambia la lista de
     *                       archivos
     */
    public DragDropFilePanel(Consumer<List<File>> onFilesChanged) {
        this.onFilesChanged = onFilesChanged;
        setLayout(new BorderLayout(10, 10));

        // Zona de drop
        JPanel dropZone = createDropZone();

        // Lista de archivos
        JPanel fileListPanel = createFileListPanel();

        // Añadir componentes
        add(dropZone, BorderLayout.NORTH);
        add(fileListPanel, BorderLayout.CENTER);

        // Configurar clipboard paste
        setupClipboardPaste();
    }

    /**
     * Crea la zona visual donde se arrastran los archivos.
     */
    private JPanel createDropZone() {
        JPanel dropZone = new JPanel(new BorderLayout());
        dropZone.setPreferredSize(new Dimension(400, 100));
        dropZone.setBackground(DROP_ZONE_NORMAL);
        dropZone.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(DROP_ZONE_BORDER, 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // Label con instrucciones
        dropZoneLabel = new JLabel(
                "<html><center>" +
                        "<b style='font-size: 14px;'>📎 Arrastra archivos aquí</b><br>" +
                        "<span style='color: gray; font-size: 11px;'>o haz clic para seleccionar, o Ctrl+V para pegar</span>"
                        +
                        "</center></html>",
                SwingConstants.CENTER);
        dropZoneLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        dropZone.add(dropZoneLabel, BorderLayout.CENTER);

        // Configurar drag & drop
        setupDragAndDrop(dropZone);

        // Click para abrir file chooser
        dropZone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dropZone.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openFileChooser();
            }
        });

        return dropZone;
    }

    /**
     * Configura el sistema de drag & drop.
     */
    private void setupDragAndDrop(JPanel dropZone) {
        new DropTarget(dropZone, new DropTargetAdapter() {

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                // Accept drag if it contains files
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    dropZone.setBackground(DROP_ZONE_HOVER);
                    dropZoneLabel.setText(
                            "<html><center>" +
                                    "<b style='font-size: 14px; color: #4169E1;'>⬇️ Suelta los archivos aquí</b>" +
                                    "</center></html>");
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                // Continue accepting drag
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                resetDropZone(dropZone);
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    // Accept drop first
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) transferable.getTransferData(
                                DataFlavor.javaFileListFlavor);

                        if (droppedFiles != null && !droppedFiles.isEmpty()) {
                            addFiles(droppedFiles);
                            dtde.dropComplete(true);

                            // Mostrar feedback visual
                            showSuccessFeedback(dropZone, droppedFiles.size());
                        } else {
                            dtde.dropComplete(false);
                        }
                    } else {
                        dtde.rejectDrop();
                        dtde.dropComplete(false);
                    }
                } catch (Exception e) {
                    redmineconnector.util.LoggerUtil.logError("DragDropFilePanel",
                            "Error processing dropped files", e);
                    dtde.dropComplete(false);
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = e.getMessage();
                        if (errorMsg == null || errorMsg.trim().isEmpty()) {
                            errorMsg = e.getClass().getSimpleName() + ": Error al procesar los archivos";
                        }
                        JOptionPane.showMessageDialog(
                                DragDropFilePanel.this,
                                "Error al procesar archivos: " + errorMsg,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                } finally {
                    resetDropZone(dropZone);
                }
            }
        });
    }

    /**
     * Muestra feedback visual de éxito.
     */
    private void showSuccessFeedback(JPanel dropZone, int fileCount) {
        dropZone.setBackground(new Color(220, 255, 220));
        dropZoneLabel.setText(
                "<html><center>" +
                        "<b style='font-size: 14px; color: green;'>✓ " + fileCount +
                        " archivo(s) añadido(s)</b>" +
                        "</center></html>");

        // Volver a normal después de 1 segundo
        Timer timer = new Timer(1000, e -> resetDropZone(dropZone));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Resetea el aspecto visual de la zona de drop.
     */
    private void resetDropZone(JPanel dropZone) {
        dropZone.setBackground(DROP_ZONE_NORMAL);
        dropZoneLabel.setText(
                "<html><center>" +
                        "<b style='font-size: 14px;'>📎 Arrastra archivos aquí</b><br>" +
                        "<span style='color: gray; font-size: 11px;'>o haz clic para seleccionar, o Ctrl+V para pegar</span>"
                        +
                        "</center></html>");
    }

    /**
     * Abre un file chooser tradicional.
     */
    private void openFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            addFiles(java.util.Arrays.asList(files));
        }
    }

    /**
     * Crea el panel con la lista de archivos seleccionados.
     */
    private JPanel createFileListPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Label
        JLabel label = new JLabel("Archivos seleccionados:");
        label.setFont(new Font("Arial", Font.BOLD, 11));
        panel.add(label, BorderLayout.NORTH);

        // Lista
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(400, 150));
        panel.add(scrollPane, BorderLayout.CENTER);

        // No buttons needed - files are auto-uploaded and panel is auto-cleared

        return panel;
    }

    /**
     * Añade archivos a la lista.
     */
    private void addFiles(List<File> files) {
        for (File file : files) {
            if (!selectedFiles.contains(file)) {
                selectedFiles.add(file);
                fileListModel.addElement(formatFileName(file));
            }
        }
        notifyFilesChanged();
    }

    /**
     * Formatea el nombre del archivo para mostrar.
     */
    private String formatFileName(File file) {
        long sizeKB = file.length() / 1024;
        String size = sizeKB < 1024
                ? sizeKB + " KB"
                : String.format("%.1f MB", sizeKB / 1024.0);
        return String.format("📄 %s (%s)", file.getName(), size);
    }

    /**
     * Elimina los archivos seleccionados de la lista.
     */
    private void removeSelectedFiles() {
        int[] selectedIndices = fileList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            return;
        }

        // Eliminar de atrás hacia adelante para mantener índices válidos
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            int index = selectedIndices[i];
            selectedFiles.remove(index);
            fileListModel.remove(index);
        }
        notifyFilesChanged();
    }

    /**
     * Limpia todos los archivos.
     */
    private void clearAllFiles() {
        if (selectedFiles.isEmpty()) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar todos los archivos de la lista?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            selectedFiles.clear();
            fileListModel.clear();
            notifyFilesChanged();
        }
    }

    /**
     * Notifica cambios en la lista de archivos.
     */
    private void notifyFilesChanged() {
        if (onFilesChanged != null) {
            onFilesChanged.accept(new ArrayList<>(selectedFiles));
        }
    }

    /**
     * Obtiene la lista de archivos seleccionados.
     */
    public List<File> getSelectedFiles() {
        return new ArrayList<>(selectedFiles);
    }

    /**
     * Limpia la lista de archivos programáticamente.
     */
    public void clear() {
        selectedFiles.clear();
        fileListModel.clear();
        notifyFilesChanged();
    }

    /**
     * Limpia la lista de archivos SIN notificar cambios.
     * Útil para evitar recursión cuando se llama desde el callback.
     */
    public void clearSilently() {
        selectedFiles.clear();
        fileListModel.clear();
        // NO llama a notifyFilesChanged() para evitar recursión
    }

    /**
     * Establece archivos programáticamente.
     */
    public void setFiles(List<File> files) {
        clear();
        addFiles(files);
    }

    /**
     * Configura el atajo Ctrl+V para pegar imágenes desde el portapapeles.
     */
    private void setupClipboardPaste() {
        // Hacer el panel focusable
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Ctrl+V
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    pasteImageFromClipboard();
                }
            }
        });
    }

    /**
     * Pega una imagen desde el portapapeles.
     */
    private void pasteImageFromClipboard() {
        if (!ClipboardImageHandler.hasImageInClipboard()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No hay ninguna imagen en el portapapeles.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BufferedImage image = ClipboardImageHandler.getImageFromClipboard();
        if (image == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error al obtener la imagen del portapapeles.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        File imageFile = ClipboardImageHandler.saveImageToTempFile(image, "pasted");
        if (imageFile == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error al guardar la imagen.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Añadir a la lista
        List<File> files = new ArrayList<>();
        files.add(imageFile);
        addFiles(files);

        // Mostrar feedback
        JOptionPane.showMessageDialog(
                this,
                "Imagen pegada: " + imageFile.getName(),
                "Éxito",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
