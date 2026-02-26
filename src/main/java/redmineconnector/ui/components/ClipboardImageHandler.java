package redmineconnector.ui.components;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import redmineconnector.util.LoggerUtil;

/**
 * Utilidad para manejar operaciones de portapapeles relacionadas con imágenes.
 * Permite detectar, extraer y guardar imágenes desde el portapapeles del
 * sistema.
 */
public class ClipboardImageHandler {

    /**
     * Verifica si el portapapeles contiene una imagen.
     *
     * @return true si hay una imagen en el portapapeles, false en caso contrario
     */
    public static boolean hasImageInClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents == null) {
                return false;
            }

            return contents.isDataFlavorSupported(DataFlavor.imageFlavor);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene la imagen del portapapeles.
     *
     * @return BufferedImage con la imagen del portapapeles, o null si no hay imagen
     */
    public static BufferedImage getImageFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents == null || !contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return null;
            }

            Object imageData = contents.getTransferData(DataFlavor.imageFlavor);

            if (imageData instanceof BufferedImage) {
                return (BufferedImage) imageData;
            } else if (imageData instanceof Image) {
                return toBufferedImage((Image) imageData);
            }

            return null;
        } catch (Exception e) {
            LoggerUtil.logError("ClipboardImageHandler", "Error getting image from clipboard", e);
            return null;
        }
    }

    /**
     * Guarda una imagen en un archivo temporal PNG.
     *
     * @param image  la imagen a guardar
     * @param prefix prefijo para el nombre del archivo (ej: "screenshot", "paste")
     * @return File con la imagen guardada, o null si hubo un error
     */
    public static File saveImageToTempFile(BufferedImage image, String prefix) {
        if (image == null) {
            return null;
        }

        try {
            // Generar nombre único con timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = prefix + "_" + timestamp + ".png";

            // Crear archivo temporal
            File tempFile = File.createTempFile(prefix + "_", ".png");
            tempFile.deleteOnExit(); // Eliminar al cerrar la aplicación

            // Guardar imagen como PNG
            ImageIO.write(image, "png", tempFile);

            // Renombrar con nombre descriptivo
            File finalFile = new File(tempFile.getParent(), filename);
            if (tempFile.renameTo(finalFile)) {
                finalFile.deleteOnExit();
                return finalFile;
            } else {
                return tempFile;
            }
        } catch (Exception e) {
            LoggerUtil.logError("ClipboardImageHandler", "Error saving image", e);
            return null;
        }
    }

    /**
     * Guarda una imagen en un directorio específico.
     *
     * @param image   la imagen a guardar
     * @param prefix  prefijo para el nombre del archivo
     * @param destDir directorio de destino
     * @return File con la imagen guardada, o null si hubo un error
     */
    public static File saveImageToDir(BufferedImage image, String prefix, File destDir) {
        if (image == null || destDir == null) {
            return null;
        }

        try {
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            // Generar nombre único con timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = prefix + "_" + timestamp + ".png";
            File finalFile = new File(destDir, filename);

            // Guardar imagen como PNG
            ImageIO.write(image, "png", finalFile);

            return finalFile;
        } catch (Exception e) {
            LoggerUtil.logError("ClipboardImageHandler", "Error saving image to dir", e);
            return null;
        }
    }

    /**
     * Convierte una Image a BufferedImage.
     *
     * @param img la imagen a convertir
     * @return BufferedImage
     */
    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Crear BufferedImage con las dimensiones de la imagen original
        BufferedImage bufferedImage = new BufferedImage(
                img.getWidth(null),
                img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        // Dibujar la imagen en el BufferedImage
        java.awt.Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return bufferedImage;
    }

    /**
     * Método de conveniencia para obtener y guardar imagen del portapapeles en un
     * solo paso.
     *
     * @param prefix prefijo para el nombre del archivo
     * @return File con la imagen guardada, o null si no hay imagen o hubo un error
     */
    public static File getAndSaveClipboardImage(String prefix) {
        BufferedImage image = getImageFromClipboard();
        if (image == null) {
            return null;
        }
        return saveImageToTempFile(image, prefix);
    }
}
