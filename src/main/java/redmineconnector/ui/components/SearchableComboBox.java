package redmineconnector.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import redmineconnector.model.SimpleEntity;

import java.util.List;
import java.util.ArrayList;
import javax.swing.ButtonGroup;

/**
 * Componente de lista desplegable con búsqueda para la selección única de
 * elementos SimpleEntity.
 * Similar a FilterMultiSelect pero para selección única (como un ComboBox con
 * búsqueda).
 */
public class SearchableComboBox extends JButton {
    private final String title;
    private JDialog popupDialog;
    private SimpleEntity selectedItem;
    private Runnable onChange;
    private List<SimpleEntity> allItems = new ArrayList<>();
    private JPanel listPanel;

    public SearchableComboBox(String title) {
        this.title = title;
        setText(title + ": (Ninguna)");
        setHorizontalAlignment(SwingConstants.LEFT);
        setMargin(new Insets(2, 5, 2, 5));
        addActionListener(e -> showPopup());
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
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

        JMenuItem itemClear = new JMenuItem("Limpiar Selección");
        itemClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemClear.setForeground(Color.RED);
        itemClear.setOpaque(true);
        itemClear.setBackground(Color.WHITE);
        itemClear.addActionListener(e -> {
            clearSelection();
            popupDialog.dispose();
        });
        topPanel.add(itemClear, BorderLayout.NORTH);

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
        int popupWidth = Math.max(300, getWidth());
        scroll.setPreferredSize(new Dimension(popupWidth, Math.min(totalHeight + 10, maxHeight)));
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
        selectedItem = null;
        updateLabel();
        if (onChange != null) {
            onChange.run();
        }
    }

    public void setItems(List<SimpleEntity> items) {
        this.allItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    private void refreshList(String query) {
        listPanel.removeAll();
        if (allItems == null)
            return;

        ButtonGroup group = new ButtonGroup();
        String q = query.toLowerCase().trim();

        for (SimpleEntity se : allItems) {
            if (!q.isEmpty() && !se.name.toLowerCase().contains(q)) {
                continue;
            }

            JRadioButton item = new JRadioButton(se.name);
            item.setBackground(Color.WHITE);
            item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            item.setSelected(selectedItem != null && selectedItem.id == se.id);

            group.add(item);

            item.addActionListener(e -> {
                selectedItem = se;
                updateLabel();
                if (onChange != null) {
                    onChange.run();
                }
                popupDialog.dispose();
            });

            listPanel.add(item);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private int maxWidthChars = 20;

    public void setMaxWidthChars(int chars) {
        this.maxWidthChars = chars;
        updateLabel();
    }

    private void updateLabel() {
        String text;
        if (selectedItem == null) {
            text = title + ": (Ninguna)";
        } else {
            text = title + ": " + selectedItem.name;
        }

        // Truncate to prevent dialog width explosion
        if (text.length() > maxWidthChars) {
            text = text.substring(0, maxWidthChars - 3) + "...";
        }
        setText(text);
        setToolTipText(selectedItem != null ? selectedItem.name : null);
    }

    public SimpleEntity getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(SimpleEntity item) {
        this.selectedItem = item;
        updateLabel();
    }

    public void setSelectedById(int id) {
        if (allItems != null) {
            for (SimpleEntity se : allItems) {
                if (se.id == id) {
                    setSelectedItem(se);
                    return;
                }
            }
        }
        clearSelection();
    }
}
