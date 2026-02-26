package redmineconnector.test;

import redmineconnector.config.ConfigManager;
import redmineconnector.ui.theme.Theme;
import redmineconnector.ui.theme.ThemeConfig;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

/**
 * Unit tests for ConfigManager - Configuration persistence and encryption.
 * Tests save/load, encryption, theme management, and migration.
 */
public class ConfigManagerTest {

        public static void runTests(SimpleTestRunner runner) {

                runner.run("ConfigManager.saveAndLoad - Basic properties", () -> {
                        Properties props = new Properties();
                        props.setProperty("test.url", "https://example.com");
                        props.setProperty("test.limit", "100");
                        props.setProperty("test.name", "TestClient");

                        ConfigManager.saveConfig(props);
                        Properties loaded = ConfigManager.loadConfig();

                        SimpleTestRunner.assertTrue(
                                        "https://example.com".equals(loaded.getProperty("test.url")),
                                        "URL should match");
                        SimpleTestRunner.assertTrue(
                                        "100".equals(loaded.getProperty("test.limit")),
                                        "Limit should match");
                        SimpleTestRunner.assertTrue(
                                        "TestClient".equals(loaded.getProperty("test.name")),
                                        "Name should match");

                        // Cleanup
                        new File("redmine_config.properties").delete();
                });

                runner.run("ConfigManager.encryption - API keys encrypted in file", () -> {
                        try {
                                Properties props = new Properties();
                                props.setProperty("client1.key", "mySecretApiKey123");
                                props.setProperty("client1.url", "https://redmine.example.com");

                                ConfigManager.saveConfig(props);

                                // Read file directly to verify encryption
                                Properties rawFile = new Properties();
                                try (FileInputStream fis = new FileInputStream("redmine_config.properties")) {
                                        rawFile.load(fis);
                                }

                                String rawKey = rawFile.getProperty("client1.key");
                                String rawUrl = rawFile.getProperty("client1.url");

                                SimpleTestRunner.assertNotEquals(
                                                "mySecretApiKey123",
                                                rawKey,
                                                "Key should be encrypted in file");
                                SimpleTestRunner.assertTrue(
                                                "https://redmine.example.com".equals(rawUrl),
                                                "Non-key properties should NOT be encrypted");
                                SimpleTestRunner.assertTrue(
                                                rawKey != null && rawKey.length() > 20,
                                                "Encrypted key should be longer");

                                // Cleanup
                                new File("redmine_config.properties").delete();
                        } catch (Exception e) {
                                throw new RuntimeException("Test failed", e);
                        }
                });

                runner.run("ConfigManager.encryption - Decryption on load", () -> {
                        Properties props = new Properties();
                        props.setProperty("client2.key", "testKey456");

                        ConfigManager.saveConfig(props);
                        Properties loaded = ConfigManager.loadConfig();

                        SimpleTestRunner.assertTrue(
                                        "testKey456".equals(loaded.getProperty("client2.key")),
                                        "Key should be decrypted when loaded");

                        // Cleanup
                        new File("redmine_config.properties").delete();
                });

                runner.run("ConfigManager.encryption - Multiple keys", () -> {
                        Properties props = new Properties();
                        props.setProperty("client1.key", "key1");
                        props.setProperty("client2.key", "key2");
                        props.setProperty("client3.key", "key3");
                        props.setProperty("client1.url", "url1");

                        ConfigManager.saveConfig(props);
                        Properties loaded = ConfigManager.loadConfig();

                        SimpleTestRunner.assertTrue("key1".equals(loaded.getProperty("client1.key")), "Client1 key");
                        SimpleTestRunner.assertTrue("key2".equals(loaded.getProperty("client2.key")), "Client2 key");
                        SimpleTestRunner.assertTrue("key3".equals(loaded.getProperty("client3.key")), "Client3 key");
                        SimpleTestRunner.assertTrue("url1".equals(loaded.getProperty("client1.url")), "URL");

                        // Cleanup
                        new File("redmine_config.properties").delete();
                });

                runner.run("ConfigManager.loadConfig - Empty/missing file", () -> {
                        File configFile = new File("redmine_config.properties");
                        configFile.delete();

                        Properties props = ConfigManager.loadConfig();
                        SimpleTestRunner.assertNotNull(props, "Should return empty properties");
                        SimpleTestRunner.assertTrue(props.isEmpty(), "Should be empty for missing file");
                });

                runner.run("ConfigManager.theme - Save and load LIGHT theme", () -> {
                        Properties props = new Properties();
                        ConfigManager.saveTheme(props, Theme.LIGHT);

                        ConfigManager.saveConfig(props);
                        Properties loaded = ConfigManager.loadConfig();

                        ThemeConfig theme = ConfigManager.loadTheme(loaded);
                        SimpleTestRunner.assertEquals(
                                        Theme.LIGHT,
                                        theme,
                                        "Should load LIGHT theme");

                        // Cleanup
                        new File("redmine_config.properties").delete();
                });

                runner.run("ConfigManager.theme - Save and load theme property", () -> {
                        Properties props = new Properties();
                        props.setProperty("app.theme", "LIGHT");

                        ConfigManager.saveConfig(props);
                        Properties loaded = ConfigManager.loadConfig();

                        SimpleTestRunner.assertTrue(
                                        "LIGHT".equals(loaded.getProperty("app.theme")),
                                        "Theme property should be saved");

                        // Cleanup
                        new File("redmine_config.properties").delete();
                });

                runner.run("ConfigManager.theme - Default to LIGHT", () -> {
                        Properties emptyProps = new Properties();
                        ThemeConfig theme = ConfigManager.loadTheme(emptyProps);

                        SimpleTestRunner.assertEquals(
                                        Theme.LIGHT,
                                        theme,
                                        "Should default to LIGHT theme");
                });

                runner.run("ConfigManager - Properties isolation", () -> {
                        // Verify that saveConfig doesn't modify original properties
                        Properties original = new Properties();
                        original.setProperty("test.key", "plainValue");

                        String originalValue = original.getProperty("test.key");
                        ConfigManager.saveConfig(original);
                        String afterSave = original.getProperty("test.key");

                        SimpleTestRunner.assertTrue(
                                        originalValue.equals(afterSave),
                                        "Original properties should not be modified");

                        // Cleanup
                        new File("redmine_config.properties").delete();
                });

                runner.run("ConfigManager - Special characters in values", () -> {
                        Properties props = new Properties();
                        props.setProperty("test.path", "C:\\Users\\test\\path");
                        props.setProperty("test.url", "https://example.com/path?param=value&other=123");

                        ConfigManager.saveConfig(props);
                        Properties loaded = ConfigManager.loadConfig();

                        SimpleTestRunner.assertTrue(
                                        "C:\\Users\\test\\path".equals(loaded.getProperty("test.path")),
                                        "Path with backslashes should be preserved");
                        SimpleTestRunner.assertTrue(
                                        loaded.getProperty("test.url").contains("param=value"),
                                        "URL with special chars should be preserved");

                        // Cleanup
                        new File("redmine_config.properties").delete();
                });

                runner.run("ConfigManager - Empty values", () -> {
                        Properties props = new Properties();
                        props.setProperty("test.empty", "");
                        props.setProperty("test.nonempty", "value");

                        ConfigManager.saveConfig(props);
                        Properties loaded = ConfigManager.loadConfig();

                        SimpleTestRunner.assertTrue(
                                        loaded.containsKey("test.empty"),
                                        "Empty value key should exist");
                        SimpleTestRunner.assertTrue(
                                        "".equals(loaded.getProperty("test.empty")),
                                        "Empty value should be preserved");

                        // Cleanup
                        new File("redmine_config.properties").delete();
                });
        }
}
