package redmineconnector.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import redmineconnector.model.*;
import redmineconnector.util.I18n;
import redmineconnector.service.AsyncDataService;
import redmineconnector.service.DataService;

import static redmineconnector.test.SimpleTestRunner.*;

/**
 * Tests for AsyncDataService.
 */
public class AsyncDataServiceTest {

    public static void runTests(SimpleTestRunner runner) {
        System.out.println("\n=== AsyncDataService Tests ===");

        runner.run("testFetchTasksAsync_Success", () -> {
            try {
                testFetchTasksAsync_Success();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testFetchTasksAsync_Failure", () -> {
            try {
                testFetchTasksAsync_Failure();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testFetchMetadataAsync_Success", () -> {
            try {
                testFetchMetadataAsync_Success();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testMultipleConcurrentRequests", () -> {
            try {
                testMultipleConcurrentRequests();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testExceptionHandling", () -> {
            try {
                testExceptionHandling();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void testFetchTasksAsync_Success() throws Exception {
        // Mock DataService that returns test data
        DataService mockService = new MockDataService();
        AsyncDataService asyncService = new AsyncDataService(mockService);

        CompletableFuture<List<Task>> future = asyncService.fetchTasksAsync("test", false, 10);
        List<Task> tasks = future.get(5, TimeUnit.SECONDS);

        assertNotNull(tasks, "Tasks should not be null");
        assertEquals(2, tasks.size(), "Should return 2 tasks");
        assertEquals("Task 1", tasks.get(0).subject, "First task subject");
    }

    private static void testFetchTasksAsync_Failure() throws Exception {
        DataService failingService = new FailingDataService();
        AsyncDataService asyncService = new AsyncDataService(failingService);

        CompletableFuture<List<Task>> future = asyncService.fetchTasksAsync("test", false, 10);

        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Should have thrown exception");
        } catch (ExecutionException e) {
            // Expected - verify it's wrapped correctly
            assertTrue(e.getCause() instanceof RuntimeException, "Should wrap in RuntimeException");
        }
    }

    private static void testFetchMetadataAsync_Success() throws Exception {
        DataService mockService = new MockDataService();
        AsyncDataService asyncService = new AsyncDataService(mockService);

        CompletableFuture<List<SimpleEntity>> future = asyncService.fetchMetadataAsync("users", "test");
        List<SimpleEntity> users = future.get(5, TimeUnit.SECONDS);

        assertNotNull(users, "Users should not be null");
        assertEquals(1, users.size(), "Should return 1 user");
    }

    private static void testMultipleConcurrentRequests() throws Exception {
        DataService mockService = new MockDataService();
        AsyncDataService asyncService = new AsyncDataService(mockService);

        // Launch 5 concurrent requests
        CompletableFuture<List<Task>>[] futures = new CompletableFuture[5];
        for (int i = 0; i < 5; i++) {
            futures[i] = asyncService.fetchTasksAsync("test", false, 10);
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        // Verify all succeeded
        for (CompletableFuture<List<Task>> future : futures) {
            List<Task> tasks = future.get();
            assertEquals(2, tasks.size(), "Each request should return 2 tasks");
        }
    }

    private static void testExceptionHandling() throws Exception {
        DataService failingService = new FailingDataService();
        AsyncDataService asyncService = new AsyncDataService(failingService);

        CompletableFuture<Task> future = asyncService.fetchTaskDetailsAsync(123);

        boolean exceptionCaught = false;
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            exceptionCaught = true;
            assertTrue(
                    e.getCause().getMessage()
                            .contains(I18n.format("async.error.task_details", 123).replace(": 123", "")),
                    "Exception message should be descriptive");
        }

        assertTrue(exceptionCaught, "Should have caught exception");
    }

    // Mock implementations

    private static class MockDataService implements DataService {
        @Override
        public List<Task> fetchTasks(String pid, boolean closed, int limit) {
            List<Task> tasks = new ArrayList<>();
            Task t1 = new Task();
            t1.id = 1;
            t1.subject = "Task 1";
            Task t2 = new Task();
            t2.id = 2;
            t2.subject = "Task 2";
            tasks.add(t1);
            tasks.add(t2);
            return tasks;
        }

        @Override
        public List<SimpleEntity> fetchMetadata(String type, String pid) {
            List<SimpleEntity> entities = new ArrayList<>();
            SimpleEntity e = new SimpleEntity(1, "Test User");
            entities.add(e);
            return entities;
        }

        @Override
        public Task fetchTaskDetails(int id) {
            Task t = new Task();
            t.id = id;
            t.subject = "Detailed Task";
            return t;
        }

        @Override
        public int createTask(String pid, Task task) {
            return 123;
        }

        @Override
        public void updateTask(Task task) {
        }

        @Override
        public String uploadFile(byte[] data, String contentType) {
            return "token123";
        }

        @Override
        public byte[] downloadAttachment(Attachment att) {
            return new byte[0];
        }

        @Override
        public void logTime(int issueId, String date, double hours, int userId, int activityId, String comment) {
        }

        @Override
        public List<TimeEntry> fetchTimeEntries(String pid, String dateFrom, String dateTo) {
            return new ArrayList<>();
        }

        @Override
        public List<VersionDTO> fetchVersionsFull(String pid) {
            return new ArrayList<>();
        }

        @Override
        public void createVersion(String pid, String name, String status, String startDate, String dueDate) {
        }

        @Override
        public void updateVersion(int id, String name, String status, String startDate, String dueDate) {
        }

        @Override
        public void deleteVersion(int id) {
        }

        @Override
        public List<Task> fetchTasksByVersion(String pid, int versionId) {
            return new ArrayList<>();
        }

        @Override
        public List<Task> fetchClosedTasks(String pid, String dateFrom, String dateTo) {
            return new ArrayList<>();
        }

        @Override
        public List<WikiPageDTO> fetchWikiPages(String projectId) {
            return new ArrayList<>();
        }

        @Override
        public WikiPageDTO fetchWikiPageContent(String projectId, String pageTitle) {
            return new WikiPageDTO("Test Page");
        }

        @Override
        public void createOrUpdateWikiPage(String projectId, String pageTitle, String content, String comment) {
        }

        @Override
        public void deleteWikiPage(String projectId, String pageTitle) {
        }

        @Override
        public List<WikiVersionDTO> fetchWikiHistory(String projectId, String pageTitle) {
            return new ArrayList<>();
        }

        @Override
        public void revertWikiPage(String projectId, String pageTitle, int version) {
        }

        @Override
        public void uploadWikiAttachment(String projectId, String pageTitle, String token, String filename,
                String contentType, String currentText, int version) {
        }

        @Override
        public SimpleEntity fetchCurrentUser() {
            return new SimpleEntity(1, "Test User");
        }

        @Override
        public List<Task> fetchTasksByIds(List<Integer> ids) {
            return new ArrayList<>();
        }

        @Override
        public List<SimpleEntity> fetchAllowedStatuses(String pid, int trackerId, int issueId) {
            return new ArrayList<>();
        }

        @Override
        public List<CustomFieldDefinition> fetchCustomFieldDefinitions() {
            return new ArrayList<>();
        }

        @Override
        public SimpleEntity fetchProject(String identifier) throws Exception {
            return new SimpleEntity(1, "Mock Project");
        }

        @Override
        public redmineconnector.model.ContextMetadata fetchContextMetadata(String projectId, int trackerId, int issueId)
                throws Exception {
            return new redmineconnector.model.ContextMetadata(new ArrayList<>(), new ArrayList<>(), null);
        }
    }

    private static class FailingDataService implements DataService {
        @Override
        public void uploadWikiAttachment(String projectId, String pageTitle, String token, String filename,
                String contentType, String currentText, int version) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<Task> fetchTasks(String pid, boolean closed, int limit) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<SimpleEntity> fetchMetadata(String type, String pid) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public Task fetchTaskDetails(int id) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public int createTask(String pid, Task task) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public void updateTask(Task task) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public String uploadFile(byte[] data, String contentType) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public byte[] downloadAttachment(Attachment att) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public void logTime(int issueId, String date, double hours, int userId, int activityId, String comment)
                throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<TimeEntry> fetchTimeEntries(String pid, String dateFrom, String dateTo) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<VersionDTO> fetchVersionsFull(String pid) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public void createVersion(String pid, String name, String status, String startDate, String dueDate)
                throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public void updateVersion(int id, String name, String status, String startDate, String dueDate)
                throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public void deleteVersion(int id) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<Task> fetchTasksByVersion(String pid, int versionId) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<Task> fetchClosedTasks(String pid, String dateFrom, String dateTo) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<WikiPageDTO> fetchWikiPages(String projectId) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public WikiPageDTO fetchWikiPageContent(String projectId, String pageTitle) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public void createOrUpdateWikiPage(String projectId, String pageTitle, String content, String comment)
                throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public void deleteWikiPage(String projectId, String pageTitle) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<WikiVersionDTO> fetchWikiHistory(String projectId, String pageTitle) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public void revertWikiPage(String projectId, String pageTitle, int version) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public SimpleEntity fetchCurrentUser() throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<Task> fetchTasksByIds(List<Integer> ids) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<SimpleEntity> fetchAllowedStatuses(String pid, int trackerId, int issueId) throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public List<CustomFieldDefinition> fetchCustomFieldDefinitions() throws Exception {
            throw new Exception("Simulated failure");
        }

        @Override
        public SimpleEntity fetchProject(String identifier) throws Exception {
            throw new Exception("FAIL");
        }

        @Override
        public redmineconnector.model.ContextMetadata fetchContextMetadata(String projectId, int trackerId, int issueId)
                throws Exception {
            throw new Exception("FAIL");
        }
    }
}
