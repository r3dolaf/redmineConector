package redmineconnector.ui.components;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import redmineconnector.config.StyleConfig;

public class TaskTablePanel extends JPanel {
    private JTable table;
    private TaskTableModel model;
    private TableRowSorter<TaskTableModel> sorter;
    private JScrollPane scrollPane;
    private EnhancedRenderer renderer;
    // private JLabel lblStats; // Removed unused

    private final StyleConfig styleConfig;

    public TaskTablePanel(StyleConfig styleConfig) {
        super(new BorderLayout());
        this.styleConfig = styleConfig;

        createUI();
        applyStyles();
    }

    public void applyStyles() {
        if (table != null) {
            table.setBackground(styleConfig.bgPanel);
            table.setForeground(styleConfig.textPrimary);
            table.setSelectionBackground(styleConfig.bgSelection);
            table.setSelectionForeground(styleConfig.textPrimary); // Or textInverted if needed
            table.setGridColor(styleConfig.border);

            if (table.getTableHeader() != null) {
                table.getTableHeader().setBackground(styleConfig.bgHeader);
                table.getTableHeader().setForeground(styleConfig.textPrimary);
            }
        }

        if (scrollPane != null) {
            scrollPane.getViewport().setBackground(styleConfig.bgPanel);
        }

        // Renderer picks up defaults from styleConfig, but we might want to trigger a
        // full repaint
        // No explicit update needed for renderer itself as it reads styleConfig each
        // paint,
        // OR we can add a refresh method to renderer if it caches colors.
        // EnhancedRenderer reads from 'styles' field. Since 'styles' is a reference
        // object,
        // updates within StyleConfig are visible immediately.

        repaint();
    }

    private void createUI() {
        model = new TaskTableModel();
        table = new ThemeTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Setup table properties
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Use defaults from ThemeTable, but ensure specific overrides
        table.setRowHeight(24);

        renderer = new EnhancedRenderer(styleConfig);
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Integer.class, renderer);
        table.setDefaultRenderer(String.class, renderer);
        table.setDefaultRenderer(Double.class, renderer);
        table.setDefaultRenderer(java.util.Date.class, renderer);
        table.setDefaultRenderer(Boolean.class, renderer);

        // Add mouse motion for hover effects
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != renderer.getMouseRow()) {
                    renderer.setMouseRow(row);
                    table.repaint();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                renderer.setMouseRow(-1);
                table.repaint();
            }
        });

        // Column Config
        TableColumnModel cm = table.getColumnModel();
        if (cm.getColumnCount() > 0) {
            cm.getColumn(0).setPreferredWidth(50);
            cm.getColumn(0).setMaxWidth(70);
            if (cm.getColumnCount() > 1)
                cm.getColumn(1).setPreferredWidth(300);
            if (cm.getColumnCount() > 6)
                cm.getColumn(6).setPreferredWidth(100);
            if (cm.getColumnCount() > 9)
                cm.getColumn(9).setPreferredWidth(60);
        }

        // Default Sort - ID descending
        List<javax.swing.RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new javax.swing.RowSorter.SortKey(0, javax.swing.SortOrder.DESCENDING)); // ID
        sorter.setSortKeys(sortKeys);

        scrollPane = new JScrollPane(table);
        // Ensure viewport matches table background (important for short lists in dark
        // mode)
        scrollPane.getViewport().setBackground(table.getBackground());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(scrollPane, BorderLayout.CENTER);
    }

    // Getters
    public JTable getTable() {
        return table;
    }

    public TaskTableModel getModel() {
        return model;
    }

    public TableRowSorter<TaskTableModel> getSorter() {
        return sorter;
    }

    public EnhancedRenderer getRenderer() {
        return renderer;
    }
}
