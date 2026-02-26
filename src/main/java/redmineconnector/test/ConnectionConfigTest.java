package redmineconnector.test;

import java.util.Properties;
import java.util.regex.Pattern;

import redmineconnector.config.ConnectionConfig;

public class ConnectionConfigTest {

    public static void runTests(SimpleTestRunner runner) {
        runner.run("ConnectionConfig.Defaults", () -> {
            Properties props = new Properties();
            ConnectionConfig config = new ConnectionConfig("test", props);

            SimpleTestRunner.assertEquals("https://redmine.ejemplo.com/", config.url, "Default URL mismatch");
            SimpleTestRunner.assertEquals(0, config.limit, "Default limit mismatch");
            SimpleTestRunner.assertTrue(config.showNotifications, "Notifications default true");
        });

        runner.run("ConnectionConfig.Parsing", () -> {
            Properties props = new Properties();
            props.setProperty("test.url", "http://myredmine.com");
            props.setProperty("test.limit", "50");
            props.setProperty("test.closed", "true");

            ConnectionConfig config = new ConnectionConfig("test", props);

            SimpleTestRunner.assertEquals("http://myredmine.com", config.url, "URL parsing failed");
            SimpleTestRunner.assertEquals(50, config.limit, "Limit parsing failed");
            SimpleTestRunner.assertTrue(config.showClosed, "ShowClosed parsing failed");
        });

        runner.run("ConnectionConfig.IntegerSafety", () -> {
            Properties props = new Properties();
            props.setProperty("test.limit", "invalid_int");
            ConnectionConfig config = new ConnectionConfig("test", props);

            SimpleTestRunner.assertEquals(0, config.limit, "Should fallback to 0 on invalid int");
        });

        runner.run("ConnectionConfig.RegexPattern", () -> {
            Properties props = new Properties();
            props.setProperty("test.pattern", "[Ref #{id}]");
            ConnectionConfig config = new ConnectionConfig("test", props);

            Pattern p = config.getExtractionPattern();
            SimpleTestRunner.assertNotNull(p, "Pattern should not be null");
            // Regex for [Ref #{id}] -> \Q[Ref #\E(\d+)\Q]\E approx

            java.util.regex.Matcher m = p.matcher("Commit message [Ref #1234]");
            SimpleTestRunner.assertTrue(m.find(), "Should match pattern");
            SimpleTestRunner.assertEquals("1234", m.group(1), "Should extract ID");
        });
    }
}
