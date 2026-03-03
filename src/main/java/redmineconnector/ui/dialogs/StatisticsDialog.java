package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import redmineconnector.model.Task;
import redmineconnector.ui.UIHelper;
import redmineconnector.util.I18n;

public class StatisticsDialog extends JDialog {
    public StatisticsDialog(Window owner, String title, List<Task> tasks) {
        super(owner, I18n.format("stats.dialog.title", title), ModalityType.MODELESS);
        setSize(850, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        UIHelper.addEscapeListener(this);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(I18n.get("stats.tab.general"), createGeneralTab(tasks));
        tabs.addTab(I18n.get("stats.tab.matrix"), createMatrixTab(tasks));
        tabs.addTab(I18n.get("stats.tab.timeline"), createTimelineTab(tasks));
        add(tabs, BorderLayout.CENTER);
    }

    public static JPanel createGeneralTab(List<Task> tasks) {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        long total = tasks.size();
        long open = tasks.stream()
                .filter(t -> !t.status.equalsIgnoreCase("Closed") && !t.status.equalsIgnoreCase("Cerrada")
                        && !t.status.equalsIgnoreCase("Rechazada") && !t.status.equalsIgnoreCase("Resolved")
                        && !t.status.equalsIgnoreCase("Resuelta"))
                .count();
        double totalHours = tasks.stream().mapToDouble(t -> t.spentHours).sum();
        double avgDone = tasks.isEmpty() ? 0 : tasks.stream().mapToInt(t -> t.doneRatio).average().orElse(0);
        long avgAge = tasks.isEmpty() ? 0
                : (long) tasks.stream()
                        .mapToLong(t -> (new Date().getTime() - t.createdOn.getTime()) / (1000 * 3600 * 24))
                        .average().orElse(0);

        JPanel header = new JPanel(new GridLayout(1, 5, 5, 0));
        header.setBorder(
                new CompoundBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), new EmptyBorder(0, 0, 10, 0)));
        header.add(createMetricCard(I18n.get("stats.card.total"), String.valueOf(total)));
        header.add(createMetricCard(I18n.get("stats.card.open"), String.valueOf(open)));
        header.add(createMetricCard(I18n.get("stats.card.hours"), String.format("%.1f", totalHours)));
        header.add(createMetricCard(I18n.get("stats.card.progress"), String.format("%.0f%%", avgDone)));
        header.add(createMetricCard(I18n.get("stats.card.age"), I18n.format("stats.unit.days", avgAge)));

        main.add(header);
        main.add(Box.createVerticalStrut(15));

