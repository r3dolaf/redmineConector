package redmineconnector.test;

import redmineconnector.model.WikiPageDTO;

/**
 * Unit tests for WikiPageDTO model.
 * Tests wiki page properties and version handling.
 * 
 * WikiPageDTO API:
 * - new WikiPageDTO(String title, String text, String version, String
 * updatedOn, String author)
 * - new WikiPageDTO(String title) - convenience constructor
 */
public class WikiPageDTOTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("WikiPageDTO - Full constructor", () -> {
            WikiPageDTO page = new WikiPageDTO(
                    "Home",
                    "# Welcome\n\nThis is the home page.",
                    "5",
                    "2025-12-29T10:30:00Z",
                    "John Doe");

            SimpleTestRunner.assertTrue("Home".equals(page.title), "Title should be set");
            SimpleTestRunner.assertTrue(page.text.contains("Welcome"), "Text should be set");
            SimpleTestRunner.assertTrue("5".equals(page.version), "Version should be set");
            SimpleTestRunner.assertTrue("2025-12-29T10:30:00Z".equals(page.updatedOn), "UpdatedOn should be set");
            SimpleTestRunner.assertTrue("John Doe".equals(page.author), "Author should be set");
        });

        runner.run("WikiPageDTO - Convenience constructor", () -> {
            WikiPageDTO page = new WikiPageDTO("Setup");

            SimpleTestRunner.assertTrue("Setup".equals(page.title), "Title should be set");
            SimpleTestRunner.assertTrue("".equals(page.text), "Text should be empty");
            SimpleTestRunner.assertTrue("".equals(page.version), "Version should be empty");
            SimpleTestRunner.assertTrue("".equals(page.updatedOn), "UpdatedOn should be empty");
            SimpleTestRunner.assertTrue("".equals(page.author), "Author should be empty");
            SimpleTestRunner.assertNotNull(page.attachments, "Attachments list should be initialized");
        });

        runner.run("WikiPageDTO - Page titles", () -> {
            String[] titles = {
                    "Home",
                    "Installation",
                    "API_Documentation",
                    "User-Guide",
                    "FAQ"
            };

            for (String title : titles) {
                WikiPageDTO page = new WikiPageDTO(title);
                SimpleTestRunner.assertTrue(
                        title.equals(page.title),
                        "Page title: " + title);
            }
        });

        runner.run("WikiPageDTO - Version as string", () -> {
            String[] versions = { "1", "2", "10", "100" };

            for (String version : versions) {
                WikiPageDTO page = new WikiPageDTO("Page", "Text", version, "2025-12-29", "User");
                SimpleTestRunner.assertTrue(
                        version.equals(page.version),
                        "Version: " + version);
            }
        });

        runner.run("WikiPageDTO - Empty text", () -> {
            WikiPageDTO page = new WikiPageDTO("Page", "", "1", "2025-12-29", "User");

            SimpleTestRunner.assertTrue("".equals(page.text), "Empty text allowed");
        });

        runner.run("WikiPageDTO - Null text", () -> {
            WikiPageDTO page = new WikiPageDTO("Page", null, "1", "2025-12-29", "User");

            SimpleTestRunner.assertNull(page.text, "Text can be null");
        });

        runner.run("WikiPageDTO - Markdown content", () -> {
            String markdown = "# Header\n\n## Subheader\n\n* List item\n* Another item\n\n```java\ncode block\n```";
            WikiPageDTO page = new WikiPageDTO("Doc", markdown, "1", "2025-12-29", "User");

            SimpleTestRunner.assertTrue(page.text.contains("# Header"), "Markdown headers");
            SimpleTestRunner.assertTrue(page.text.contains("```java"), "Code blocks");
        });

        runner.run("WikiPageDTO - Textile content", () -> {
            String textile = "h1. Header\n\nh2. Subheader\n\n* List\n** Nested";
            WikiPageDTO page = new WikiPageDTO("Doc", textile, "1", "2025-12-29", "User");

            SimpleTestRunner.assertTrue(page.text.contains("h1."), "Textile headers");
        });

        runner.run("WikiPageDTO - Long content", () -> {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                content.append("Line ").append(i).append("\n");
            }
            WikiPageDTO page = new WikiPageDTO("LongPage", content.toString(), "1", "2025-12-29", "User");

            SimpleTestRunner.assertTrue(
                    page.text.length() > 5000,
                    "Should handle long wiki pages");
        });

        runner.run("WikiPageDTO - Special characters in title", () -> {
            WikiPageDTO page = new WikiPageDTO("API_v2.0");

            SimpleTestRunner.assertTrue(
                    page.title.contains("_"),
                    "Should preserve underscores");
        });

        runner.run("WikiPageDTO - toString representation", () -> {
            WikiPageDTO page = new WikiPageDTO("Test Page", "Content", "5", "2025-12-29", "User");

            String str = page.toString();
            SimpleTestRunner.assertNotNull(str, "toString should return value");
            // toString returns title
            SimpleTestRunner.assertTrue(
                    "Test Page".equals(str),
                    "toString should return title");
        });

        runner.run("WikiPageDTO - Attachments list initialized", () -> {
            WikiPageDTO page = new WikiPageDTO("Page");

            SimpleTestRunner.assertNotNull(page.attachments, "Attachments should be initialized");
            SimpleTestRunner.assertTrue(page.attachments.isEmpty(), "Attachments should be empty initially");
        });
    }
}
