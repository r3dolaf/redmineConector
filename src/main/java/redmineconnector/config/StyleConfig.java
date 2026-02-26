package redmineconnector.config;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class StyleConfig {
    private final Map<String, Color> statusColors = new HashMap<>();

    // --- Semantic Colors (Theme Aware) ---
    public Color bgMain;
    public Color bgPanel;
    public Color bgInput;
    public Color bgHeader;
    public Color bgSelection;

    public Color textPrimary;
    public Color textSecondary;
    public Color textInverted;

    public Color border;
    public Color separator;

    public Color actionPrimary;
    public Color actionSuccess;
    public Color actionDanger;
    public Color actionWarning;

    // --- Fonts ---
    // (Could be expanded later, for now we rely on logical adjustments)

    private boolean isDark = false;

    public StyleConfig() {
        // Default to Light Theme
        setTheme(false);
    }

    public void setTheme(boolean dark) {
        this.isDark = dark;
        statusColors.clear();
        loadDefaults(); // Reload status defaults (could be theme-dependent too)

        if (dark) {
            // Dark Theme Palette
            bgMain = new Color(40, 44, 52); // Dark grey
            bgPanel = new Color(33, 37, 43); // Darker grey
            bgInput = new Color(45, 49, 58); // Input bg
            bgHeader = new Color(30, 33, 38); // Header bg
            bgSelection = new Color(64, 72, 89); // Selection blue-grey

            textPrimary = new Color(220, 223, 228); // White-ish
            textSecondary = new Color(157, 165, 180); // Grey text
            textInverted = new Color(33, 37, 43); // Dark text for light chips

            border = new Color(24, 26, 31); // Dark border
            separator = new Color(55, 60, 70);

            actionPrimary = new Color(97, 175, 239); // Blue
            actionSuccess = new Color(152, 195, 121); // Green
            actionDanger = new Color(224, 108, 117); // Red
            actionWarning = new Color(229, 192, 123); // Yellow

        } else {
            // Light Theme Palette (Matching existing "System" feel but standardized)
            bgMain = new Color(245, 247, 250); // Light blue-grey
            bgPanel = Color.WHITE;
            bgInput = Color.WHITE;
            bgHeader = new Color(240, 242, 245); // Light grey header
            bgSelection = new Color(184, 207, 229); // System blue-ish

            textPrimary = new Color(33, 37, 41); // Dark grey
            textSecondary = new Color(108, 117, 125); // Medium grey
            textInverted = Color.WHITE;

            border = new Color(220, 224, 228); // Light border
            separator = new Color(230, 230, 230);

            actionPrimary = new Color(13, 110, 253); // Bootstrap Blue
            actionSuccess = new Color(25, 135, 84); // Green
            actionDanger = new Color(220, 53, 69); // Red
            actionWarning = new Color(255, 193, 7); // Yellow
        }
    }

    public boolean isDark() {
        return isDark;
    }

    // --- Priority Styles ---
    public static class PriorityStyle {
        public String regex;
        public Color color;
        public boolean bold;

        public PriorityStyle(String regex, Color color, boolean bold) {
            this.regex = regex;
            this.color = color;
            this.bold = bold;
        }
    }

    private final Map<String, PriorityStyle> priorityStyles = new HashMap<>();

    public void load(Properties props, String prefix) {
        // Status colors
        String keyStart = prefix + ".color.";
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(keyStart)) {
                try {
                    String statusName = key.substring(keyStart.length()).toLowerCase().trim();
                    statusColors.put(statusName, Color.decode(props.getProperty(key)));
                } catch (Exception ignored) {
                }
            }
        }
        
        // Priority styles
        // Format: prefix.priority.[name].regex
        // Format: prefix.priority.[name].color
        // Format: prefix.priority.[name].bold
        String priStart = prefix + ".priority.";
        // We scan for names by looking for .regex
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(priStart) && key.endsWith(".regex")) {
                String name = key.substring(priStart.length(), key.length() - 6); // remove .regex
                String regex = props.getProperty(key);
                Color color = Color.BLACK; 
                boolean bold = false;
                
                String colorStr = props.getProperty(priStart + name + ".color");
                if (colorStr != null) {
                    try { color = Color.decode(colorStr); } catch (Exception e) {}
                }
                
                 String boldStr = props.getProperty(priStart + name + ".bold");
                 if (boldStr != null) {
                     bold = Boolean.parseBoolean(boldStr);
                 }
                 
                 priorityStyles.put(name, new PriorityStyle(regex, color, bold));
            }
        }
        
        // Ensure defaults if missing
        if (priorityStyles.isEmpty()) {
            loadPriorityDefaults();
        }
    }

    public Color getColor(String status) {
        if (status == null)
            return null;
        return statusColors.getOrDefault(status.toLowerCase().trim(), isDark ? textPrimary : Color.BLACK);
    }
    
    public PriorityStyle getPriorityStyle(String priorityName) {
        if (priorityName == null) return null;
        String p = priorityName.toLowerCase();
        
        // Check exact match first
        // But usually we iterate regexes. 
        // Let's iterate values to find a match. Order matters? Hashmap is unordered.
        // Ideally we should have a list or fixed keys. 
        // For now, let's hardcode the check order or relying on the map keys being "urgent", "high", etc.
        
        // Priority check order: Immediate > Urgent > High > Normal > Low
        if (checkPriorityMatch(p, "immediate")) return priorityStyles.get("immediate");
        if (checkPriorityMatch(p, "urgent")) return priorityStyles.get("urgent");
        if (checkPriorityMatch(p, "high")) return priorityStyles.get("high");
        if (checkPriorityMatch(p, "normal")) return priorityStyles.get("normal");
        if (checkPriorityMatch(p, "low")) return priorityStyles.get("low");
        
        return null; // No special style
    }
    
    private boolean checkPriorityMatch(String text, String key) {
        PriorityStyle style = priorityStyles.get(key);
        if (style == null || style.regex == null || style.regex.isEmpty()) return false;
        try {
            return text.matches(style.regex);
        } catch (Exception e) {
            return false;
        }
    }

    public void loadDefaults() {
        // Defaults could be adjusted for themes if needed
        Color defaultStatus = isDark ? textPrimary : Color.BLACK;
        statusColors.put("nueva", defaultStatus);
        statusColors.put("new", defaultStatus);
        statusColors.put("en curso", actionPrimary);
        statusColors.put("in progress", actionPrimary);
        statusColors.put("resuelta", actionSuccess);
        statusColors.put("resolved", actionSuccess);
        statusColors.put("cerrada", textSecondary);
        statusColors.put("closed", textSecondary);
        statusColors.put("rechazada", actionDanger);
        statusColors.put("rejected", actionDanger);
        
        loadPriorityDefaults();
    }
    
    public void loadPriorityDefaults() {
        if (!priorityStyles.containsKey("immediate")) {
            // Magenta/Purple for Immediate
            priorityStyles.put("immediate", new PriorityStyle(".*(immediate|inmediata).*", new Color(255, 0, 255), true));
        }
        if (!priorityStyles.containsKey("urgent")) {
            priorityStyles.put("urgent", new PriorityStyle(".*(urgent|urgente).*", actionDanger, true));
        }
        if (!priorityStyles.containsKey("high")) {
            priorityStyles.put("high", new PriorityStyle(".*(high|alta|elevada).*", actionWarning, false));
        }
        if (!priorityStyles.containsKey("normal")) { // usually no special style, but let's define it so it's editable
            priorityStyles.put("normal", new PriorityStyle(".*(normal|media).*", actionSuccess, false));
        }
        // Low?
        if (!priorityStyles.containsKey("low")) {
            priorityStyles.put("low", new PriorityStyle(".*(low|baja).*", isDark ? Color.GRAY : Color.LIGHT_GRAY, false));
        }
    }
    
    public Map<String, PriorityStyle> getPriorityStyles() {
        return priorityStyles;
    }
}
