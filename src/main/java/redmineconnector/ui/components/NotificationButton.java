package redmineconnector.ui.components;

import redmineconnector.notifications.NotificationManager;

import javax.swing.*;
import java.awt.*;

/**
 * Button that displays notification icon with unread count badge.
 */
public class NotificationButton extends JButton {

    private int unreadCount = 0;
    private final Runnable onClick;

    public NotificationButton(Runnable onClick) {
        super("🔔");
        this.onClick = onClick;

        // Visual settings
        setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        setToolTipText("Centro de Notificaciones");
        setBackground(new Color(250, 240, 250)); // Light purple/pink
        setFocusPainted(false);
        setMargin(new Insets(2, 6, 2, 6)); // Tighter margins

        addActionListener(e -> {
            if (onClick != null) {
                onClick.run();
            }
        });

        // Listen for notification changes
        NotificationManager.addNotificationListener(n -> SwingUtilities.invokeLater(this::updateBadge));
        updateBadge();
    }

    public void updateBadge() {
        unreadCount = NotificationManager.getUnreadCount();
        // Dynamic background for visibility
        if (unreadCount > 0) {
            setBackground(new Color(255, 225, 225)); // Alert: Pale red background
        } else {
            setBackground(new Color(250, 240, 250)); // Default: Light purple/pink
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (unreadCount > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw badge
            int badgeSize = 18;
            int x = getWidth() - badgeSize - 2;
            int y = 2;

            g2.setColor(new Color(220, 53, 69)); // Red
            g2.fillOval(x, y, badgeSize, badgeSize);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String text = unreadCount > 9 ? "9+" : String.valueOf(unreadCount);
            FontMetrics fm = g2.getFontMetrics();
            int textX = x + (badgeSize - fm.stringWidth(text)) / 2;
            int textY = y + ((badgeSize - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(text, textX, textY);

            g2.dispose();
        }
    }
}
