package redmineconnector.test;

import redmineconnector.model.TimeEntry;

/**
 * Unit tests for TimeEntry model.
 * Tests time logging properties and calculations.
 * 
 * TimeEntry API: new TimeEntry(int id, int issueId, String issueSubject, String
 * user,
 * double hours, String spentOn, String comment)
 */
public class TimeEntryTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("TimeEntry - Constructor with all parameters", () -> {
            TimeEntry entry = new TimeEntry(
                    1,
                    101,
                    "Login Feature",
                    "John Doe",
                    3.5,
                    "2025-12-29",
                    "Implemented login feature");

            SimpleTestRunner.assertTrue(entry.id == 1, "ID should be set");
            SimpleTestRunner.assertTrue(entry.issueId == 101, "Issue ID should be set");
            SimpleTestRunner.assertTrue("Login Feature".equals(entry.issueSubject), "Issue subject should be set");
            SimpleTestRunner.assertTrue("John Doe".equals(entry.user), "User should be set");
            SimpleTestRunner.assertTrue(entry.hours == 3.5, "Hours should be set");
            SimpleTestRunner.assertTrue("2025-12-29".equals(entry.spentOn), "Date should be set");
            SimpleTestRunner.assertTrue(
                    "Implemented login feature".equals(entry.comment),
                    "Comment should be set");
        });

        runner.run("TimeEntry - Fractional hours", () -> {
            TimeEntry entry1 = new TimeEntry(1, 100, "Task", "User", 0.25, "2025-12-29", "Quarter hour");
            SimpleTestRunner.assertTrue(entry1.hours == 0.25, "Quarter hour");

            TimeEntry entry2 = new TimeEntry(2, 100, "Task", "User", 1.75, "2025-12-29", "Hour and three quarters");
            SimpleTestRunner.assertTrue(entry2.hours == 1.75, "Hour and three quarters");
        });

        runner.run("TimeEntry - Zero hours", () -> {
            TimeEntry entry = new TimeEntry(1, 100, "Task", "User", 0.0, "2025-12-29", "No time");

            SimpleTestRunner.assertTrue(entry.hours == 0.0, "Zero hours allowed");
        });

        runner.run("TimeEntry - Large hours", () -> {
            TimeEntry entry = new TimeEntry(1, 100, "Task", "User", 24.0, "2025-12-29", "Full day");

            SimpleTestRunner.assertTrue(entry.hours == 24.0, "Should handle 24 hours");
        });

        runner.run("TimeEntry - Date format", () -> {
            TimeEntry entry = new TimeEntry(1, 100, "Task", "User", 8.0, "2025-12-29", "Work");

            SimpleTestRunner.assertTrue(
                    "2025-12-29".equals(entry.spentOn),
                    "Date format yyyy-MM-dd");
        });

        runner.run("TimeEntry - Null comment", () -> {
            TimeEntry entry = new TimeEntry(1, 100, "Task", "User", 5.0, "2025-12-29", null);

            SimpleTestRunner.assertNull(entry.comment, "Comment can be null");
        });

        runner.run("TimeEntry - Empty comment", () -> {
            TimeEntry entry = new TimeEntry(1, 100, "Task", "User", 5.0, "2025-12-29", "");

            SimpleTestRunner.assertTrue("".equals(entry.comment), "Empty comment allowed");
        });

        runner.run("TimeEntry - Various issue subjects", () -> {
            String[] subjects = {
                    "Development",
                    "Bug Fixing",
                    "Code Review",
                    "Testing",
                    "Documentation"
            };

            for (int i = 0; i < subjects.length; i++) {
                TimeEntry entry = new TimeEntry(i, 100 + i, subjects[i], "User", 2.0, "2025-12-29", "Work");
                SimpleTestRunner.assertTrue(
                        subjects[i].equals(entry.issueSubject),
                        "Issue subject: " + subjects[i]);
            }
        });

        runner.run("TimeEntry - Multiple users", () -> {
            String[] users = { "John Doe", "Jane Smith", "developer@example.com" };

            for (int i = 0; i < users.length; i++) {
                TimeEntry entry = new TimeEntry(i, 100, "Task", users[i], 4.0, "2025-12-29", "Work");
                SimpleTestRunner.assertTrue(
                        users[i].equals(entry.user),
                        "User: " + users[i]);
            }
        });
    }
}
