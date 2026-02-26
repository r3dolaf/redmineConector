package redmineconnector.test;

import java.util.List;

import redmineconnector.model.CustomField;
import redmineconnector.model.Task;
import redmineconnector.util.JsonParser;

public class CustomFieldsTest {

    public static void main(String[] args) {
        testParseCustomFields();
        testSerializeCustomFields();
        testParseCustomFieldDefinitions();
    }

    private static void testParseCustomFields() {
        System.out.println("Testing Parse Custom Fields...");

        // Since parseTask takes a map, we simulate the internal call or use a public
        // wrapper if available.
        // JsonParser.parseTaskMap is private, but JsonParser.parseTasks(json) uses it.
        // Let's simulate a list response.
        String listJson = "{\"issues\": [" +
                "{" +
                "\"id\": 1," +
                "\"subject\": \"Test Task\"," +
                "\"custom_fields\": [" +
                "  {\"id\": 10, \"name\": \"Field One\", \"value\": \"Value 1\"}," +
                "  {\"id\": 11, \"name\": \"Field Two\", \"value\": \"Value 2\"}" +
                "]" +
                "}" +
                "]}";

        // We need to use JsonParser.parseTasks(json)
        try {
            // Re-construct proper method call if possible, or simulate logic
            // JsonParser.parseTasks is not static? It is static.
            // Let's verify method signature.
        } catch (Exception e) {
        }

        // Actually, let's just use the logic we added to JsonParser directly via a mock
        // if needed,
        // or better, invoke the public method that calls it.
        // JsonParser doesn't have a public parseSingleTask method usually, only
        // parseTasks (list).

        List<Task> parsedTasks = redmineconnector.util.JsonParser.parseIssues(listJson);
        if (parsedTasks.isEmpty()) {
            System.err.println("FAILED: No tasks parsed");
            return;
        }

        Task t = parsedTasks.get(0);
        if (t.customFields == null || t.customFields.size() != 2) {
            System.err.println("FAILED: Expected 2 custom fields, got "
                    + (t.customFields == null ? "null" : t.customFields.size()));
            return;
        }

        CustomField cf1 = t.customFields.get(0);
        if (cf1.id != 10 || !"Field One".equals(cf1.name) || !"Value 1".equals(cf1.value)) {
            System.err.println("FAILED: CF1 mismatch: " + cf1);
        } else {
            System.out.println("PASSED: CF1 matches");
        }

        CustomField cf2 = t.customFields.get(1);
        if (cf2.id != 11 || !"Field Two".equals(cf2.name) || !"Value 2".equals(cf2.value)) {
            System.err.println("FAILED: CF2 mismatch: " + cf2);
        } else {
            System.out.println("PASSED: CF2 matches");
        }
    }

    private static void testSerializeCustomFields() {
        System.out.println("Testing Serialize Custom Fields...");
        Task t = new Task();
        t.subject = "New Task with CF";
        t.description = "Desc";
        t.customFields.add(new CustomField(5, "MyField", "MyValue"));
        t.customFields.add(new CustomField(6, "OtherField", "OtherValue"));

        String json = JsonParser.serializeTaskForCreate("1", t);
        System.out.println("Serialized JSON: " + json);

        if (!json.contains("\"custom_fields\":[")) {
            System.err.println("FAILED: JSON does not contain custom_fields array");
            return;
        }

        if (!json.contains("{\"id\":5,\"value\":\"MyValue\"}")) {
            System.err.println("FAILED: JSON does not contain CF 5");
            return;
        }

        if (!json.contains("{\"id\":6,\"value\":\"OtherValue\"}")) {
            System.err.println("FAILED: JSON does not contain CF 6");
            return;
        }

        System.out.println("PASSED: Serialization looks correct");
    }

    private static void testParseCustomFieldDefinitions() {
        System.out.println("Testing Parse Custom Field Definitions...");
        String json = "{\"custom_fields\": [" +
                "  {\"id\": 1, \"name\": \"CField 1\", \"field_format\": \"string\", \"is_required\": true}," +
                "  {\"id\": 2, \"name\": \"CField 2\", \"field_format\": \"list\", \"possible_values\": [{\"value\":\"A\"}, {\"value\":\"B\"}],"
                +
                "   \"is_required\": false}" +
                "]}";

        List<redmineconnector.model.CustomFieldDefinition> defs = redmineconnector.util.JsonParser
                .parseCustomFieldDefinitions(json);
        if (defs == null || defs.size() != 2) {
            System.err.println("FAILED: Expected 2 definitions, got " + (defs == null ? "null" : defs.size()));
            return;
        }

        redmineconnector.model.CustomFieldDefinition d1 = defs.get(0);
        if (d1.id != 1 || !"CField 1".equals(d1.name) || !d1.isRequired) {
            System.err.println("FAILED: D1 mismatch: " + d1.name + ", req=" + d1.isRequired);
        } else {
            System.out.println("PASSED: D1 matches");
        }

        redmineconnector.model.CustomFieldDefinition d2 = defs.get(1);
        if (d2.id != 2 || !"list".equals(d2.type) || d2.possibleValues.size() != 2) {
            System.err.println(
                    "FAILED: D2 mismatch: " + d2.name + ", type=" + d2.type + ", vals=" + d2.possibleValues.size());
        } else {
            System.out.println("PASSED: D2 matches");
        }
    }
}
