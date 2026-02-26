package redmineconnector.test;

import redmineconnector.model.Attachment;

/**
 * Unit tests for Attachment model.
 * Tests construction, getters/setters, and edge cases.
 * 
 * Attachment API: new Attachment(int id, String name, String url, String type,
 * long size)
 */
public class AttachmentTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("Attachment - Constructor with all parameters", () -> {
            Attachment att = new Attachment(
                    123,
                    "document.pdf",
                    "https://example.com/attachments/123",
                    "application/pdf",
                    12345L);

            SimpleTestRunner.assertTrue(att.id == 123, "ID should be set");
            SimpleTestRunner.assertTrue("document.pdf".equals(att.filename), "Filename should be set");
            SimpleTestRunner.assertTrue("application/pdf".equals(att.contentType), "Content type should be set");
            SimpleTestRunner.assertTrue(att.filesize == 12345L, "Filesize should be set");
            SimpleTestRunner.assertTrue(
                    "https://example.com/attachments/123".equals(att.contentUrl),
                    "Content URL should be set");
        });

        runner.run("Attachment - Field access", () -> {
            Attachment att = new Attachment(456, "image.png", "https://example.com/files/456", "image/png", 98765L);

            SimpleTestRunner.assertTrue(att.id == 456, "ID getter");
            SimpleTestRunner.assertTrue("image.png".equals(att.filename), "Filename getter");
            SimpleTestRunner.assertTrue("image/png".equals(att.contentType), "Content type getter");
            SimpleTestRunner.assertTrue(att.filesize == 98765L, "Filesize getter");
        });

        runner.run("Attachment - Null filename", () -> {
            Attachment att = new Attachment(1, null, "url", "type", 100L);

            SimpleTestRunner.assertNull(att.filename, "Filename can be null");
        });

        runner.run("Attachment - Empty strings", () -> {
            Attachment att = new Attachment(2, "", "", "", 0L);

            SimpleTestRunner.assertTrue("".equals(att.filename), "Empty filename");
            SimpleTestRunner.assertTrue("".equals(att.contentType), "Empty content type");
            SimpleTestRunner.assertTrue("".equals(att.contentUrl), "Empty URL");
        });

        runner.run("Attachment - Large filesize", () -> {
            long largeSize = 1024L * 1024L * 100L; // 100 MB
            Attachment att = new Attachment(3, "large.zip", "url", "application/zip", largeSize);

            SimpleTestRunner.assertTrue(
                    att.filesize == 104857600L,
                    "Should handle large filesize");
        });

        runner.run("Attachment - Special characters in filename", () -> {
            Attachment att = new Attachment(4, "file with spaces.pdf", "url", "application/pdf", 1000L);

            SimpleTestRunner.assertTrue(
                    "file with spaces.pdf".equals(att.filename),
                    "Should handle spaces in filename");
        });

        runner.run("Attachment - toString representation", () -> {
            Attachment att = new Attachment(789, "test.txt", "url", "text/plain", 2048L);

            String str = att.toString();
            SimpleTestRunner.assertNotNull(str, "toString should return value");
            // toString format: "📎 filename (size KB)"
            SimpleTestRunner.assertTrue(
                    str.contains("test.txt"),
                    "toString should contain filename");
            SimpleTestRunner.assertTrue(
                    str.contains("KB"),
                    "toString should show size in KB");
        });

        runner.run("Attachment - Various content types", () -> {
            String[] types = {
                    "application/pdf",
                    "image/png",
                    "text/plain",
                    "application/json",
                    "video/mp4"
            };

            for (int i = 0; i < types.length; i++) {
                Attachment att = new Attachment(i, "file" + i, "url", types[i], 1000L);
                SimpleTestRunner.assertTrue(
                        types[i].equals(att.contentType),
                        "Content type: " + types[i]);
            }
        });
    }
}
