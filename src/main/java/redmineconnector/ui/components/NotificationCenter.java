package redmineconnector.ui.components;

import redmineconnector.model.Notification;
import redmineconnector.notifications.NotificationManager;
import redmineconnector.util.I18n;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Panel displaying notification history with filtering and actions.
 */
public class NotificationCenter extends JPanel {

    private final DefaultListModel<Notification> listModel = new DefaultListModel<>();
    private final JList<Notification> notificationList = new JList<>(listModel);
    private final JLabel lblUnreadCount = new JLabel();
    private final Runnable onNotificationClick;

    public NotificationCenter(Runnable onNotificationClick) {
        super(new BorderLayout());
        this.onNotificationClick = onNotificationClick;
        initUI();
        loadNotifications();

        // Listen for new notifications
        NotificationManager.addNotificationListener(n -> SwingUtilities.invokeLater(this::loadNotifications));
    }

    private void initUI() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(400, 500));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("🔔 Centro de Notificaciones");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblUnreadCount, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Notification list
        notificationList.setCellRenderer(new NotificationRenderer());
        notificationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notificationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Notification selected = notificationList.getSelectedValue();
                if (selected != null && !selected.read) {
                    NotificationManager.markAsRead(selected.id);
                    loadNotifications();
                }
            }
        });

        notificationList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && onNotificationClick != null) {
                    onNotificationClick.run();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(notificationList);
        add(scroll, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnMarkAllRead = new JButton("Marcar todas leídas");
        btnMarkAllRead.addActionListener(e -> {
            NotificationManager.markAllAsRead();
            loadNotifications();
        });

        JButton btnClearAll = new JButton("Limpiar todo");
        btnClearAll.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar todas las notificaciones?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                NotificationManager.clearAll();
                loadNotifications();
            }
        });

        actions.add(btnMarkAllRead);
        actions.add(btnClearAll);
        add(actions, BorderLayout.SOUTH);
    }

    private void loadNotifications() {
        listModel.clear();
        List<Notification> notifications = NotificationManager.getAllNotifications();
        for (Notification n : notifications) {
            listModel.addElement(n);
        }

        int unread = NotificationManager.getUnreadCount();
        lblUnreadCount.setText(unread > 0 ? unread + " no leídas" : "");
    }

    /**
     * Custom renderer for notifications.
     */
    private static class NotificationRenderer extends JPanel implements ListCellRenderer<Notification> {
        private final JLabel lblIcon = new JLabel();
        private final JLabel lblMessage = new JLabel();
        private final JLabel lblTime = new JLabel();
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");

        public NotificationRenderer() {
            setLayout(new BorderLayout(5, 5));
            setBorder(new EmptyBorder(5, 5, 5, 5));

            lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblTime.setForeground(Color.GRAY);

            add(lblIcon, BorderLayout.WEST);

            JPanel center = new JPanel(new BorderLayout());
            center.add(lblMessage, BorderLayout.CENTER);
            center.add(lblTime, BorderLayout.SOUTH);
            add(center, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Notification> list,
                Notification value, int index, boolean isSelected, boolean cellHasFocus) {

            lblIcon.setText(value.type.icon);
            lblMessage.setText(String.format("<html><b>%s:</b> %s (#%d)</html>",
                    value.type.label, value.message, value.taskId));
            lblTime.setText(sdf.format(value.timestamp));

            if (!value.read) {
                setBackground(new Color(230, 240, 255));
                lblMessage.setFont(lblMessage.getFont().deriveFont(Font.BOLD));
            } else {
                setBackground(Color.WHITE);
                lblMessage.setFont(lblMessage.getFont().deriveFont(Font.PLAIN));
            }

            if (isSelected) {
                setBackground(new Color(200, 220, 255));
            }

            return this;
        }
    }
}
