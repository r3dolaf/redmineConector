package redmineconnector.test;

import java.util.ArrayList;
import java.util.List;

import redmineconnector.model.*;
import redmineconnector.service.CachedDataService;
import redmineconnector.service.CacheService;
import redmineconnector.service.DataService;
import redmineconnector.service.SimpleCacheService;

import static redmineconnector.test.SimpleTestRunner.*;

/**
 * Tests for CachedDataService.
 */
public class CachedDataServiceTest {

    public static void runTests(SimpleTestRunner runner) {
        System.out.println("\n=== CachedDataService Tests ===");

        runner.run("testMetadataCaching", () -> {
            try {
                testMetadataCaching();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testCacheHit", () -> {
            try {
                testCacheHit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testCacheMiss", () -> {
            try {
                testCacheMiss();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testTasksNotCached", () -> {
            try {
                testTasksNotCached();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testCacheInvalidationOnCreate", () -> {
            try {
                testCacheInvalidationOnCreate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testVersionsCaching", () -> {
            try {
                testVersionsCaching();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testWikiCaching", () -> {
            try {
                testWikiCaching();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testAllowedStatusesCaching", () -> {
            try {
                testAllowedStatusesCaching();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testMetadataOfflineFallback", () -> {
            try {
                testMetadataOfflineFallback();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void testMetadataCaching() throws Exception {
        CountingDataService countingService = new CountingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(countingService, cache);

        // First call - should hit the service
        cachedService.fetchMetadata("users", "project1");
        assertEquals(1, countingService.metadataCallCount, "Should call service once");

        // Second call - should use cache
        cachedService.fetchMetadata("users", "project1");
        assertEquals(1, countingService.metadataCallCount, "Should not call service again (cached)");

        // Different project - should hit service
        cachedService.fetchMetadata("users", "project2");
        assertEquals(2, countingService.metadataCallCount, "Should call service for different project");
    }

    private static void testCacheHit() throws Exception {
        CountingDataService countingService = new CountingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(countingService, cache);

        // Populate cache
        List<SimpleEntity> result1 = cachedService.fetchMetadata("trackers", "project1");

        // Verify cache hit
        List<SimpleEntity> result2 = cachedService.fetchMetadata("trackers", "project1");

        assertEquals(1, countingService.metadataCallCount, "Should only call service once");
        assertEquals(result1.size(), result2.size(), "Results should be identical");
    }

    private static void testCacheMiss() throws Exception {
        CountingDataService countingService = new CountingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(countingService, cache);

        // First call - cache miss
        cachedService.fetchMetadata("priorities", null);
        assertEquals(1, countingService.metadataCallCount, "Should call service on cache miss");

        // Invalidate cache
        cache.invalidateAll();

        // Second call - cache miss again
        cachedService.fetchMetadata("priorities", null);
        assertEquals(2, countingService.metadataCallCount, "Should call service again after invalidation");
    }

    private static void testTasksNotCached() throws Exception {
        CountingDataService countingService = new CountingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(countingService, cache);

        // Call fetchTasks multiple times
        cachedService.fetchTasks("project1", false, 10);
        cachedService.fetchTasks("project1", false, 10);
        cachedService.fetchTasks("project1", false, 10);

        assertEquals(3, countingService.tasksCallCount, "Tasks should NOT be cached");
    }

    private static void testCacheInvalidationOnCreate() throws Exception {
        CountingDataService countingService = new CountingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(countingService, cache);

        // Populate cache
        cachedService.fetchMetadata("users", "project1");
        assertEquals(1, countingService.metadataCallCount, "Initial call");

        // Create a task (should invalidate cache)
        Task newTask = new Task();
        newTask.subject = "New Task";
        cachedService.createTask("project1", newTask);

        // Fetch again - should hit service (cache invalidated)
        cachedService.fetchMetadata("users", "project1");
        assertEquals(2, countingService.metadataCallCount, "Should call service again after cache invalidation");
    }

    private static void testVersionsCaching() throws Exception {
        CountingDataService countingService = new CountingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(countingService, cache);

        // First call
        cachedService.fetchVersionsFull("project1");
        assertEquals(1, countingService.versionsCallCount, "Should call service once");

        // Second call - should use cache
        cachedService.fetchVersionsFull("project1");
        assertEquals(1, countingService.versionsCallCount, "Should use cache");

        // Create version - should invalidate cache
        cachedService.createVersion("project1", "v1.0", "open", null, null);

        // Third call - should hit service again
        cachedService.fetchVersionsFull("project1");
        assertEquals(2, countingService.versionsCallCount, "Should call service after invalidation");
    }

    private static void testWikiCaching() throws Exception {
        CountingDataService countingService = new CountingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(countingService, cache);

        // Fetch wiki pages
        cachedService.fetchWikiPages("project1");
        assertEquals(1, countingService.wikiPagesCallCount, "Should call service once");

        // Fetch again - should use cache
        cachedService.fetchWikiPages("project1");
        assertEquals(1, countingService.wikiPagesCallCount, "Should use cache");

        // Update wiki page to trigger cache invalidation
        cachedService.createOrUpdateWikiPage("project1", "NewPage", "Content", "Comment");

        // Fetch again - should hit service
        cachedService.fetchWikiPages("project1");
        assertEquals(2, countingService.wikiPagesCallCount, "Should call service after update");
    }

    private static void testAllowedStatusesCaching() throws Exception {
        CountingDataService countingService = new CountingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(countingService, cache);

        // First call
        cachedService.fetchAllowedStatuses("proj1", 1, 0);
        assertEquals(1, countingService.allowedStatusesCallCount, "Should call service once");

        // Second call - should use cache
        cachedService.fetchAllowedStatuses("proj1", 1, 0);
        assertEquals(1, countingService.allowedStatusesCallCount, "Should use cache");

        // Different tracker - should hit service
        cachedService.fetchAllowedStatuses("proj1", 2, 0);
        assertEquals(2, countingService.allowedStatusesCallCount, "Should call service for different tracker");

        // Different issueId - should hit service
        cachedService.fetchAllowedStatuses("proj1", 1, 100);
        assertEquals(3, countingService.allowedStatusesCallCount, "Should call service for different issueId");
    }

    private static void testMetadataOfflineFallback() throws Exception {
        // Custom mock that fails on second call
        class FailingDataService extends CountingDataService {
            boolean shouldFail = false;

            @Override
            public List<SimpleEntity> fetchMetadata(String type, String pid) {
                if (shouldFail) {
                    throw new RuntimeException("Network Failure");
                }
                return super.fetchMetadata(type, pid);
            }
        }

        FailingDataService failingService = new FailingDataService();
        CacheService cache = new SimpleCacheService(false);
        CachedDataService cachedService = new CachedDataService(failingService, cache);

        // 1. Successful call (populates cache)
        List<SimpleEntity> result1 = cachedService.fetchMetadata("users", "p1");
        assertEquals(1, result1.size(), "Should return data");
        assertEquals(1, failingService.metadataCallCount, "Should call service");

        // 2. Set service to fail
        failingService.shouldFail = true;

        // 3. Call again - should succeed using cache
        List<SimpleEntity> result2 = cachedService.fetchMetadata("users", "p1");
        assertEquals(1, result2.size(), "Should return cached data despite failure");

        // 4. Call for new key - should fail (no cache)
        try {
            cachedService.fetchMetadata("trackers", "p1");
            throw new RuntimeException("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Network Failure", e.getMessage(), "Should propagate exception on cache miss");
        }
    }

    // Counting mock service to verify cache behavior
    private static class CountingDataService implements DataService {
        int tasksCallCount = 0;
        int metadataCallCount = 0;
        int versionsCallCount = 0;
        int wikiPagesCallCount = 0;
        int allowedStatusesCallCount = 0;

        @Override
        public List<Task> fetchTasks(String pid, boolean closed, int limit) {
            tasksCallCount++;
            return new ArrayList<>();
        }

        @Override
        public List<SimpleEntity> fetchMetadata(String type, String pid) {
            metadataCallCount++;
            List<SimpleEntity> entities = new ArrayList<>();
            SimpleEntity e = new SimpleEntity(1, "Test " + type);
            entities.add(e);
            return entities;
        }

        @Override
        public Task fetchTaskDetails(int id) {
            return new Task();
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
            return "token";
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
            versionsCallCount++;
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
            wikiPagesCallCount++;
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
        public List<SimpleEntity> fetchAllowedStatuses(String pid, int trackerId, int issueId) throws Exception {
            allowedStatusesCallCount++;
            return new ArrayList<>();
        }

        @Override
        public List<CustomFieldDefinition> fetchCustomFieldDefinitions() throws Exception {
            return new ArrayList<>();
        }

        @Override
        public SimpleEntity fetchProject(String identifier) throws Exception {
            return new SimpleEntity(1, "Mock");
        }

        @Override
        public redmineconnector.model.ContextMetadata fetchContextMetadata(String projectId, int trackerId, int issueId)
                throws Exception {
            return new redmineconnector.model.ContextMetadata(new ArrayList<>(), new ArrayList<>(), null);
        }
    }
}
