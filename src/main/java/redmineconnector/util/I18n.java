package redmineconnector.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * Utility class for Internationalization.
 * Loads messages from 'redmineconnector.resources.messages'.
 * Supports UTF-8 encoding for accents and special characters.
 */
public class I18n {
    private static final String BUNDLE_NAME = "redmineconnector.resources.messages";
    private static ResourceBundle RESOURCE_BUNDLE;

    static {
        loadBundle(null);
    }

    public static void loadBundle(Locale locale) {
        Locale l = locale;
        if (l == null) {
            l = Locale.getDefault();
            try {
                java.util.Properties p = new java.util.Properties();
                java.io.File f = new java.io.File("redmine_config.properties");
                if (f.exists()) {
                    try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                        p.load(in);
                        String lang = p.getProperty("app.language");
                        if (lang != null && !lang.isEmpty()) {
                            l = new Locale(lang);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        try {
            // Force UTF-8 loading
            RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, l, new UTF8Control());
            Locale.setDefault(l); // Sync default locale
        } catch (Exception e) {
            System.err.println("ERROR: Could not load ResourceBundle: " + BUNDLE_NAME);
            redmineconnector.util.LoggerUtil.logError("I18n",
                    "Failed to load resource bundle: " + BUNDLE_NAME, e);
        }
    }

    private I18n() {
    }

    /**
     * Custom Control to load properties files in UTF-8.
     */
    private static class UTF8Control extends Control {
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    // Use InputStreamReader with UTF-8
                    bundle = new java.util.PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }

        @Override
        public List<String> getFormats(String baseName) {
            return Collections.singletonList("java.properties");
        }
    }

    /**
     * Get a string by key.
     * 
     * @param key the message key
     * @return the localized string or !key! if missing
     */
    public static String get(String key) {
        if (RESOURCE_BUNDLE == null)
            return "!" + key + "!";
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * Get a string by key with a default value fallback.
     * 
     * @param key the message key
     * @param def default value if key is missing
     * @return the localized string or default value
     */
    public static String get(String key, String def) {
        if (RESOURCE_BUNDLE == null)
            return def;
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return def;
        }
    }

    /**
     * Get a formatted string by key with arguments.
     * Use {0}, {1} in properties file.
     * 
     * @param key  the message key
     * @param args arguments to replace placeholders
     * @return the formatted string
     */
    public static String format(String key, Object... args) {
        if (RESOURCE_BUNDLE == null)
            return "!" + key + "!";
        try {
            String pattern = RESOURCE_BUNDLE.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static Locale getCurrentLocale() {
        return RESOURCE_BUNDLE != null ? RESOURCE_BUNDLE.getLocale() : Locale.getDefault();
    }
}