        JPanel charts = new JPanel(new GridLayout(2, 2, 10, 10));
        Map<String, Long> statusCounts = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.status, Collectors.counting()));
        charts.add(createChartWrapper(I18n.get("stats.chart.status"),
                new SimpleBarChartPanel(statusCounts, new Color(100, 149, 237))));

        Map<String, Long> typeCounts = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.tracker, Collectors.counting()));
        charts.add(createChartWrapper(I18n.get("stats.chart.type"),
                new SimpleBarChartPanel(typeCounts, new Color(60, 179, 113))));

        Map<String, Long> prioCounts = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.priority, Collectors.counting()));
        charts.add(createChartWrapper(I18n.get("stats.chart.priority"),
                new SimpleBarChartPanel(prioCounts, new Color(255, 160, 122))));

        Map<String, Long> userCounts = tasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.assignedTo.isEmpty() ? I18n.get("stats.user.unassigned") : t.assignedTo,
                        Collectors.counting()));
        Map<String, Long> topUsers = userCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        charts.add(createChartWrapper(I18n.get("stats.chart.users"), new Color(147, 112, 219), topUsers));

        main.add(charts);
        return main;
    }

    private static JPanel createChartWrapper(String title, JPanel chart) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(chart, BorderLayout.CENTER);
        return p;
    }

    private static JPanel createChartWrapper(String title, Color color, Map<String, Long> data) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(new SimpleBarChartPanel(data, color), BorderLayout.CENTER);
        return p;
    }

    public static JPanel createMatrixTab(List<Task> tasks) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JLabel(I18n.get("stats.matrix.intro")), BorderLayout.NORTH);

        Set<String> statuses = tasks.stream().map(t -> t.status).filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toSet());
        Set<String> priorities = tasks.stream().map(t -> t.priority).filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toSet());
        List<String> sortedStats = new ArrayList<>(statuses);
        Collections.sort(sortedStats);
        List<String> sortedPrio = new ArrayList<>(priorities);
        Collections.sort(sortedPrio);

        DefaultTableModel tm = new DefaultTableModel();
        tm.addColumn(I18n.get("stats.matrix.col.header"));
        for (String s : sortedStats)
            tm.addColumn(s);

        for (String prio : sortedPrio) {
            Vector<Object> row = new Vector<>();
            row.add(prio);
            for (String st : sortedStats) {
                long count = tasks.stream().filter(t -> t.priority.equals(prio) && t.status.equals(st)).count();
                row.add(count);
            }
            tm.addRow(row);
        }
        JTable table = new JTable(tm);
        table.setRowHeight(30);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column > 0 && value instanceof Long) {
                    long v = (Long) value;
                    if (v == 0)
                        c.setBackground(Color.WHITE);
                    else {
                        int intensity = Math.min(255, (int) (v * 40));
                        c.setBackground(new Color(255, 255 - intensity, 255 - intensity));
                    }
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    c.setBackground(new Color(240, 240, 240));
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    public static JPanel createTimelineTab(List<Task> tasks) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JLabel(I18n.get("stats.timeline.intro")), BorderLayout.NORTH);
        Map<String, Integer> counts = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        for (Task t : tasks) {
            if (t.createdOn != null) {
                String k = sdf.format(t.createdOn);
                counts.put(k, counts.getOrDefault(k, 0) + 1);
            }
        }
        p.add(new AreaChartPanel(counts), BorderLayout.CENTER);
        return p;
    }

    private static JPanel createMetricCard(String title, String val) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new LineBorder(new Color(230, 230, 230), 1));
        JLabel lblT = new JLabel(title, SwingConstants.CENTER);
        lblT.setForeground(Color.GRAY);
        lblT.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JLabel lblV = new JLabel(val, SwingConstants.CENTER);
        lblV.setFont(new Font("Segoe UI", Font.BOLD, 15));
        p.add(lblV, BorderLayout.CENTER);
        p.add(lblT, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(80, 50));
        return p;
    }

    static class SimpleBarChartPanel extends JPanel {
        private final Map<String, Long> data;
        private final Color barColor;

        public SimpleBarChartPanel(Map<String, Long> data, Color color) {
            this.data = data;
            this.barColor = color;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty())
                return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            long max = data.values().stream().max(Long::compareTo).orElse(1L);
            int w = getWidth();
            int h = getHeight();
            int barHeight = Math.max(20, h / data.size() - 5);
            int y = 5;
            List<Map.Entry<String, Long>> sorted = data.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .collect(Collectors.toList());
            for (Map.Entry<String, Long> e : sorted) {
                if (y + barHeight > h)
                    break;
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                String lbl = e.getKey();
                if (lbl.length() > 15)
                    lbl = lbl.substring(0, 15) + "...";
                g2.drawString(lbl, 5, y + barHeight / 2 + 5);
                int barX = 110;
                int maxBarW = w - barX - 40;
                int barW = (int) ((double) e.getValue() / max * maxBarW);
                g2.setColor(barColor);
                g2.fillRoundRect(barX, y, barW, barHeight - 2, 5, 5);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.valueOf(e.getValue()), barX + barW + 5, y + barHeight / 2 + 5);
                y += barHeight;
            }
        }
    }

    static class AreaChartPanel extends JPanel {
        private final Map<String, Integer> data;

        public AreaChartPanel(Map<String, Integer> d) {
            this.data = d;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty())
                return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = 40;
            int w = getWidth() - pad * 2;
            int h = getHeight() - pad * 2;
            int maxVal = data.values().stream().max(Integer::compareTo).orElse(1);
            g2.drawLine(pad, getHeight() - pad, getWidth() - pad, getHeight() - pad);
            g2.drawLine(pad, getHeight() - pad, pad, pad);
            List<String> keys = new ArrayList<>(data.keySet());
            int xStep = w / Math.max(1, keys.size() - 1);
            Polygon p = new Polygon();
            p.addPoint(pad, getHeight() - pad);
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                int val = data.get(key);
                int x = pad + i * xStep;
                int y = getHeight() - pad - (int) ((double) val / maxVal * h);
                p.addPoint(x, y);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                if (keys.size() < 15 || i % (keys.size() / 10) == 0) {
                    g2.drawString(key, x - 15, getHeight() - pad + 15);
                }
            }
            p.addPoint(pad + (keys.size() - 1) * xStep, getHeight() - pad);
            g2.setColor(new Color(173, 216, 230, 150));
            g2.fillPolygon(p);
            g2.setColor(Color.BLUE);
            g2.drawPolyline(p.xpoints, p.ypoints, p.npoints - 2);
        }
    }
}
