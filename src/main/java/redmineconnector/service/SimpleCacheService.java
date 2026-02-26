package redmineconnector.service;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory implementation of CacheService using ConcurrentHashMap.
 * Thread-safe and supports automatic expiration of entries based on TTL.
 * 
 * <p>
 * Features:
 * <ul>
 * <li>Thread-safe concurrent access</li>
 * <li>Automatic expiration based on TTL</li>
 * <li>Periodic cleanup of expired entries</li>
 * <li>Pattern-based invalidation with wildcard support</li>
 * </ul>
 * 
 * @author Redmine Connector Team
 * @version 2.0
 */
public class SimpleCacheService implements CacheService {

    private final ConcurrentHashMap<String, CacheEntry<?>> cache;
    private final ScheduledExecutorService cleanupExecutor;
    private final boolean persistent;

    /**
     * Internal cache entry with value and expiration time.
     */
    private static class CacheEntry<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        final T value;
        final long expirationTime;

        CacheEntry(T value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        boolean isExpired() {
            return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
        }
    }

    /**
     * Creates a SimpleCacheService with automatic cleanup every 60 seconds
     * and persistence enabled by default.
     */
    public SimpleCacheService() {
        this(true);
    }

    /**
     * Creates a SimpleCacheService with specified persistence.
     * 
     * @param persistent true to load/save from disk, false for in-memory only
     */
    public SimpleCacheService(boolean persistent) {
        this.cache = new ConcurrentHashMap<>();
        this.persistent = persistent;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CacheCleanup");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic cleanup of expired entries
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 60, 60, TimeUnit.SECONDS);

        // Load persistend cache
        if (persistent) {
            loadCache();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        CacheEntry<?> entry = cache.get(key);

        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return Optional.empty();
        }

        return Optional.of((T) entry.value);
    }

    @Override
    public <T> void put(String key, T value, long ttlSeconds) {
        long expirationTime = ttlSeconds > 0
                ? System.currentTimeMillis() + (ttlSeconds * 1000)
                : 0; // 0 means no expiration

        cache.put(key, new CacheEntry<>(value, expirationTime));
    }

    @Override
    public void invalidate(String key) {
        cache.remove(key);
    }

    @Override
    public void invalidateAll() {
        cache.clear();
    }

    @Override
    public void invalidatePattern(String pattern) {
        // Convert simple wildcard pattern to regex
        // e.g., "users:*" becomes "users:.*"
        String regex = pattern.replace("*", ".*").replace("?", ".");

        cache.keySet().removeIf(key -> key.matches(regex));
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean contains(String key) {
        CacheEntry<?> entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    /**
     * Removes all expired entries from the cache.
     * Called periodically by the cleanup executor.
     */
    private void cleanupExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Shuts down the cleanup executor and saves the cache to disk.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        if (persistent) {
            saveCache();
        }
    }

    // --- Persistence Logic ---

    private void saveCache() {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream(getCacheFile()))) {
            // Filter out expired entries before saving
            cleanupExpired();
            oos.writeObject(cache);
            // System.out.println("Cache saved to disk.");
        } catch (Exception e) {
            redmineconnector.util.LoggerUtil.logError("SimpleCacheService", "Error saving cache: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadCache() {
        java.io.File file = getCacheFile();
        if (!file.exists()) {
            return;
        }

        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                new java.io.FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof ConcurrentHashMap) {
                ConcurrentHashMap<String, CacheEntry<?>> loaded = (ConcurrentHashMap<String, CacheEntry<?>>) obj;
                this.cache.putAll(loaded);
                // Clean immediately in case we loaded old expired stuff
                cleanupExpired();
                // System.out.println("Cache loaded from disk (" + cache.size() + " entries).");
            }
        } catch (Exception e) {
            redmineconnector.util.LoggerUtil.logError("SimpleCacheService", "Error loading cache: " + e.getMessage(),
                    e);
            // If corrupt, delete it
            file.delete();
        }
    }

    private java.io.File getCacheFile() {
        return new java.io.File(System.getProperty("user.home"), ".redmine_connector_cache.dat");
    }
}
