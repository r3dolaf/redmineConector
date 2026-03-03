package redmineconnector.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

public class DatePickerPopup extends JPopupMenu {
    private final Calendar calendar = Calendar.getInstance();
    private final Calendar today = Calendar.getInstance(); // Store today's date
    private Calendar selectedDate = null; // Store selected date
    private final JLabel lblMonth = new JLabel("", SwingConstants.CENTER);
    private final JPanel dayGrid = new JPanel(new GridLayout(7, 7));
    private final Consumer<String> onDateSelected;
    private final SimpleDateFormat outputFormat;

    public DatePickerPopup(Consumer<String> onSelect) {
        this.onDateSelected = onSelect;

        // Use locale-appropriate date format
        Locale currentLocale = redmineconnector.util.I18n.getCurrentLocale();
        DateFormat localDateFormat = DateFormat.getDateInstance(DateFormat.SHORT, currentLocale);

        // Determine pattern based on locale
        if (localDateFormat instanceof SimpleDateFormat) {
            String pattern = ((SimpleDateFormat) localDateFormat).toPattern();
            // Normalize to always use 4-digit year and 2-digit day/month
            pattern = pattern.replaceAll("y+", "yyyy").replaceAll("M+", "MM").replaceAll("d+", "dd");
            this.outputFormat = new SimpleDateFormat(pattern, currentLocale);
        } else {
            // Fallback to dd/MM/yyyy for most European countries
            this.outputFormat = new SimpleDateFormat("dd/MM/yyyy", currentLocale);
        }

        // Initialize calendar to today
        calendar.setTime(new Date());
        today.setTime(new Date()); // Store today

        // Force Monday as first day of week (European standard)
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        setBorder(javax.swing.BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(245, 245, 245));
        JButton prev = new JButton("<");
        prev.setMargin(new Insets(2, 5, 2, 5));
        prev.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            updateGrid();
        });
        JButton next = new JButton(">");
        next.setMargin(new Insets(2, 5, 2, 5));
        next.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
            updateGrid();
        });
        header.add(prev, BorderLayout.WEST);
        header.add(lblMonth, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(dayGrid, BorderLayout.CENTER);

        // Footer with action buttons
        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 2));

        JButton btnToday = new JButton(redmineconnector.util.I18n.get("date.picker.today"));
        btnToday.setMargin(new Insets(2, 8, 2, 8));
        btnToday.addActionListener(e -> {
            selectedDate = Calendar.getInstance();
            selectedDate.setTime(new Date());
            calendar.setTime(new Date());
            updateGrid();
        });

        JButton btnAccept = new JButton(redmineconnector.util.I18n.get("date.picker.accept"));
        btnAccept.setMargin(new Insets(2, 12, 2, 12));
        btnAccept.setFont(btnAccept.getFont().deriveFont(Font.BOLD));
        btnAccept.addActionListener(e -> {
            if (selectedDate != null) {
                onDateSelected.accept(outputFormat.format(selectedDate.getTime()));
                setVisible(false);
            }
        });

        JButton btnCancel = new JButton(redmineconnector.util.I18n.get("date.picker.cancel"));
        btnCancel.setMargin(new Insets(2, 8, 2, 8));
        btnCancel.addActionListener(e -> setVisible(false));

        footer.add(btnToday);
        footer.add(btnAccept);
        footer.add(btnCancel);
        add(footer, BorderLayout.SOUTH);

        updateGrid();
    }

    private void updateGrid() {
        dayGrid.removeAll();
        lblMonth.setText(new SimpleDateFormat("MMMM yyyy", redmineconnector.util.I18n.getCurrentLocale())
                .format(calendar.getTime()).toUpperCase());

        // Force Monday as first day of week (European standard)
        Locale currentLocale = redmineconnector.util.I18n.getCurrentLocale();

        // Add day headers starting from Monday
        SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEE", currentLocale);
        Calendar headerCal = Calendar.getInstance(currentLocale);
        headerCal.setFirstDayOfWeek(Calendar.MONDAY);
        headerCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        for (int i = 0; i < 7; i++) {
            String dayName = dayNameFormat.format(headerCal.getTime());
            JLabel l = new JLabel(dayName.substring(0, Math.min(2, dayName.length())).toUpperCase(),
                    SwingConstants.CENTER);
            l.setForeground(new Color(70, 130, 180)); // Steel blue
            l.setFont(l.getFont().deriveFont(Font.BOLD, 10f));
            dayGrid.add(l);
            headerCal.add(Calendar.DAY_OF_WEEK, 1);
        }

        Calendar iter = (Calendar) calendar.clone();
        iter.setFirstDayOfWeek(Calendar.MONDAY);
        iter.set(Calendar.DAY_OF_MONTH, 1);

        // Calculate offset based on Monday as first day
        int startDay = iter.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
        if (startDay < 0)
            startDay += 7;

        iter.add(Calendar.DAY_OF_MONTH, -startDay);

        for (int i = 0; i < 42; i++) {
            JButton btn = new JButton(String.valueOf(iter.get(Calendar.DAY_OF_MONTH)));
            btn.setMargin(new Insets(2, 2, 2, 2));
            btn.setFocusPainted(false);
            btn.setFont(btn.getFont().deriveFont(11f));

            // Check if this is today's date
            boolean isToday = iter.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    iter.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    iter.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

            // Check if this is the selected date
            boolean isSelected = selectedDate != null &&
                    iter.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    iter.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    iter.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH);

            if (iter.get(Calendar.MONTH) != calendar.get(Calendar.MONTH)) {
                btn.setForeground(Color.GRAY);
                btn.setEnabled(true); // Still clickable but grayed out
            } else if (isSelected) {
                // Highlight SELECTED date with GREEN styling
                btn.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createLineBorder(new Color(34, 139, 34), 2), // Forest green border
                        javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1)));
                btn.setFont(btn.getFont().deriveFont(Font.BOLD));
                btn.setForeground(new Color(0, 100, 0)); // Dark green text
                btn.setBackground(new Color(240, 255, 240)); // Honeydew background
                btn.setOpaque(true);
            } else if (isToday) {
                // Highlight TODAY with BLUE styling
                btn.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createLineBorder(new Color(65, 105, 225), 2), // Royal blue border
                        javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1)));
                btn.setFont(btn.getFont().deriveFont(Font.BOLD));
                btn.setForeground(new Color(0, 0, 139)); // Dark blue text
                btn.setBackground(new Color(240, 248, 255)); // Alice blue background
                btn.setOpaque(true);
            }

            Date d = iter.getTime();
            btn.addActionListener(e -> {
                // Store selected date and refresh calendar to show green border
                selectedDate = Calendar.getInstance();
                selectedDate.setTime(d);
                updateGrid(); // Refresh to show selection
            });
            dayGrid.add(btn);
            iter.add(Calendar.DAY_OF_MONTH, 1);
        }
        revalidate();
        repaint();
    }
}
