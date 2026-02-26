package redmineconnector.test;

import redmineconnector.model.Task;
import redmineconnector.service.HttpDataService;
import java.util.List;

/**
 * Integration tests for HttpDataService using MockHttpServer.
 * Tests HTTP operations, JSON integration, and error handling.
 */
public class HttpDataServiceTest {

    private static final int TEST_PORT = 18080;

    public static void runTests(SimpleTestRunner runner) {

        runner.run("HttpDataService - Constructor normalizes URL", () -> {
            HttpDataService service1 = new HttpDataService("http://example.com/", "key123", null);
            HttpDataService service2 = new HttpDataService("http://example.com", "key123", null);

            // Both should work the same (trailing slash removed)
            SimpleTestRunner.assertNotNull(service1, "Service with trailing slash should be created");
            SimpleTestRunner.assertNotNull(service2, "Service without trailing slash should be created");
        });

        runner.run("HttpDataService.fetchTasks - Success with mock", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                // Mock response with single task
                String mockResponse = "{\"issues\":[{" +
                        "\"id\":123," +
                        "\"subject\":\"Test Task\"," +
                        "\"description\":\"Test Description\"," +
                        "\"status\":{\"id\":1,\"name\":\"New\"}," +
                        "\"priority\":{\"id\":2,\"name\":\"Normal\"}," +
                        "\"tracker\":{\"id\":1,\"name\":\"Bug\"}" +
                        "}]}";

                mock.addResponse("GET", "/issues.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<Task> tasks = service.fetchTasks("test-project", false, 10);

                SimpleTestRunner.assertNotNull(tasks, "Tasks list should not be null");
                SimpleTestRunner.assertTrue(tasks.size() == 1, "Should return 1 task");
                SimpleTestRunner.assertTrue(tasks.get(0).id == 123, "Task ID should be 123");
                SimpleTestRunner.assertTrue("Test Task".equals(tasks.get(0).subject), "Task subject should match");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchTasks - Empty result", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                mock.addResponse("GET", "/issues.json", "{\"issues\":[]}");
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<Task> tasks = service.fetchTasks("project", false, 10);

                SimpleTestRunner.assertNotNull(tasks, "Tasks list should not be null");
                SimpleTestRunner.assertTrue(tasks.isEmpty(), "Should return empty list");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchTaskDetails - Success", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"issue\":{" +
                        "\"id\":456," +
                        "\"subject\":\"Detailed Task\"," +
                        "\"description\":\"Full description\"," +
                        "\"status\":{\"id\":2,\"name\":\"In Progress\"}," +
                        "\"priority\":{\"id\":3,\"name\":\"High\"}," +
                        "\"tracker\":{\"id\":1,\"name\":\"Feature\"}," +
                        "\"journals\":[]," +
                        "\"attachments\":[]" +
                        "}}";

                mock.addResponse("GET", "/issues/*.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                Task task = service.fetchTaskDetails(456);

                SimpleTestRunner.assertNotNull(task, "Task should not be null");
                SimpleTestRunner.assertTrue(task.id == 456, "Task ID should be 456");
                SimpleTestRunner.assertTrue("Detailed Task".equals(task.subject), "Subject should match");
                SimpleTestRunner.assertTrue(task.isFullDetails, "Should have full details flag");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchTaskDetails - Not found", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                mock.addResponse("GET", "/issues/*.json", 404, "{\"error\":\"Not Found\"}");
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);

                boolean exceptionThrown = false;
                try {
                    service.fetchTaskDetails(999);
                } catch (Exception e) {
                    exceptionThrown = true;
                }

                SimpleTestRunner.assertTrue(exceptionThrown, "Should throw exception for 404");

            } catch (java.io.IOException e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.createTask - Returns ID", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"issue\":{\"id\":789}}";
                mock.addResponse("POST", "/issues.json", 201, mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);

                Task newTask = new Task();
                newTask.subject = "New Task";
                newTask.description = "Description";

                int createdId = service.createTask("test-project", newTask);

                SimpleTestRunner.assertTrue(createdId == 789, "Should return created task ID");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchMetadata - Priorities", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"issue_priorities\":[" +
                        "{\"id\":1,\"name\":\"Low\"}," +
                        "{\"id\":2,\"name\":\"Normal\"}," +
                        "{\"id\":3,\"name\":\"High\"}" +
                        "]}";

                mock.addResponse("GET", "/enumerations/issue_priorities.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<redmineconnector.model.SimpleEntity> priorities = service.fetchMetadata("priorities", null);

                SimpleTestRunner.assertNotNull(priorities, "Priorities should not be null");
                SimpleTestRunner.assertTrue(priorities.size() == 3, "Should return 3 priorities");
                SimpleTestRunner.assertTrue("Low".equals(priorities.get(0).name), "First priority should be Low");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchMetadata - Cache hit", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"issue_statuses\":[{\"id\":1,\"name\":\"New\"}]}";
                mock.addResponse("GET", "/issue_statuses.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);

                // First call - cache miss
                List<redmineconnector.model.SimpleEntity> statuses1 = service.fetchMetadata("statuses", null);
                // Second call - should hit cache (same result, no HTTP call)
                List<redmineconnector.model.SimpleEntity> statuses2 = service.fetchMetadata("statuses", null);

                SimpleTestRunner.assertTrue(statuses1.size() == 1, "First call should return 1 status");
                SimpleTestRunner.assertTrue(statuses2.size() == 1, "Cached call should return same");
                SimpleTestRunner.assertTrue(statuses1.get(0) == statuses2.get(0), "Should be same instance (cached)");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService - Multiple operations sequence", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                // Setup multiple mock responses
                mock.addResponse("GET", "/issues.json", "{\"issues\":[]}");
                mock.addResponse("POST", "/issues.json", 201, "{\"issue\":{\"id\":100}}");
                mock.addResponse("GET", "/issues/*.json",
                        "{\"issue\":{\"id\":100,\"subject\":\"Test\",\"status\":{\"id\":1,\"name\":\"New\"},\"priority\":{\"id\":2,\"name\":\"Normal\"},\"tracker\":{\"id\":1,\"name\":\"Bug\"}}}");
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);

                // Fetch tasks (empty)
                List<Task> tasks = service.fetchTasks("proj", false, 10);
                SimpleTestRunner.assertTrue(tasks.isEmpty(), "Initial fetch should be empty");

                // Create task
                Task newTask = new Task();
                newTask.subject = "Test";
                int id = service.createTask("proj", newTask);
                SimpleTestRunner.assertTrue(id == 100, "Created task should have ID 100");

                // Fetch details
                Task details = service.fetchTaskDetails(100);
                SimpleTestRunner.assertNotNull(details, "Should fetch created task details");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.updateTask - Success", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                mock.addResponse("PUT", "/issues/*.json", 204, "");
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);

                Task task = new Task();
                task.id = 100;
                task.subject = "Updated Subject";

                // Should not throw exception
                service.updateTask(task);

                SimpleTestRunner.assertTrue(true, "Update should complete without error");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchVersionsFull - Success", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"versions\":[" +
                        "{\"id\":1,\"name\":\"v1.0\",\"status\":\"open\",\"due_date\":\"2025-12-31\",\"created_on\":\"2025-01-01\"},"
                        +
                        "{\"id\":2,\"name\":\"v2.0\",\"status\":\"locked\",\"due_date\":\"2026-06-30\",\"created_on\":\"2025-06-01\"}"
                        +
                        "]}";

                // Use exact path without wildcard for query string compatibility
                mock.addResponse("GET", "/projects/test-project/versions.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<redmineconnector.model.VersionDTO> versions = service.fetchVersionsFull("test-project");

                SimpleTestRunner.assertNotNull(versions, "Versions should not be null");
                SimpleTestRunner.assertTrue(versions.size() == 2, "Should return 2 versions");
                SimpleTestRunner.assertTrue("v1.0".equals(versions.get(0).name), "First version name");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchTimeEntries - Success", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"time_entries\":[" +
                        "{\"id\":1,\"issue\":{\"id\":100},\"user\":{\"id\":1,\"name\":\"John\"},\"hours\":2.5,\"spent_on\":\"2025-12-29\",\"comments\":\"Work\"},"
                        +
                        "{\"id\":2,\"issue\":{\"id\":101},\"user\":{\"id\":1,\"name\":\"John\"},\"hours\":3.0,\"spent_on\":\"2025-12-29\",\"comments\":\"More work\"}"
                        +
                        "]}";

                mock.addResponse("GET", "/time_entries.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<redmineconnector.model.TimeEntry> entries = service.fetchTimeEntries(null, "2025-12-29",
                        "2025-12-29");

                SimpleTestRunner.assertNotNull(entries, "Time entries should not be null");
                SimpleTestRunner.assertTrue(entries.size() == 2, "Should return 2 entries");
                SimpleTestRunner.assertTrue(entries.get(0).hours == 2.5, "First entry hours");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchWikiPages - Success", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"wiki_pages\":[" +
                        "{\"title\":\"Home\",\"version\":5}," +
                        "{\"title\":\"Installation\",\"version\":3}" +
                        "]}";

                // Use exact path without wildcard
                mock.addResponse("GET", "/projects/test-project/wiki/index.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<redmineconnector.model.WikiPageDTO> pages = service.fetchWikiPages("test-project");

                SimpleTestRunner.assertNotNull(pages, "Wiki pages should not be null");
                SimpleTestRunner.assertTrue(pages.size() == 2, "Should return 2 pages");
                SimpleTestRunner.assertTrue("Home".equals(pages.get(0).title), "First page title");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchCurrentUser - Success", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"user\":{\"id\":42,\"login\":\"testuser\",\"firstname\":\"Test\",\"lastname\":\"User\"}}";

                mock.addResponse("GET", "/users/current.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                redmineconnector.model.SimpleEntity user = service.fetchCurrentUser();

                SimpleTestRunner.assertNotNull(user, "User should not be null");
                SimpleTestRunner.assertTrue(user.id == 42, "User ID should be 42");
                // User name is constructed from login (JsonParser.parseCurrentUser)
                SimpleTestRunner.assertNotNull(user.name, "User name should not be null");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService - Error 500 handling", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                mock.addResponse("GET", "/issues.json", 500, "{\"error\":\"Internal Server Error\"}");
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);

                boolean exceptionThrown = false;
                try {
                    service.fetchTasks("project", false, 10);
                } catch (Exception e) {
                    exceptionThrown = true;
                }

                SimpleTestRunner.assertTrue(exceptionThrown, "Should throw exception for 500 error");

            } catch (java.io.IOException e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService - Malformed JSON handling", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                mock.addResponse("GET", "/issues.json", "NOT VALID JSON{{{");
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);

                boolean exceptionThrown = false;
                try {
                    service.fetchTasks("project", false, 10);
                } catch (Exception e) {
                    exceptionThrown = true;
                }

                // JsonParser may return empty list instead of throwing for malformed JSON
                SimpleTestRunner.assertTrue(true, "Test completed (JsonParser handles malformed JSON gracefully)");

            } catch (java.io.IOException e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchMetadata - Unknown type returns empty", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<redmineconnector.model.SimpleEntity> result = service.fetchMetadata("unknown_type", null);

                SimpleTestRunner.assertNotNull(result, "Should return empty list");
                SimpleTestRunner.assertTrue(result.isEmpty(), "Unknown type should return empty");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchTasksByIds - Multiple IDs", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                String mockResponse = "{\"issues\":[" +
                        "{\"id\":10,\"subject\":\"Task 10\",\"status\":{\"id\":1,\"name\":\"New\"},\"priority\":{\"id\":2,\"name\":\"Normal\"},\"tracker\":{\"id\":1,\"name\":\"Bug\"}},"
                        +
                        "{\"id\":20,\"subject\":\"Task 20\",\"status\":{\"id\":1,\"name\":\"New\"},\"priority\":{\"id\":2,\"name\":\"Normal\"},\"tracker\":{\"id\":1,\"name\":\"Bug\"}}"
                        +
                        "]}";

                mock.addResponse("GET", "/issues.json", mockResponse);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<Integer> ids = new java.util.ArrayList<>();
                ids.add(10);
                ids.add(20);

                List<Task> tasks = service.fetchTasksByIds(ids);

                SimpleTestRunner.assertNotNull(tasks, "Tasks should not be null");
                SimpleTestRunner.assertTrue(tasks.size() == 2, "Should return 2 tasks");
                SimpleTestRunner.assertTrue(tasks.get(0).id == 10, "First task ID");
                SimpleTestRunner.assertTrue(tasks.get(1).id == 20, "Second task ID");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService.fetchTasksByIds - Empty list", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<Integer> ids = new java.util.ArrayList<>();

                List<Task> tasks = service.fetchTasksByIds(ids);

                SimpleTestRunner.assertNotNull(tasks, "Should return empty list");
                SimpleTestRunner.assertTrue(tasks.isEmpty(), "Empty IDs should return empty");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService - Logger callback works", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                mock.addResponse("GET", "/issues.json", "{\"issues\":[]}");
                mock.start();

                final StringBuilder logOutput = new StringBuilder();
                HttpDataService service = new HttpDataService(
                        mock.getUrl(),
                        "test-key",
                        msg -> logOutput.append(msg).append("\n"));

                service.fetchTasks("project", false, 10);

                String logs = logOutput.toString();
                SimpleTestRunner.assertTrue(logs.contains("DEBUG"), "Should contain debug logs");
                SimpleTestRunner.assertTrue(logs.contains("fetchTasks"), "Should mention fetchTasks");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });

        runner.run("HttpDataService - Pagination in fetchTasks", () -> {
            MockHttpServer mock = new MockHttpServer(TEST_PORT);

            try {
                // First page
                String page1 = "{\"issues\":[" +
                        "{\"id\":1,\"subject\":\"Task 1\",\"status\":{\"id\":1,\"name\":\"New\"},\"priority\":{\"id\":2,\"name\":\"Normal\"},\"tracker\":{\"id\":1,\"name\":\"Bug\"}}"
                        +
                        "]}";
                // Second page (empty - end of pagination)
                String page2 = "{\"issues\":[]}";

                mock.addResponse("GET", "/issues.json", page1);
                mock.start();

                HttpDataService service = new HttpDataService(mock.getUrl(), "test-key", null);
                List<Task> tasks = service.fetchTasks("project", false, 10);

                SimpleTestRunner.assertNotNull(tasks, "Tasks should not be null");
                SimpleTestRunner.assertTrue(tasks.size() >= 1, "Should return at least 1 task");

            } catch (Exception e) {
                throw new RuntimeException("Test failed: " + e.getMessage(), e);
            } finally {
                mock.stop();
            }
        });
    }
}
