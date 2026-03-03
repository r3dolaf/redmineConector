package redmineconnector.ui;

import redmineconnector.metrics.MetricsCalculator;
import redmineconnector.model.Task;
import redmineconnector.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Dashboard panel displaying project metrics and statistics with visual charts.
 */
public class MetricsDashboard extends JPanel {

        private final JLabel lblTotalTasks;
        private final JLabel lblCompletedTasks;
        private final JLabel lblPendingTasks;
        private final JLabel lblTotalHours;
        private final JLabel lblAvgCompletion;
        private final JPanel pnlStatusBars = new JPanel();
        private final JPanel pnlPriorityBars = new JPanel();
        private final JTextArea txtUserBreakdown;
        private final JButton btnCopy;
        private java.util.List<Task> currentTasks;

        public MetricsDashboard() {
                setLayout(new BorderLayout(15, 15));
                setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                // Title
                JLabel title = new JLabel(redmineconnector.util.I18n.get("metrics.dashboard.title"),
                                SwingConstants.CENTER);
                title.setFont(new Font("Segoe UI", Font.BOLD, 24));
                add(title, BorderLayout.NORTH);

                // Main content
                JPanel mainPanel = new JPanel(new BorderLayout(15, 15));

                // Top section - Summary cards
                JPanel summaryPanel = new JPanel(new GridLayout(1, 5, 15, 0));
                summaryPanel.setOpaque(false);

                lblTotalTasks = createMetricLabel(redmineconnector.util.I18n.get("metrics.total.tasks"), "0");
                lblCompletedTasks = createMetricLabel(redmineconnector.util.I18n.get("metrics.completed"), "0");
                lblPendingTasks = createMetricLabel(redmineconnector.util.I18n.get("metrics.pending"), "0");
                lblTotalHours = createMetricLabel(redmineconnector.util.I18n.get("metrics.hours.total"), "0.0");
                lblAvgCompletion = createMetricLabel(redmineconnector.util.I18n.get("metrics.avg.completion"), "0%");

                summaryPanel.add(createMetricCard("📋", lblTotalTasks));
                summaryPanel.add(createMetricCard("✅", lblCompletedTasks));
                summaryPanel.add(createMetricCard("⏳", lblPendingTasks));
                summaryPanel.add(createMetricCard("⏱️", lblTotalHours));
                summaryPanel.add(createMetricCard("📈", lblAvgCompletion));

                mainPanel.add(summaryPanel, BorderLayout.NORTH);

                // Center section - Visual Breakdowns
                JPanel breakdownPanel = new JPanel(new GridLayout(1, 3, 15, 15));
                breakdownPanel.setOpaque(false);

                pnlStatusBars.setLayout(new BoxLayout(pnlStatusBars, BoxLayout.Y_AXIS));
                pnlPriorityBars.setLayout(new BoxLayout(pnlPriorityBars, BoxLayout.Y_AXIS));
                txtUserBreakdown = new JTextArea();
                txtUserBreakdown.setEditable(false);

                breakdownPanel.add(createChartContainer(redmineconnector.util.I18n.get("metrics.breakdown.status"),
                                pnlStatusBars));
                breakdownPanel.add(createChartContainer(redmineconnector.util.I18n.get("metrics.breakdown.priority"),
                                pnlPriorityBars));
                breakdownPanel.add(createScrollBreakdown(redmineconnector.util.I18n.get("metrics.breakdown.user"),
                                txtUserBreakdown));

                mainPanel.add(breakdownPanel, BorderLayout.CENTER);
                add(mainPanel, BorderLayout.CENTER);

                // Actions
                btnCopy = new JButton(
                                "📋 " + redmineconnector.util.I18n.get("metrics.btn.copy_summary", "Copiar Resumen"));
                btnCopy.setFont(new Font("Segoe UI", Font.BOLD, 14));
                btnCopy.setFocusPainted(false);
                btnCopy.addActionListener(e -> copySummaryToClipboard());

                JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                southPanel.add(btnCopy);
                add(southPanel, BorderLayout.SOUTH);

                ThemeManager.applyTheme(this);
        }

        private JLabel createMetricLabel(String title, String value) {
                return new JLabel("<html><div style='text-align:center; color:#555;'><small>" + title +
                                "</small><br><b style='font-size:22px; color:#333;'>" + value + "</b></div></html>",
                                SwingConstants.CENTER);
        }

        private JPanel createMetricCard(String icon, JLabel label) {
                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true),
                                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

                JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
                iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
                card.add(iconLabel, BorderLayout.NORTH);
                card.add(label, BorderLayout.CENTER);
                return card;
        }

        private JPanel createChartContainer(String title, JPanel content) {
                JPanel p = new JPanel(new BorderLayout());
                p.setBorder(ThemeManager.createThemedTitledBorder(title));
                p.setBackground(Color.WHITE);
                JScrollPane sp = new JScrollPane(content);
                sp.setBorder(null);
                p.add(sp, BorderLayout.CENTER);
                return p;
        }

        private JPanel createScrollBreakdown(String title, JTextArea txt) {
                JPanel p = new JPanel(new BorderLayout());
                p.setBorder(ThemeManager.createThemedTitledBorder(title));
                p.setBackground(Color.WHITE);
                txt.setFont(new Font("Monospaced", Font.PLAIN, 12));
                p.add(new JScrollPane(txt), BorderLayout.CENTER);
                return p;
        }

        public void updateMetrics(List<Task> tasks) {
                this.currentTasks = tasks;
                if (tasks == null || tasks.isEmpty()) {
                        clearMetrics();
                        return;
                }

                lblTotalTasks.setText(formatMetric(redmineconnector.util.I18n.get("metrics.total.tasks"),
                                String.valueOf(tasks.size())));
                Map<String, Integer> comp = MetricsCalculator.getCompletionStats(tasks);
                lblCompletedTasks.setText(formatMetric(redmineconnector.util.I18n.get("metrics.completed"),
                                String.valueOf(comp.getOrDefault("Completadas", 0))));
                lblPendingTasks.setText(formatMetric(redmineconnector.util.I18n.get("metrics.pending"),
                                String.valueOf(comp.getOrDefault("Pendientes", 0))));
                lblTotalHours.setText(formatMetric(redmineconnector.util.I18n.get("metrics.hours.total"),
                                String.format("%.1f", MetricsCalculator.getTotalHoursSpent(tasks))));
                lblAvgCompletion.setText(formatMetric(redmineconnector.util.I18n.get("metrics.avg.completion"),
                                String.format("%.0f%%", MetricsCalculator.getAverageCompletion(tasks))));

                updateVisualBars(pnlStatusBars, MetricsCalculator.countByStatus(tasks), tasks.size(), true);
                updateVisualBars(pnlPriorityBars, MetricsCalculator.countByPriority(tasks), tasks.size(), false);

                StringBuilder sb = new StringBuilder();
                MetricsCalculator.countByAssignedUser(tasks).entrySet().stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .forEach(e -> sb.append(String.format("%-20s: %d\n", e.getKey(), e.getValue())));
                txtUserBreakdown.setText(sb.toString());
        }

        private String formatMetric(String title, String value) {
                return "<html><div style='text-align:center;'><small>" + title
                                + "</small><br><b style='font-size:20px;'>" + value + "</b></div></html>";
        }

        private void updateVisualBars(JPanel target, Map<String, Integer> data, int total, boolean isStatus) {
                target.removeAll();
                data.entrySet().stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .forEach(e -> {
                                        double pct = (double) e.getValue() / total;
                                        target.add(new BarComponent(e.getKey(), e.getValue(), pct, isStatus));
                                        target.add(Box.createVerticalStrut(8));
                                });
                target.revalidate();
                target.repaint();
        }

        private void clearMetrics() {
                pnlStatusBars.removeAll();
                pnlPriorityBars.removeAll();
                txtUserBreakdown.setText("");
                pnlStatusBars.revalidate();
                pnlPriorityBars.revalidate();
        }

        private void copySummaryToClipboard() {
                if (currentTasks == null)
                        return;
                StringBuilder sb = new StringBuilder();
                sb.append("Resumen: ").append(currentTasks.size()).append(" tareas.\n");
                sb.append("Progreso: ")
                                .append(String.format("%.0f%%", MetricsCalculator.getAverageCompletion(currentTasks)))
                                .append("\n");
                UIHelper.copyToClipboard(sb.toString());
        }

        private class BarComponent extends JComponent {
                private final String label;
                private final int count;
                private final double percent;
                private final boolean isStatus;

                public BarComponent(String label, int count, double percent, boolean isStatus) {
                        this.label = label;
                        this.count = count;
                        this.percent = percent;
                        this.isStatus = isStatus;
                        setPreferredSize(new Dimension(100, 35));
                        setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
                }

                @Override
                protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int w = getWidth() - 40;
                        int barW = (int) (w * percent);

                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawString(label + " (" + count + ")", 5, 12);

                        g2.setColor(new Color(240, 240, 240));
                        g2.fillRoundRect(5, 18, w, 10, 5, 5);

                        Color c = isStatus ? getStatusColor(label) : getPriorityColor(label);
                        g2.setColor(c);
                        g2.fillRoundRect(5, 18, barW, 10, 5, 5);

                        g2.setColor(Color.GRAY);
                        g2.drawString(String.format("%.0f%%", percent * 100), w + 10, 27);
                }

                private Color getStatusColor(String s) {
                        s = s.toLowerCase();
                        if (s.contains("nueva"))
                                return new Color(52, 152, 219);
                        if (s.contains("cerrada") || s.contains("resuelta"))
                                return new Color(46, 204, 113);
                        if (s.contains("proceso"))
                                return new Color(241, 196, 15);
                        return new Color(149, 165, 166);
                }

                private Color getPriorityColor(String p) {
                        p = p.toLowerCase();
                        if (p.contains("urgente") || p.contains("alta"))
                                return new Color(231, 76, 60);
                        if (p.contains("normal"))
                                return new Color(46, 204, 113);
                        return new Color(52, 152, 219);
                }
        }
}
