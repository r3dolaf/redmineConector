package redmineconnector.test;

import java.awt.Color;
import java.util.Properties;

import redmineconnector.config.StyleConfig;

/**
 * Unit tests for StyleConfig - Status color management.
 * 
 * StyleConfig API:
 * - Default status color is Color.BLACK (light theme) or textPrimary (dark
 * theme)
 * - loadDefaults() sets statuses to theme-appropriate colors
 */
public class StyleConfigTest {

    public static void runTests(SimpleTestRunner runner) {
        runner.run("StyleConfig.Defaults", () -> {
            StyleConfig config = new StyleConfig();
            config.loadDefaults();

            // Default status color is BLACK in light theme (see line 113 in
            // StyleConfig.java)
            Color expectedDefault = Color.BLACK;

            SimpleTestRunner.assertEquals(expectedDefault, config.getColor("Nueva"), "Default 'Nueva' should be BLACK");

            // "Closed" is mapped to textSecondary (grey) in loadDefaults()
            Color expectedClosed = config.textSecondary;
            SimpleTestRunner.assertEquals(expectedClosed, config.getColor("Closed"),
                    "Default 'Closed' should be textSecondary (grey)");
        });

        runner.run("StyleConfig.LoadFromProperties", () -> {
            Properties props = new Properties();
            props.setProperty("client1.color.urgent", "#FF0000"); // Red
            props.setProperty("client1.color.done", "#00FF00"); // Green

            StyleConfig config = new StyleConfig();
            config.load(props, "client1");

            SimpleTestRunner.assertEquals(Color.RED, config.getColor("Urgent"), "Should parse #FF0000 as RED");
            SimpleTestRunner.assertEquals(Color.GREEN, config.getColor("Done"), "Should parse #00FF00 as GREEN");

            // "Nueva" is not loaded from properties, so it returns the default BLACK
            // (load() doesn't call loadDefaults(), so it returns BLACK as fallback)
            Color expectedUnloaded = Color.BLACK;
            SimpleTestRunner.assertEquals(expectedUnloaded, config.getColor("Nueva"),
                    "Unloaded status should be BLACK (default fallback)");
        });

        runner.run("StyleConfig.CaseInsensitivity", () -> {
            StyleConfig config = new StyleConfig();
            config.loadDefaults();
            SimpleTestRunner.assertEquals(config.getColor("nuEva"), config.getColor("NUEVA"),
                    "Should be case insensitive");
        });

        runner.run("StyleConfig.DarkTheme", () -> {
            StyleConfig config = new StyleConfig();
            config.setTheme(true); // Dark theme

            SimpleTestRunner.assertTrue(config.isDark(), "Should be dark theme");
            SimpleTestRunner.assertTrue(config.bgMain.getRGB() != Color.WHITE.getRGB(),
                    "Dark theme should have dark background");
        });

        runner.run("StyleConfig.LightTheme", () -> {
            StyleConfig config = new StyleConfig();
            config.setTheme(false); // Light theme (default)

            SimpleTestRunner.assertTrue(!config.isDark(), "Should be light theme");
            SimpleTestRunner.assertEquals(Color.WHITE, config.bgPanel,
                    "Light theme should have white panel background");
        });
    }
}
