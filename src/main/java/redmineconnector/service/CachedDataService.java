package redmineconnector.service;

import java.util.List;
import java.util.Optional;

import redmineconnector.model.*;

/**
 * Decorator for DataService that adds caching capabilities to reduce HTTP
 * requests.
 * Caches metadata (trackers, users, priorities, etc.) but not tasks to ensure
 * fresh data.
 * 
 * <p>
 * Caching strategy:
 * <ul>
 * <li><b>Cached (5 min TTL):</b> metadata, versions</li>
 * <li><b>Not cached:</b> tasks, task details, time entries</li>
 * <li><b>Invalidation:</b> automatic on create/update/delete operations</li>
 * </ul>
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * DataService baseService = new HttpDataService(url, key, logger);
 * CacheService cache = new SimpleCacheService();
 * DataService cachedService = new CachedDataService(baseService, cache);
 * 
 * // First call hits HTTP
 * List&lt;SimpleEntity&gt; users = cachedService.fetchMetadata("users", "project1");
 * 
 * // Second call within 5 minutes uses cache
 * List&lt;SimpleEntity&gt; users2 = cachedService.fetchMetadata("users", "project1");
 * </pre>
 * 
 * @author Redmine Connector Team
 * @version 2.0
 */
public class CachedDataService implements DataService {

    private final DataService delegate;
    private final CacheService cache;
    private final long metadataTtl;

    /**
     * Creates a CachedDataService with default TTL (300 seconds = 5 minutes).
     * 
     * @param delegate underlying DataService
     * @param cache    cache implementation
     */
    public CachedDataService(DataService delegate, CacheService cache) {
        this(delegate, cache, 300);
    }

    /**
     * Creates a CachedDataService with custom TTL.
     * 
     * @param delegate    underlying DataService
     * @param cache       cache implementation
     * @param metadataTtl TTL for metadata in seconds
     */
    public CachedDataService(DataService delegate, CacheService cache, long metadataTtl) {
        this.delegate = delegate;
        this.cache = cache;
        this.metadataTtl = metadataTtl;
    }

    @Override
    public List<Task> fetchTasks(String pid, boolean closed, int limit) throws Exception {
        String cacheKey = "tasks:" + pid + ":" + closed + ":" + limit;
        try {
            List<Task> result = delegate.fetchTasks(pid, closed, limit);
            cache.put(cacheKey, result, metadataTtl);
            return result;
        } catch (Exception e) {
            Optional<List<Task>> cached = cache.get(cacheKey);
            if (cached.isPresent()) {
                return cached.get();
            }
            throw e;
        }
    }

    @Override
    public List<SimpleEntity> fetchMetadata(String type, String pid) throws Exception {
        String cacheKey = "metadata:" + type + ":" + (pid != null ? pid : "global");

        // Try cache first
        Optional<List<SimpleEntity>> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Cache miss - fetch and cache
        try {
            List<SimpleEntity> result = delegate.fetchMetadata(type, pid);
            cache.put(cacheKey, result, metadataTtl);
            return result;
        } catch (Exception e) {
            if (cached.isPresent())
                return cached.get();
            throw e;
        }
    }

    @Override
    public List<CustomFieldDefinition> fetchCustomFieldDefinitions() throws Exception {
        String cacheKey = "custom_fields_defs";

        // Try cache first
        Optional<List<CustomFieldDefinition>> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Cache miss - fetch and cache
        List<CustomFieldDefinition> result = delegate.fetchCustomFieldDefinitions();
        cache.put(cacheKey, result, metadataTtl);
        return result;
    }

    @Override
    public Task fetchTaskDetails(int id) throws Exception {
        // Task details are NOT cached - they may be updated frequently
        return delegate.fetchTaskDetails(id);
    }

    @Override
    public int createTask(String pid, Task task) throws Exception {
        int result = delegate.createTask(pid, task);

        // Invalidate metadata cache for this project (new task may affect metadata)
        cache.invalidatePattern("metadata:*:" + pid);

        return result;
    }

    @Override
    public void updateTask(Task task) throws Exception {
        delegate.updateTask(task);

        // No cache invalidation needed - tasks are not cached
    }

    @Override
    public String uploadFile(byte[] data, String contentType) throws Exception {
        // File uploads are not cached
        return delegate.uploadFile(data, contentType);
    }

    @Override
    public byte[] downloadAttachment(Attachment att) throws Exception {
        // Attachments could be cached, but we skip for now to avoid memory issues
        return delegate.downloadAttachment(att);
    }

    @Override
    public void logTime(int issueId, String date, double hours, int userId, int activityId, String comment)
            throws Exception {
        delegate.logTime(issueId, date, hours, userId, activityId, comment);
    }

    @Override
    public List<TimeEntry> fetchTimeEntries(String pid, String dateFrom, String dateTo) throws Exception {
        // Time entries are NOT cached - they change frequently
        return delegate.fetchTimeEntries(pid, dateFrom, dateTo);
    }

    @Override
    public List<VersionDTO> fetchVersionsFull(String pid) throws Exception {
        String cacheKey = "versions:" + pid;

        // Cache versions
        Optional<List<VersionDTO>> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<VersionDTO> result = delegate.fetchVersionsFull(pid);
        cache.put(cacheKey, result, metadataTtl);
        return result;
    }

    @Override
    public void createVersion(String pid, String name, String status, String startDate, String dueDate)
            throws Exception {
        delegate.createVersion(pid, name, status, startDate, dueDate);

        // Invalidate versions cache
        cache.invalidate("versions:" + pid);
        cache.invalidatePattern("metadata:versions:" + pid);
    }

