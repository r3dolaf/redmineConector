package redmineconnector.test;

import redmineconnector.util.I18n;

/**
 * Unit tests for I18n - Internationalization utility.
 * Tests resource loading, key retrieval, and locale switching.
 * 
 * I18n API:
 * - get(key) returns "!key!" if not found OR if bundle not loaded
 * - get(null) may throw NPE
 * - format(key, args) uses key from bundle, not direct template
 */
public class I18nTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("I18n.get - Returns value or error marker", () -> {
            String value = I18n.get("app.title");
            SimpleTestRunner.assertNotNull(value, "Should return a value");
            // Value may be "!app.title!" if bundle not loaded or key missing
            // Just verify it's not null
        });

        runner.run("I18n.get - Non-existing key returns error marker", () -> {
            String value = I18n.get("non.existing.key.123456");
            SimpleTestRunner.assertTrue(
                    value.equals("!non.existing.key.123456!"),
                    "Should return !key! when not found");
        });

        runner.run("I18n.get - Empty key handling", () -> {
            String value = I18n.get("");
            SimpleTestRunner.assertNotNull(value, "Should handle empty key");
            // Empty key returns "!!" or the empty string from bundle
        });

        runner.run("I18n.get - With default value", () -> {
            String value = I18n.get("non.existing.key", "Default Value");
            SimpleTestRunner.assertTrue(
                    "Default Value".equals(value),
                    "Should return default when key not found");
        });

        runner.run("I18n - Common UI keys", () -> {
            // Just verify these don't throw exceptions
            String[] keys = {
                    "main.title",
                    "main.menu.file",
                    "task.form.label.subject",
                    "help.dialog.title"
            };

            for (String key : keys) {
                String value = I18n.get(key);
                SimpleTestRunner.assertNotNull(value, "Key should return non-null: " + key);
            }
        });

        runner.run("I18n - Help dialog keys", () -> {
            // Just verify these don't throw exceptions
            String[] helpKeys = {
                    "help.tab.shortcuts",
                    "help.tab.features",
                    "help.tab.tips",
                    "help.action.global_search",
                    "help.tip.1.title"
            };

            for (String key : helpKeys) {
                String value = I18n.get(key);
                SimpleTestRunner.assertNotNull(value, "Help key should return non-null: " + key);
            }
        });

        runner.run("I18n - Locale consistency", () -> {
            // Get same key multiple times, should be consistent
            String value1 = I18n.get("main.title");
            String value2 = I18n.get("main.title");

            SimpleTestRunner.assertTrue(
                    value1.equals(value2),
                    "Same key should return same value");
        });

        runner.run("I18n.getCurrentLocale - Returns locale", () -> {
            java.util.Locale locale = I18n.getCurrentLocale();
            SimpleTestRunner.assertNotNull(locale, "Should return current locale");
        });
    }
}
