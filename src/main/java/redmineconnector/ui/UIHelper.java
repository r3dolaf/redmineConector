package redmineconnector.ui;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

public class UIHelper {
    public static void addEscapeListener(JDialog dialog) {
        ActionListener escAction = e -> dialog.dispose();
        dialog.getRootPane().registerKeyboardAction(escAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static javax.swing.Icon getRadioIcon(boolean selected) {
        return new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.WHITE);
                g2.fillOval(x, y, 12, 12);
                g2.setColor(java.awt.Color.GRAY);
                g2.drawOval(x, y, 12, 12);
                if (selected) {
                    g2.setColor(new java.awt.Color(50, 150, 250));
                    g2.fillOval(x + 3, y + 3, 7, 7);
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 13;
            }

            @Override
            public int getIconHeight() {
                return 13;
            }
        };
    }

    public static void copyToClipboard(String text) {
        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    public static java.io.File exportResource(String resourceName) throws java.io.IOException {
        java.io.InputStream is = UIHelper.class.getResourceAsStream("/" + resourceName);
        if (is == null) {
            // Try without leading slash just in case
            is = UIHelper.class.getResourceAsStream(resourceName);
        }
        if (is == null) {
            throw new java.io.IOException("Recurso no encontrado: " + resourceName);
        }

        java.io.File tempFile = java.io.File.createTempFile("redmine_manual_", "_" + resourceName);
        tempFile.deleteOnExit();

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
}
