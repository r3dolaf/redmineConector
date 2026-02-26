package redmineconnector.test;

import redmineconnector.model.Journal;

/**
 * Unit tests for Journal model (task history/notes).
 * Tests construction, properties, and edge cases.
 * 
 * Journal API: new Journal(String user, String notes, String createdOn)
 */
public class JournalTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("Journal - Constructor with all parameters", () -> {
            Journal journal = new Journal(
                    "John Doe",
                    "This is a note",
                    "2025-12-29T10:30:00Z");

            SimpleTestRunner.assertTrue("John Doe".equals(journal.user), "User should be set");
            SimpleTestRunner.assertTrue(
                    "This is a note".equals(journal.notes),
                    "Notes should be set");
            SimpleTestRunner.assertTrue(
                    "2025-12-29T10:30:00Z".equals(journal.createdOn),
                    "Created date should be set");
        });

        runner.run("Journal - Field access", () -> {
            Journal journal = new Journal("Jane Smith", "Updated status", "2025-12-29T14:00:00Z");

            SimpleTestRunner.assertTrue("Jane Smith".equals(journal.user), "User");
            SimpleTestRunner.assertTrue("Updated status".equals(journal.notes), "Notes");
            SimpleTestRunner.assertTrue("2025-12-29T14:00:00Z".equals(journal.createdOn), "CreatedOn");
        });

        runner.run("Journal - Empty notes", () -> {
            Journal journal = new Journal("User", "", "2025-12-29");

            SimpleTestRunner.assertTrue("".equals(journal.notes), "Empty notes allowed");
        });

        runner.run("Journal - Null values", () -> {
            Journal journal = new Journal(null, null, null);

            SimpleTestRunner.assertNull(journal.user, "User can be null");
            SimpleTestRunner.assertNull(journal.notes, "Notes can be null");
            SimpleTestRunner.assertNull(journal.createdOn, "CreatedOn can be null");
        });

        runner.run("Journal - Long notes", () -> {
            StringBuilder longNotes = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longNotes.append("This is a very long note. ");
            }
            Journal journal = new Journal("User", longNotes.toString(), "2025-12-29");

            SimpleTestRunner.assertTrue(
                    journal.notes.length() > 10000,
                    "Should handle long notes");
        });

        runner.run("Journal - Special characters in notes", () -> {
            Journal journal = new Journal("User", "Note with special chars: <>&\"'", "2025-12-29");

            SimpleTestRunner.assertTrue(
                    journal.notes.contains("<>"),
                    "Should preserve special characters");
        });

        runner.run("Journal - Date formats", () -> {
            String[] dates = {
                    "2025-12-29T10:30:00Z",
                    "2025-12-29",
                    "2025-12-29 10:30:00"
            };

            for (String date : dates) {
                Journal journal = new Journal("User", "Note", date);
                SimpleTestRunner.assertTrue(
                        date.equals(journal.createdOn),
                        "Date format: " + date);
            }
        });

        runner.run("Journal - Multiple users", () -> {
            String[] users = { "John Doe", "Jane Smith", "Admin", "developer@example.com" };

            for (String user : users) {
                Journal journal = new Journal(user, "Note", "2025-12-29");
                SimpleTestRunner.assertTrue(
                        user.equals(journal.user),
                        "User: " + user);
            }
        });
    }
}
