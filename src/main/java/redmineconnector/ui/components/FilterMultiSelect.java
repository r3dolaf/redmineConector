package redmineconnector.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import redmineconnector.model.SimpleEntity;

public class FilterMultiSelect extends JButton {
    private final String title;
    private JDialog popupDialog;
    private final Set<String> selectedNames = new HashSet<>();
    private final Runnable onChange;
    private List<SimpleEntity> allItems;
    private JPanel listPanel;

    public FilterMultiSelect(String title, Runnable onChange) {
        this.title = title;
        this.onChange = onChange;
        setText(title + ": (Todos)");
        setHorizontalAlignment(SwingConstants.LEFT);
        setMargin(new Insets(2, 5, 2, 5));
        addActionListener(e -> showPopup());
    }

    private long lastCloseTime = 0;

    private void showPopup() {
        if (System.currentTimeMillis() - lastCloseTime < 200) {
            return;
        }
        if (popupDialog != null && popupDialog.isVisible()) {
            popupDialog.dispose();
            return;
        }
        if (allItems == null || allItems.isEmpty())
            return;

        Window ancestor = SwingUtilities.getWindowAncestor(this);
        popupDialog = new JDialog(ancestor != null ? ancestor : (Frame) null);
        popupDialog.setUndecorated(true);
        popupDialog.setModal(false);
        popupDialog.setType(Window.Type.POPUP);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // --- Top Panel (Clear + Search) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.setBackground(Color.WHITE);

        JMenuItem itemAll = new JMenuItem("Limpiar Filtros");
        itemAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemAll.setForeground(Color.RED);
        itemAll.setOpaque(true);
        itemAll.setBackground(Color.WHITE);
        itemAll.addActionListener(e -> {
            clearSelection();
            popupDialog.dispose();
        });
        topPanel.add(itemAll, BorderLayout.NORTH);

        JTextField txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(100, 26));
        txtSearch.putClientProperty("JTextField.placeholderText", "Buscar...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                refreshList(txtSearch.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                refreshList(txtSearch.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                refreshList(txtSearch.getText());
            }
        });
        topPanel.add(txtSearch, BorderLayout.SOUTH);
        content.add(topPanel, BorderLayout.NORTH);

        // --- List Panel ---
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        refreshList(""); // Build initial list

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        int rowHeight = 25;
        int totalHeight = allItems.size() * rowHeight;
        int maxHeight = 300;
        scroll.setPreferredSize(new Dimension(250, Math.min(totalHeight + 10, maxHeight)));
        content.add(scroll, BorderLayout.CENTER);

        popupDialog.setContentPane(content);
        popupDialog.pack();

        // Positioning
        Point p = getLocationOnScreen();
        popupDialog.setLocation(p.x, p.y + getHeight());

        // Auto-close on focus loss
        popupDialog.addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                lastCloseTime = System.currentTimeMillis();
                popupDialog.dispose();
            }
        });

        popupDialog.setVisible(true);
        txtSearch.requestFocusInWindow();
    }

    public void clearSelection() {
        selectedNames.clear();
        updateLabel();
        onChange.run();
    }

    public void setItems(List<SimpleEntity> items) {
        this.allItems = items;
    }

    private void refreshList(String query) {
        listPanel.removeAll();
        if (allItems == null)
            return;

        String q = query.toLowerCase().trim();
        for (SimpleEntity se : allItems) {
            if (!q.isEmpty() && !se.name.toLowerCase().contains(q)) {
                continue;
            }

            JCheckBox item = new JCheckBox(se.name);
            item.setBackground(Color.WHITE);
            item.setFocusPainted(false);
            if (selectedNames.contains(se.name)) {
                item.setSelected(true);
            }
            item.addActionListener(e -> {
                if (item.isSelected())
                    selectedNames.add(se.name);
                else
                    selectedNames.remove(se.name);
                updateLabel();
                onChange.run();
            });
            listPanel.add(item);
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    public void updateLabel() {
        if (selectedNames.isEmpty()) {
            setText(title + ": (Todos)");
            setBackground(null);
        } else {
            setText(title + ": (" + selectedNames.size() + ")");
            setBackground(new Color(220, 240, 255));
        }
    }

    public Set<String> getSelectedNames() {
        return selectedNames;
    }
}
