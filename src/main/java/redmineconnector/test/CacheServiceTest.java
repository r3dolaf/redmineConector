package redmineconnector.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import redmineconnector.model.SimpleEntity;
import redmineconnector.service.CacheService;
import redmineconnector.service.SimpleCacheService;

import static redmineconnector.test.SimpleTestRunner.*;

/**
 * Tests for CacheService implementations.
 */
public class CacheServiceTest {

    public static void runTests(SimpleTestRunner runner) {
        System.out.println("\n=== CacheService Tests ===");

        runner.run("testPutAndGet", CacheServiceTest::testPutAndGet);
        runner.run("testExpiration", () -> {
            try {
                testExpiration();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        runner.run("testInvalidate", CacheServiceTest::testInvalidate);
        runner.run("testInvalidatePattern", CacheServiceTest::testInvalidatePattern);
        runner.run("testInvalidateAll", CacheServiceTest::testInvalidateAll);
        runner.run("testContains", CacheServiceTest::testContains);
        runner.run("testSize", CacheServiceTest::testSize);
        runner.run("testNoExpiration", () -> {
            try {
                testNoExpiration();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void testPutAndGet() {
        CacheService cache = new SimpleCacheService(false);

        List<String> data = new ArrayList<>();
        data.add("item1");
        data.add("item2");

        cache.put("test:key", data, 60);

        Optional<List<String>> result = cache.get("test:key");
        assertTrue(result.isPresent(), "Cache should contain the key");
        assertEquals(2, result.get().size(), "Should return the same list");
        assertEquals("item1", result.get().get(0), "First item should match");
    }

    private static void testExpiration() throws InterruptedException {
        CacheService cache = new SimpleCacheService(false);

        cache.put("expiring:key", "value", 1); // 1 second TTL

        // Should be present immediately
        assertTrue(cache.contains("expiring:key"), "Should be present immediately");

        // Wait for expiration
        Thread.sleep(1100);

        // Should be expired now
        Optional<String> result = cache.get("expiring:key");
        assertTrue(!result.isPresent(), "Should be expired after TTL");
    }

    private static void testInvalidate() {
        CacheService cache = new SimpleCacheService(false);

        cache.put("key1", "value1", 60);
        cache.put("key2", "value2", 60);

        assertTrue(cache.contains("key1"), "key1 should exist");
        assertTrue(cache.contains("key2"), "key2 should exist");

        cache.invalidate("key1");

        assertTrue(!cache.contains("key1"), "key1 should be invalidated");
        assertTrue(cache.contains("key2"), "key2 should still exist");
    }

    private static void testInvalidatePattern() {
        CacheService cache = new SimpleCacheService(false);

        cache.put("users:project1", "data1", 60);
        cache.put("users:project2", "data2", 60);
        cache.put("trackers:project1", "data3", 60);

        cache.invalidatePattern("users:*");

        assertTrue(!cache.contains("users:project1"), "users:project1 should be invalidated");
        assertTrue(!cache.contains("users:project2"), "users:project2 should be invalidated");
        assertTrue(cache.contains("trackers:project1"), "trackers:project1 should still exist");
    }

    private static void testInvalidateAll() {
        CacheService cache = new SimpleCacheService(false);

        cache.put("key1", "value1", 60);
        cache.put("key2", "value2", 60);
        cache.put("key3", "value3", 60);

        assertEquals(3, cache.size(), "Should have 3 entries");

        cache.invalidateAll();

        assertEquals(0, cache.size(), "Should have 0 entries after invalidateAll");
    }

    private static void testContains() {
        CacheService cache = new SimpleCacheService(false);

        assertTrue(!cache.contains("nonexistent"), "Should not contain nonexistent key");

        cache.put("existing", "value", 60);

        assertTrue(cache.contains("existing"), "Should contain existing key");
    }

    private static void testSize() {
        CacheService cache = new SimpleCacheService(false);

        assertEquals(0, cache.size(), "Initial size should be 0");

        cache.put("key1", "value1", 60);
        assertEquals(1, cache.size(), "Size should be 1");

        cache.put("key2", "value2", 60);
        assertEquals(2, cache.size(), "Size should be 2");

        cache.invalidate("key1");
        assertEquals(1, cache.size(), "Size should be 1 after invalidation");
    }

    private static void testNoExpiration() throws InterruptedException {
        CacheService cache = new SimpleCacheService(false);

        cache.put("permanent", "value", 0); // 0 = no expiration

        Thread.sleep(100); // Wait a bit

        assertTrue(cache.contains("permanent"), "Should still be present (no expiration)");
    }
}
