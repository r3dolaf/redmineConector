package redmineconnector.test;

import java.util.List;

import redmineconnector.model.Task;
import redmineconnector.model.WikiPageDTO;
import redmineconnector.util.JsonParser;

public class JsonParserTest {

    public static void runTests(SimpleTestRunner runner) {
        runner.run("JsonParser.extractId", () -> {
            String json = "{\"issue\":{\"id\":123, \"subject\":\"Test\"}}";
            int id = JsonParser.extractId(json);
            SimpleTestRunner.assertEquals(123, id, "ID extraction failed");
        });

        runner.run("JsonParser.parseIssues (empty)", () -> {
            String json = "{\"issues\":[], \"total_count\": 0}";
            List<Task> tasks = JsonParser.parseIssues(json);
            SimpleTestRunner.assertTrue(tasks.isEmpty(), "Should be empty list");
        });

        runner.run("JsonParser.parseIssues (single)", () -> {
            String json = "{\"issues\":[{\"id\":10, \"subject\":\"Bug\", \"status\":{\"id\":1, \"name\":\"New\"}}]}";
            List<Task> tasks = JsonParser.parseIssues(json);
            SimpleTestRunner.assertEquals(1, tasks.size(), "List size mismatch");
            SimpleTestRunner.assertEquals(10, tasks.get(0).id, "Task ID mismatch");
            SimpleTestRunner.assertEquals("Bug", tasks.get(0).subject, "Task subject mismatch");
            SimpleTestRunner.assertEquals("New", tasks.get(0).status, "Task status mismatch");
        });

        runner.run("JsonParser.serializeTaskForCreate", () -> {
            Task t = new Task();
            t.subject = "New Feature";
            t.description = "Desc";
            t.priorityId = 2;
            String json = JsonParser.serializeTaskForCreate("1", t);
            SimpleTestRunner.assertTrue(json.contains("\"project_id\":\"1\""), "Missing project_id");
            SimpleTestRunner.assertTrue(json.contains("\"subject\":\"New Feature\""), "Missing subject");
            SimpleTestRunner.assertTrue(json.contains("\"priority_id\":2"), "Missing priority");
        });

        runner.run("JsonParser.parseWikiPagesIndex", () -> {
            String json = "{\"wiki_pages\":[{\"title\":\"Home\"}, {\"title\":\"Setup\"}]}";
            List<WikiPageDTO> pages = JsonParser.parseWikiPagesIndex(json);
            SimpleTestRunner.assertEquals(2, pages.size(), "Wiki pages size mismatch");
            SimpleTestRunner.assertEquals("Home", pages.get(0).title, "First page title mismatch");
        });

        runner.run("JsonParser.parseIssues (nested id conflict)", () -> {
            // Nested object has an ID that appears BEFORE the task ID
            String json = "{\"issues\":[{\"custom_fields\":[{\"id\":999, \"name\":\"Field\"}], \"id\":1, \"subject\":\"Real\"}]}";
            List<Task> tasks = JsonParser.parseIssues(json);
            SimpleTestRunner.assertEquals(1, tasks.size(), "List size mismatch");
            SimpleTestRunner.assertEquals(1, tasks.get(0).id, "Incorrectly parsed nested ID as issue ID");
        });

        runner.run("JsonParser.get (escaped characters)", () -> {
            String json = "{\"text\": \"Line1\\nLine2\\tTab\\\"Quote\\\"\"}";
            String val = JsonParser.get(json, "text");
            SimpleTestRunner.assertEquals("Line1\nLine2\tTab\"Quote\"", val, "Escaped characters failed");
        });

        runner.run("JsonParser.unexpectedStructure", () -> {
            // Empty object in array
            String json = "{\"issues\":[{}, {\"id\":5}]}";
            List<Task> tasks = JsonParser.parseIssues(json);
            SimpleTestRunner.assertEquals(1, tasks.size(), "Should skip empty/invalid objects");
            SimpleTestRunner.assertEquals(5, tasks.get(0).id, "Should parse valid object");
        });
    }
}
