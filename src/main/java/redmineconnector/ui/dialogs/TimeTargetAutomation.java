package redmineconnector.ui.dialogs;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import redmineconnector.config.ConfigManager;

public class TimeTargetAutomation {

    private static final Map<DayOfWeek, Double> weekdayRules = new HashMap<>();
    private static final TreeMap<LocalDate, RangeRule> rangeRules = new TreeMap<>();

    // Priority enum removal - logic is now implicit per range

    private static class RangeRule {
        LocalDate end;
        double target;
        boolean isPriority; // New field

        public RangeRule(LocalDate end, double target, boolean isPriority) {
            this.end = end;
            this.target = target;
            this.isPriority = isPriority;
        }
    }

    static {
        loadConfig();
    }

    public static void loadConfig() {
        Properties props = ConfigManager.loadConfig();
        weekdayRules.clear();
        rangeRules.clear();

        // Load Weekday Rules
        String wd = props.getProperty("reports.automation.weekdays", "");
        if (!wd.isEmpty()) {
            for (String part : wd.split(";")) {
                String[] split = part.split("=");
                if (split.length == 2) {
                    try {
                        weekdayRules.put(DayOfWeek.valueOf(split[0]), Double.valueOf(split[1]));
                    } catch (Exception e) {
                    }
                }
            }
        }

        // Load Ranges (e.g., "2024-08-01|2024-08-31|7.0|true;...")
        String ranges = props.getProperty("reports.automation.ranges", "");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (!ranges.isEmpty()) {
            for (String part : ranges.split(";")) {
                String[] split = part.split("\\|");
                if (split.length >= 3) {
                    try {
                        LocalDate start = LocalDate.parse(split[0], dtf);
                        LocalDate end = LocalDate.parse(split[1], dtf);
                        double target = Double.parseDouble(split[2]);
                        boolean isPriority = false;
                        if (split.length > 3)
                            isPriority = Boolean.parseBoolean(split[3]);

                        rangeRules.put(start, new RangeRule(end, target, isPriority));
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public static void saveConfig() {
        Properties props = ConfigManager.loadConfig();

        // Clean up old priority property if exists
        props.remove("reports.automation.priority");

        // Save Weekdays
        StringBuilder sbWd = new StringBuilder();
        weekdayRules.forEach((day, val) -> {
            if (sbWd.length() > 0)
                sbWd.append(";");
            sbWd.append(day.name()).append("=").append(val);
        });
        props.setProperty("reports.automation.weekdays", sbWd.toString());

        // Save Ranges
        StringBuilder sbRange = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        rangeRules.forEach((start, rule) -> {
            if (sbRange.length() > 0)
                sbRange.append(";");
            sbRange.append(start.format(dtf)).append("|")
                    .append(rule.end.format(dtf)).append("|")
                    .append(rule.target).append("|")
                    .append(rule.isPriority);
        });
        props.setProperty("reports.automation.ranges", sbRange.toString());

        ConfigManager.saveConfig(props);
    }

    public static void setWeekdayRule(DayOfWeek day, Double target) {
        if (target == null)
            weekdayRules.remove(day);
        else
            weekdayRules.put(day, target);
    }

    public static Double getWeekdayRule(DayOfWeek day) {
        return weekdayRules.get(day);
    }

    public static void addRangeRule(LocalDate start, LocalDate end, double target, boolean isPriority) {
        rangeRules.put(start, new RangeRule(end, target, isPriority));
    }

    public static void clearRanges() {
        rangeRules.clear();
    }

    public static Map<String, String[]> getRangesDetails() {
        Map<String, String[]> map = new TreeMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        rangeRules.forEach((start, rule) -> {
            // Key: Range String, Value: [Target, PriorityStr]
            map.put(start.format(dtf) + " -> " + rule.end.format(dtf),
                    new String[] { String.valueOf(rule.target), String.valueOf(rule.isPriority) });
        });
        return map;
    }

    public static double getTargetForDate(LocalDate date) {
        // 1. High Priority Ranges (Override Weekdays)
        Double hC = matchRange(date, true);
        if (hC != null)
            return hC;

        // 2. Weekday Rules
        Double w = matchWeekday(date);
        if (w != null)
            return w;

        // 3. Normal Ranges (Apply if no weekday rule)
        Double nC = matchRange(date, false);
        if (nC != null)
            return nC;

        // 4. Default
        return 8.5;
    }

    private static Double matchRange(LocalDate date, boolean checkPriority) {
        for (Map.Entry<LocalDate, RangeRule> entry : rangeRules.entrySet()) {
            if (entry.getValue().isPriority == checkPriority) {
                if (!date.isBefore(entry.getKey()) && !date.isAfter(entry.getValue().end)) {
                    return entry.getValue().target;
                }
            }
        }
        return null;
    }

    private static Double matchWeekday(LocalDate date) {
        if (weekdayRules.containsKey(date.getDayOfWeek())) {
            return weekdayRules.get(date.getDayOfWeek());
        }
        return null;
    }
}
