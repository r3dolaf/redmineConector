package redmineconnector.test;

import redmineconnector.model.VersionDTO;

/**
 * Unit tests for VersionDTO model.
 * Tests version/milestone properties and status handling.
 * 
 * VersionDTO API: new VersionDTO(int id, String name, String status, String
 * startDate, String dueDate)
 */
public class VersionDTOTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("VersionDTO - Constructor with all parameters", () -> {
            VersionDTO version = new VersionDTO(
                    10,
                    "v1.0.0",
                    "open",
                    "2025-01-01",
                    "2025-12-31");

            SimpleTestRunner.assertTrue(version.id == 10, "ID should be set");
            SimpleTestRunner.assertTrue("v1.0.0".equals(version.name), "Name should be set");
            SimpleTestRunner.assertTrue("open".equals(version.status), "Status should be set");
            SimpleTestRunner.assertTrue("2025-01-01".equals(version.startDate), "Start date should be set");
            SimpleTestRunner.assertTrue("2025-12-31".equals(version.dueDate), "Due date should be set");
        });

        runner.run("VersionDTO - Status values", () -> {
            String[] statuses = { "open", "locked", "closed" };

            for (int i = 0; i < statuses.length; i++) {
                VersionDTO version = new VersionDTO(i, "v1." + i, statuses[i], "2025-01-01", "2025-12-31");
                SimpleTestRunner.assertTrue(
                        statuses[i].equals(version.status),
                        "Status: " + statuses[i]);
            }
        });

        runner.run("VersionDTO - Version naming patterns", () -> {
            String[] names = {
                    "v1.0",
                    "v2.5.1",
                    "Sprint 10",
                    "Release 2025-Q1",
                    "Milestone 3"
            };

            for (int i = 0; i < names.length; i++) {
                VersionDTO version = new VersionDTO(i, names[i], "open", null, null);
                SimpleTestRunner.assertTrue(
                        names[i].equals(version.name),
                        "Version name: " + names[i]);
            }
        });

        runner.run("VersionDTO - Null dates", () -> {
            VersionDTO version = new VersionDTO(1, "Backlog", "open", null, null);

            SimpleTestRunner.assertNull(version.startDate, "Start date can be null");
            SimpleTestRunner.assertNull(version.dueDate, "Due date can be null");
        });

        runner.run("VersionDTO - Empty strings", () -> {
            VersionDTO version = new VersionDTO(2, "", "", "", "");

            SimpleTestRunner.assertTrue("".equals(version.name), "Empty name");
            SimpleTestRunner.assertTrue("".equals(version.status), "Empty status");
        });

        runner.run("VersionDTO - Date format", () -> {
            VersionDTO version = new VersionDTO(3, "v1.0", "open", "2025-01-01", "2025-12-31");

            SimpleTestRunner.assertTrue(
                    "2025-12-31".equals(version.dueDate),
                    "Date format yyyy-MM-dd");
        });

        runner.run("VersionDTO - Special characters in name", () -> {
            VersionDTO version = new VersionDTO(4, "v1.0-beta (pre-release)", "open", null, null);

            SimpleTestRunner.assertTrue(
                    version.name.contains("beta"),
                    "Should preserve special characters in name");
        });

        runner.run("VersionDTO - toString representation", () -> {
            VersionDTO version = new VersionDTO(42, "Test Version", "open", null, null);

            String str = version.toString();
            SimpleTestRunner.assertNotNull(str, "toString should return value");
            // toString format: "name (status)"
            SimpleTestRunner.assertTrue(
                    str.contains("Test Version"),
                    "toString should contain name");
            SimpleTestRunner.assertTrue(
                    str.contains("open"),
                    "toString should contain status");
        });

        runner.run("VersionDTO - Various IDs", () -> {
            int[] ids = { 1, 100, 999, 12345 };

            for (int id : ids) {
                VersionDTO version = new VersionDTO(id, "v1.0", "open", null, null);
                SimpleTestRunner.assertTrue(
                        version.id == id,
                        "ID: " + id);
            }
        });
    }
}