    @Override
    public void updateVersion(int id, String name, String status, String startDate, String dueDate) throws Exception {
        delegate.updateVersion(id, name, status, startDate, dueDate);

        // Invalidate all versions caches (we don't know which project)
        cache.invalidatePattern("versions:*");
        cache.invalidatePattern("metadata:versions:*");
    }

    @Override
    public void deleteVersion(int id) throws Exception {
        delegate.deleteVersion(id);

        // Invalidate all versions caches
        cache.invalidatePattern("versions:*");
        cache.invalidatePattern("metadata:versions:*");
    }

    @Override
    public List<Task> fetchTasksByVersion(String pid, int versionId) throws Exception {
        // Tasks are NOT cached
        return delegate.fetchTasksByVersion(pid, versionId);
    }

    @Override
    public List<Task> fetchClosedTasks(String pid, String dateFrom, String dateTo) throws Exception {
        // Tasks are NOT cached
        return delegate.fetchClosedTasks(pid, dateFrom, dateTo);
    }

    @Override
    public List<WikiPageDTO> fetchWikiPages(String projectId) throws Exception {
        String cacheKey = "wiki:index:" + projectId;

        // Cache wiki page list
        Optional<List<WikiPageDTO>> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<WikiPageDTO> result = delegate.fetchWikiPages(projectId);
        cache.put(cacheKey, result, metadataTtl);
        return result;
    }

    @Override
    public WikiPageDTO fetchWikiPageContent(String projectId, String pageTitle) throws Exception {
        String cacheKey = "wiki:page:" + projectId + ":" + pageTitle;

        // Cache wiki page content
        Optional<WikiPageDTO> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        WikiPageDTO result = delegate.fetchWikiPageContent(projectId, pageTitle);
        cache.put(cacheKey, result, metadataTtl);
        return result;
    }

    @Override
    public List<WikiVersionDTO> fetchWikiHistory(String projectId, String pageTitle) throws Exception {
        // History is typically not cached as it can be large and changes often (or
        // never if old versions)
        return delegate.fetchWikiHistory(projectId, pageTitle);
    }

    @Override
    public void revertWikiPage(String projectId, String pageTitle, int version) throws Exception {
        delegate.revertWikiPage(projectId, pageTitle, version);

        // Invalidate current page cache as it has changed
        cache.invalidate("wiki:page:" + projectId + ":" + pageTitle);
        cache.invalidate("wiki:index:" + projectId);
    }

    @Override
    public void createOrUpdateWikiPage(String projectId, String pageTitle, String content, String comment)
            throws Exception {
        delegate.createOrUpdateWikiPage(projectId, pageTitle, content, comment);

        // Invalidate wiki caches
        cache.invalidate("wiki:index:" + projectId);
        cache.invalidate("wiki:page:" + projectId + ":" + pageTitle);
    }

    @Override
    public void deleteWikiPage(String projectId, String pageTitle) throws Exception {
        delegate.deleteWikiPage(projectId, pageTitle);

        // Invalidate wiki caches
        cache.invalidate("wiki:index:" + projectId);
        cache.invalidate("wiki:page:" + projectId + ":" + pageTitle);
    }

    @Override
    public void uploadWikiAttachment(String projectId, String pageTitle, String token, String filename,
            String contentType, String currentText, int version) throws Exception {
        delegate.uploadWikiAttachment(projectId, pageTitle, token, filename, contentType, currentText, version);
        // Invalidate page as it has new attachment
        cache.invalidate("wiki:page:" + projectId + ":" + pageTitle);
    }

    /**
     * Gets the underlying cache service.
     * 
     * @return the cache service
     */
    public CacheService getCache() {
        return cache;
    }

    /**
     * Gets the underlying delegate service.
     * 
     * @return the delegate DataService
     */
    public DataService getDelegate() {
        return delegate;
    }

    @Override
    public SimpleEntity fetchCurrentUser() throws Exception {
        // No caching for current user - always fetch fresh
        return delegate.fetchCurrentUser();
    }

    @Override
    public SimpleEntity fetchProject(String identifier) throws Exception {
        // Could be cached, but for now just delegate
        return delegate.fetchProject(identifier);
    }

    @Override
    public redmineconnector.model.ContextMetadata fetchContextMetadata(String projectId, int trackerId, int issueId)
            throws Exception {
        // Dynamic data, no caching needed
        return delegate.fetchContextMetadata(projectId, trackerId, issueId);
    }

    @Override
    public List<Task> fetchTasksByIds(List<Integer> ids) throws Exception {
        // Tasks are not cached
        return delegate.fetchTasksByIds(ids);
    }

    @Override
    public List<SimpleEntity> fetchAllowedStatuses(String pid, int trackerId, int issueId) throws Exception {
        String cacheKey = "allowed_statuses:" + pid + ":" + trackerId + ":" + issueId;
        Optional<List<SimpleEntity>> cached = cache.get(cacheKey);
        if (cached.isPresent())
            return cached.get();
        List<SimpleEntity> result = delegate.fetchAllowedStatuses(pid, trackerId, issueId);
        cache.put(cacheKey, result, metadataTtl);
        return result;
    }

    @Override
    public void shutdown() {
        if (cache instanceof SimpleCacheService) {
            ((SimpleCacheService) cache).shutdown();
        }
    }
}
