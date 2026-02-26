package redmineconnector.service;

import java.util.Optional;

/**
 * Generic cache interface for storing and retrieving data with time-to-live
 * (TTL) support.
 * Implementations should be thread-safe for concurrent access.
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * CacheService cache = new SimpleCacheService();
 * cache.put("users:project1", userList, 300); // Cache for 5 minutes
 * 
 * Optional&lt;List&lt;User&gt;&gt; cached = cache.get("users:project1");
 * if (cached.isPresent()) {
 *     // Use cached data
 * } else {
 *     // Fetch fresh data
 * }
 * </pre>
 * 
 * @author Redmine Connector Team
 * @version 2.0
 */
public interface CacheService {

    /**
     * Retrieves a cached value if present and not expired.
     * 
     * @param <T> type of the cached value
     * @param key cache key
     * @return Optional containing the value if present and not expired, empty
     *         otherwise
     */
    <T> Optional<T> get(String key);

    /**
     * Stores a value in the cache with specified TTL.
     * 
     * @param <T>        type of the value to cache
     * @param key        cache key
     * @param value      value to cache
     * @param ttlSeconds time-to-live in seconds (0 for no expiration)
     */
    <T> void put(String key, T value, long ttlSeconds);

    /**
     * Removes a specific entry from the cache.
     * 
     * @param key cache key to invalidate
     */
    void invalidate(String key);

    /**
     * Removes all entries from the cache.
     */
    void invalidateAll();

    /**
     * Removes all entries whose keys match the given pattern.
     * Pattern matching is implementation-specific but typically supports wildcards.
     * 
     * @param pattern pattern to match (e.g., "users:*" to invalidate all user
     *                caches)
     */
    void invalidatePattern(String pattern);

    /**
     * Returns the number of entries currently in the cache.
     * 
     * @return cache size
     */
    int size();

    /**
     * Checks if the cache contains a valid (non-expired) entry for the given key.
     * 
     * @param key cache key
     * @return true if a valid entry exists, false otherwise
     */
    boolean contains(String key);
}
