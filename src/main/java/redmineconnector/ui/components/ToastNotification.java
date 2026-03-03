package redmineconnector.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import redmineconnector.util.LoggerUtil;

/**
 * A non-intrusive toast notification window that appears typically at the
 * bottom-right
 * of the parent window or screen and auto-dismisses after a short delay.
 */
public class ToastNotification extends JWindow {

    public enum Type {
        INFO(new Color(60, 150, 255), "ℹ️"),
        WARNING(new Color(255, 165, 0), "⚠️"),
        ERROR(new Color(255, 80, 80), "❌"),
        SUCCESS(new Color(46, 204, 113), "✅");

        final Color color;
        final String icon;

        Type(Color color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    private static final int TOAST_WIDTH = 350;
    private static final int TOAST_HEIGHT = 80;
    private static final int MARGIN = 20;

    public ToastNotification(Window owner, String message, Type type) {
        super(owner);
        initUI(message, type);
    }

    private void initUI(String message, Type type) {
        setLayout(new BorderLayout());
        setAlwaysOnTop(true);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                // Custom painting for rounded border and background
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                // Draw left colored strip
                g2.setColor(type.color);
                g2.fillRoundRect(0, 0, 10, getHeight() - 1, 10, 10);
                g2.fillRect(5, 0, 5, getHeight() - 1); // Square off the right side of the strip

                // Draw border
                g2.setColor(new Color(200, 200, 200));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(10, 15, 10, 10));

        // Icon
        JLabel lblIcon = new JLabel(type.icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        contentPanel.add(lblIcon, BorderLayout.WEST);

        // Message
        JLabel lblMessage = new JLabel("<html><body style='width: 250px'>" + message + "</body></html>");
        lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(lblMessage, BorderLayout.CENTER);

        // Close button
        JButton btnClose = new JButton("×");
        btnClose.setFont(new Font("Arial", Font.BOLD, 16));
        btnClose.setForeground(Color.GRAY);
        btnClose.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        btnClose.setContentAreaFilled(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> closeToast());

        // Panel for close button to align it top-right
        JPanel closePanel = new JPanel(new BorderLayout());
        closePanel.setOpaque(false);
        closePanel.add(btnClose, BorderLayout.NORTH);
        contentPanel.add(closePanel, BorderLayout.EAST);

        add(contentPanel);
        setSize(TOAST_WIDTH, TOAST_HEIGHT);

        // Add click listener to dismiss
        contentPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                closeToast();
            }
        });
    }

    public void showToast() {
        if (getOwner() != null) {
            // Position relative to owner (bottom-right)
            int x = getOwner().getX() + getOwner().getWidth() - getWidth() - MARGIN;
            int y = getOwner().getY() + getOwner().getHeight() - getHeight() - MARGIN;
            setLocation(x, y);
        } else {
            // Position on screen bottom-right
            Dimension scrubSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(scrubSize.width - getWidth() - MARGIN, scrubSize.height - getHeight() - MARGIN);
        }

        setVisible(true);

        // Auto close after 4 seconds
        Timer timer = new Timer(4000, e -> {
            closeToast();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void closeToast() {
        setVisible(false);
        dispose();
    }
}
