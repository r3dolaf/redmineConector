package redmineconnector.ui.dialogs;

import redmineconnector.model.Task;
import redmineconnector.ui.InstanceController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;

public class GlobalSearchDialog extends JDialog {
    private final List<InstanceController> controllers;
    private final Consumer<SearchResult> onResultSelected;
    private JTextField txtSearch;
    private DefaultListModel<SearchResult> listModel;
    private JList<SearchResult> resultList;

    public GlobalSearchDialog(Frame owner, List<InstanceController> controllers,
            Consumer<SearchResult> onResultSelected) {
        super(owner, true);
        this.controllers = controllers;
        this.onResultSelected = onResultSelected;

        setUndecorated(true);
        setLayout(new BorderLayout());
        setBackground(new Color(255, 255, 255, 0)); // Transparent background for rounded feel if desired

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        mainPanel.setBackground(Color.WHITE);

        txtSearch = new JTextField();
        txtSearch.setFont(new Font("SansSerif", Font.PLAIN, 18));
        txtSearch.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.add(txtSearch, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setCellRenderer(new SearchResultRenderer());
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(owner);

        initListeners();
    }

    private void initListeners() {
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    resultList.setSelectedIndex(0);
                    resultList.requestFocus();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (listModel.size() > 0) {
                        selectResult(listModel.getElementAt(0));
                    }
                } else {
                    performSearch(txtSearch.getText());
                }
            }
        });

        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    selectResult(resultList.getSelectedValue());
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        });

        resultList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    selectResult(resultList.getSelectedValue());
                }
            }
        });

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                dispose();
            }
        });
    }

    private void performSearch(String text) {
        listModel.clear();
        if (text == null || text.trim().length() < 2)
            return;

        String query = text.toLowerCase();
        for (InstanceController controller : controllers) {
            for (Task task : controller.getCurrentTasks()) {
                if (String.valueOf(task.id).contains(query) ||
                        (task.subject != null && task.subject.toLowerCase().contains(query))) {
                    listModel.addElement(new SearchResult(controller, task));
                }
                if (listModel.size() > 50)
                    break; // Limit results
            }
            if (listModel.size() > 50)
                break;
        }
    }

    private void selectResult(SearchResult result) {
        if (result != null) {
            onResultSelected.accept(result);
            dispose();
        }
    }

    public void showSearch() {
        txtSearch.setText("");
        listModel.clear();
        setVisible(true);
        txtSearch.requestFocus();
    }

    public static class SearchResult {
        public final InstanceController controller;
        public final Task task;

        public SearchResult(InstanceController controller, Task task) {
            this.controller = controller;
            this.task = task;
        }
    }

    private class SearchResultRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            SearchResult res = (SearchResult) value;
            JPanel p = new JPanel(new BorderLayout(10, 5));
            p.setBorder(new EmptyBorder(8, 10, 8, 10));
            p.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

            JLabel lblId = new JLabel("#" + res.task.id);
            lblId.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblId.setForeground(new Color(100, 100, 100));

            JLabel lblSubject = new JLabel(res.task.subject);
            lblSubject.setFont(new Font("SansSerif", Font.PLAIN, 14));
            lblSubject.setForeground(isSelected ? list.getSelectionForeground() : Color.BLACK);

            JLabel lblClient = new JLabel(res.controller.getTitle());
            lblClient.setFont(new Font("SansSerif", Font.ITALIC, 11));
            lblClient.setForeground(new Color(150, 150, 150));

            JPanel left = new JPanel(new BorderLayout(5, 0));
            left.setOpaque(false);
            left.add(lblId, BorderLayout.WEST);
            left.add(lblSubject, BorderLayout.CENTER);

            p.add(left, BorderLayout.CENTER);
            p.add(lblClient, BorderLayout.EAST);

            return p;
        }
    }
}
