package redmineconnector.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import redmineconnector.model.*;
import redmineconnector.util.HttpUtils;
import redmineconnector.util.JsonParser;

public class HttpDataService implements DataService {
    private final String baseUrl, apiKey;
    private final Consumer<String> logger;
    private final java.util.Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        List<SimpleEntity> data;
        long timestamp;

        CacheEntry(List<SimpleEntity> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    public HttpDataService(String url, String key, Consumer<String> logger) {
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.apiKey = key;
        this.logger = logger;
    }

    @Override
    public List<Task> fetchTasks(String pid, boolean closed, int limit) throws Exception {
        List<Task> all = new ArrayList<>();
        String status = closed ? "*" : "open";
        int max = (limit <= 0) ? Integer.MAX_VALUE : limit;
        int offset = 0, batch = redmineconnector.util.AppConstants.DEFAULT_FETCH_BATCH_SIZE;
        if (logger != null)
            logger.accept("DEBUG: Iniciando fetchTasks desde " + baseUrl + " (proj=" + pid + ")");
        while (all.size() < max) {
            int needed = Math.min(batch, max - all.size());
            if (needed <= 0)
                break;
            String uri = String.format(
                    "%s/issues.json?project_id=%s&status_id=%s&limit=%d&offset=%d&sort=id:desc&include=attachments&key=%s",
                    baseUrl, pid, status, needed, offset, apiKey);
            if (logger != null)
                logger.accept("DEBUG: GET " + uri);
            List<Task> page = JsonParser.parseIssues(HttpUtils.get(uri));
            if (logger != null)
                logger.accept("DEBUG: Página obtenida. Items: " + page.size());
            if (page.isEmpty())
                break;
            page.forEach(t -> t.webUrl = baseUrl + "/issues/" + t.id);
            all.addAll(page);
            if (page.size() < needed)
                break;
            offset += page.size();
        }
        if (logger != null)
            logger.accept("DEBUG: Fetch completado. Total tareas: " + all.size());
        return all;
    }

    @Override
    public Task fetchTaskDetails(int id) throws Exception {
        String uri = String.format("%s/issues/%d.json?include=attachments,journals,changesets,custom_fields&key=%s",
                baseUrl, id,
                apiKey);
        if (logger != null)
            logger.accept("DEBUG: Obteniendo detalles ID " + id + ": " + uri);
        List<Task> result = JsonParser.parseIssues(HttpUtils.get(uri));
        if (result.isEmpty())
            throw new Exception("Tarea no encontrada");
        Task t = result.get(0);
        t.webUrl = baseUrl + "/issues/" + t.id;
        t.isFullDetails = true;
        return t;
    }

    @Override
    public List<SimpleEntity> fetchMetadata(String type, String pid) throws Exception {
        String cacheKey = type + ":" + (pid != null ? pid : "");
        long ttl = redmineconnector.util.AppConstants.CACHE_TTL_MS;
        if (cache.containsKey(cacheKey) && !cache.get(cacheKey).isExpired(ttl)) {
            if (logger != null)
                logger.accept("DEBUG: Metadata Cache HIT: " + type);
            return cache.get(cacheKey).data;
        }

        if (logger != null)
            logger.accept("DEBUG: Cargando metadatos (Cache MISS): " + type);
        String url, jsonKey = type;
        String cleanPid = pid != null ? pid.trim() : "";
        switch (type) {
            case "users":
                url = String.format("%s/projects/%s/memberships.json?limit=100&key=%s", baseUrl, cleanPid, apiKey);
                break;
            case "trackers":
                url = String.format("%s/projects/%s.json?include=trackers&key=%s", baseUrl, cleanPid, apiKey);
                break;
            case "categories":
                if (cleanPid.isEmpty())
                    return new ArrayList<>();
                url = String.format("%s/projects/%s/issue_categories.json?key=%s", baseUrl, cleanPid, apiKey);
                jsonKey = "issue_categories";
                break;
            case "priorities":
                url = baseUrl + "/enumerations/issue_priorities.json?key=" + apiKey;
                jsonKey = "issue_priorities";
                break;
            case "statuses":
                url = baseUrl + "/issue_statuses.json?key=" + apiKey;
                jsonKey = "issue_statuses";
                break;
            case "versions":
                if (cleanPid.isEmpty())
                    return new ArrayList<>();
                url = String.format("%s/projects/%s/versions.json?key=%s", baseUrl, cleanPid, apiKey);
                jsonKey = "versions";
                break;
            case "activities":
                url = baseUrl + "/enumerations/time_entry_activities.json?key=" + apiKey;
                jsonKey = "time_entry_activities";
                break;
            default:
                return new ArrayList<>();
        }
        try {
            if (logger != null)
                logger.accept("DEBUG: GET Metadata " + url);
            String json = HttpUtils.get(url);
            List<SimpleEntity> result;
            if ("users".equals(type))
                result = JsonParser.parseMembers(json);
            else if ("trackers".equals(type)) {
                // Try to parse project-specific trackers first
                result = JsonParser.parseProjectTrackers(json);
                // If parsing returns empty, it might be that the project ID was empty or
                // invalid,
                // or the project really has no specific trackers (unlikely).
                // Or maybe the response was just not what we expected.
                // Fallback to global trackers if empty.
                if (result.isEmpty()) {
                    try {
                        result = JsonParser.parseEntities(HttpUtils.get(baseUrl + "/trackers.json?key=" + apiKey),
                                "trackers");
                    } catch (Exception e) {
                        // Keep empty result if fallback fails
                        if (logger != null)
                            logger.accept("DEBUG: Fallback to global trackers failed: " + e.getMessage());
                    }
                }
            } else if ("versions".equals(type))
                result = JsonParser.parseOpenVersions(json);
            else
                result = JsonParser.parseEntities(json, jsonKey);

            cache.put(cacheKey, new CacheEntry(result));
            return result;
        } catch (Exception e) {
            if (logger != null)
                logger.accept("ERROR: Falló carga metadatos (" + type + "): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<CustomFieldDefinition> fetchCustomFieldDefinitions() throws Exception {
        // Must include 'trackers' and 'projects' to avoid global-assumption in legacy
        // fallback logic
        String url = baseUrl + "/custom_fields.json?include=trackers,projects";
        String json = HttpUtils.get(url, apiKey);
        return JsonParser.parseCustomFieldDefinitions(json);
    }

    @Override
    public int createTask(String pid, Task t) throws Exception {
        String json = JsonParser.serializeTaskForCreate(pid, t);
        if (logger != null)
            logger.accept("DEBUG: Creando tarea payload: " + json);
        String resp = HttpUtils.post(baseUrl + "/issues.json", apiKey, json, false);
        if (logger != null)
            logger.accept("DEBUG: Create Response: " + resp);
        return JsonParser.extractId(resp);
    }

    @Override
    public void updateTask(Task t) throws Exception {
        String json = JsonParser.serializeTaskForUpdate(t);
        if (logger != null)
            logger.accept("DEBUG: Actualizando tarea #" + t.id + ". Payload: " + json);
        HttpUtils.put(baseUrl + "/issues/" + t.id + ".json", apiKey, json);
        if (logger != null)
            logger.accept("DEBUG: Update completado para #" + t.id);
    }

    @Override
    public String uploadFile(byte[] data, String contentType) throws Exception {
        String url = baseUrl + "/uploads.json?key=" + apiKey;
        if (logger != null)
            logger.accept("DEBUG: Subiendo archivo (" + data.length + " bytes) a " + url);
        String token = JsonParser.extractToken(HttpUtils.postBinary(url, data, "application/octet-stream", apiKey));
        if (logger != null)
            logger.accept("DEBUG: Upload token recibido: " + token);
        return token;
    }

    @Override
    public byte[] downloadAttachment(Attachment att) throws Exception {
        String url;
        if (att.contentUrl != null && !att.contentUrl.trim().isEmpty()) {
            url = att.contentUrl;
            // Always append key to URL for binary downloads to avoid redirect auth issues
            if (!url.contains("key=")) {
                url += (url.contains("?") ? "&" : "?") + "key=" + apiKey;
            }
            if (att.filename == null || att.filename.trim().isEmpty()) {
                att.filename = "attachment_" + att.id + ".dat";
            }
        } else {
            if (att.filename == null || att.filename.trim().isEmpty()) {
                att.filename = "attachment_" + att.id + ".dat";
            }
            url = String.format("%s/attachments/download/%d?key=%s", baseUrl, att.id, apiKey);
        }
        if (logger != null)
            logger.accept("DEBUG: Descargando adjunto: " + url);
        byte[] bytes = HttpUtils.downloadBytes(url, apiKey);
        if (logger != null)
            logger.accept("DEBUG: Descarga finalizada (" + bytes.length + " bytes)");
        return bytes;
    }

    @Override
    public void logTime(int issueId, String date, double hours, int userId, int activityId, String comment)
            throws Exception {
        String json = JsonParser.serializeTimeEntry(issueId, date, hours, userId, activityId, comment);
        if (logger != null)
            logger.accept("DEBUG: LogTime Req #" + issueId + ": " + json);
        String resp = HttpUtils.post(baseUrl + "/time_entries.json", apiKey, json, false);
        if (logger != null)
            logger.accept("DEBUG: LogTime Res: " + resp);
    }

    @Override
    public List<TimeEntry> fetchTimeEntries(String pid, String dateFrom, String dateTo) throws Exception {
        List<TimeEntry> all = new ArrayList<>();
        int limit = redmineconnector.util.AppConstants.DEFAULT_FETCH_BATCH_SIZE;
        int offset = 0;
        while (true) {
            String uri;
            if (pid == null || pid.trim().isEmpty()) {
                uri = String.format("%s/time_entries.json?from=%s&to=%s&limit=%d&offset=%d&key=%s",
                        baseUrl, dateFrom, dateTo, limit, offset, apiKey);
            } else {
                uri = String.format("%s/time_entries.json?project_id=%s&from=%s&to=%s&limit=%d&offset=%d&key=%s",
                        baseUrl, pid, dateFrom, dateTo, limit, offset, apiKey);
            }
            if (logger != null)
                logger.accept("DEBUG: Getting TimeEntries: " + uri);
            String json = HttpUtils.get(uri);
            List<TimeEntry> page = JsonParser.parseTimeEntries(json);
            all.addAll(page);
            if (page.size() < limit)
                break;
            offset += limit;
        }
        return all;
    }

    @Override
    public List<VersionDTO> fetchVersionsFull(String pid) throws Exception {
        String uri = String.format("%s/projects/%s/versions.json?key=%s", baseUrl, pid, apiKey);
        if (logger != null)
            logger.accept("DEBUG: Get All Versions " + uri);
        return JsonParser.parseVersionsFull(HttpUtils.get(uri));
    }

    @Override
    public void createVersion(String pid, String name, String status, String startDate, String dueDate)
            throws Exception {
        String json = JsonParser.serializeVersion(name, status, startDate, dueDate);
        String uri = String.format("%s/projects/%s/versions.json", baseUrl, pid);
        if (logger != null)
            logger.accept("DEBUG: Create Version: " + json + " at " + uri);
        HttpUtils.post(uri, apiKey, json, false);
    }

    @Override
    public void updateVersion(int id, String name, String status, String startDate, String dueDate) throws Exception {
        String json = JsonParser.serializeVersion(name, status, startDate, dueDate);
        String uri = String.format("%s/versions/%d.json", baseUrl, id);
        if (logger != null)
            logger.accept("DEBUG: Update Version #" + id + ": " + json);
        HttpUtils.put(uri, apiKey, json);
    }

    @Override
    public void deleteVersion(int id) throws Exception {
        String uri = String.format("%s/versions/%d.json", baseUrl, id);
        if (logger != null)
            logger.accept("DEBUG: Delete Version #" + id);
        HttpUtils.delete(uri, apiKey);
    }

    @Override
    public List<Task> fetchTasksByVersion(String pid, int versionId) throws Exception {
        String uri = String.format(
                "%s/issues.json?project_id=%s&fixed_version_id=%d&status_id=*&limit=100&sort=id:desc&key=%s", baseUrl,
                pid, versionId, apiKey);
        if (logger != null)
            logger.accept("DEBUG: Fetch Tasks for Version " + versionId);
        return JsonParser.parseIssues(HttpUtils.get(uri));
    }

    @Override
    public List<Task> fetchClosedTasks(String pid, String dateFrom, String dateTo) throws Exception {
        List<Task> all = new ArrayList<>();
        int limit = redmineconnector.util.AppConstants.DEFAULT_FETCH_BATCH_SIZE, offset = 0;
        String dateFilter = "%3E%3C" + dateFrom + "%7C" + dateTo;
        while (true) {
            String uri = String.format(
                    "%s/issues.json?project_id=%s&status_id=closed&closed_on=%s&limit=%d&offset=%d&sort=id:desc&key=%s",
                    baseUrl, pid, dateFilter, limit, offset, apiKey);
            if (logger != null)
                logger.accept("DEBUG: Fetch Closed Tasks: " + uri);
            List<Task> page = JsonParser.parseIssues(HttpUtils.get(uri));
            all.addAll(page);
            if (page.size() < limit)
                break;
            offset += limit;
        }
        return all;
    }

    @Override
    public List<WikiVersionDTO> fetchWikiHistory(String projectId, String pageTitle) throws Exception {
        String encodedTitle = URLEncoder.encode(pageTitle, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        String uri = String.format("%s/projects/%s/wiki/%s/revisions.json?key=%s", baseUrl, projectId, encodedTitle,
                apiKey);
        if (logger != null)
            logger.accept("DEBUG: Fetching Wiki History: " + uri);
        return JsonParser.parseWikiHistory(HttpUtils.get(uri));
    }

    @Override
    public void revertWikiPage(String projectId, String pageTitle, int version) throws Exception {
        // To revert, we'd fetch that version's content and update as new.
        // fetchWikiPageContent(projectId, pageTitle, version);
        // createOrUpdateWikiPage(..., content, "Reverted to v" + version);
        throw new UnsupportedOperationException("Revert not fully implemented in API layer yet.");
    }

    @Override
    public List<WikiPageDTO> fetchWikiPages(String projectId) throws Exception {
        String uri = String.format("%s/projects/%s/wiki/index.json?key=%s", baseUrl, projectId, apiKey);
        if (logger != null)
            logger.accept("DEBUG: Fetching Wiki Pages: " + uri);
        return JsonParser.parseWikiPagesIndex(HttpUtils.get(uri));
    }

    @Override
    public WikiPageDTO fetchWikiPageContent(String projectId, String pageTitle) throws Exception {
        String encodedTitle = URLEncoder.encode(pageTitle, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        String uri = String.format("%s/projects/%s/wiki/%s.json?include=attachments&key=%s", baseUrl, projectId,
                encodedTitle, apiKey);
        if (logger != null)
            logger.accept("DEBUG: Fetching Wiki Page Content: " + uri);
        return JsonParser.parseWikiPageContent(HttpUtils.get(uri));
    }

    @Override
    public void createOrUpdateWikiPage(String projectId, String pageTitle, String content, String comment)
            throws Exception {
        String encodedTitle = URLEncoder.encode(pageTitle, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        String uri = String.format("%s/projects/%s/wiki/%s.json", baseUrl, projectId, encodedTitle);
        String json = JsonParser.serializeWikiPage(content, comment);
        if (logger != null)
            logger.accept("DEBUG: Create/Update Wiki Page: " + pageTitle + " with payload: " + json);
        HttpUtils.put(uri, apiKey, json);
    }

    @Override
    public void deleteWikiPage(String projectId, String pageTitle) throws Exception {
        String encodedTitle = URLEncoder.encode(pageTitle, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        String uri = String.format("%s/projects/%s/wiki/%s.json", baseUrl, projectId, encodedTitle);
        if (logger != null)
            logger.accept("DEBUG: Deleting Wiki Page: " + pageTitle);
        HttpUtils.delete(uri, apiKey);
    }

    @Override
    public void uploadWikiAttachment(String projectId, String pageTitle, String token, String filename,
            String contentType, String currentText, int version) throws Exception {
        String encodedTitle = URLEncoder.encode(pageTitle, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        String uri = String.format("%s/projects/%s/wiki/%s.json", baseUrl, projectId, encodedTitle);

        // Redmine JSON format: { "wiki_page": { "text": "...", "version": N, "uploads":
        // [{ "token": "...", "filename": "...", "content_type": "..." }], "comments":
        // "..." } }
        // We must escape the text content properly for JSON
        String escapedText = JsonParser.escapeJsonString(currentText != null ? currentText : "");
        String escapedComment = "Adjuntado archivo: " + filename;

        String json = String.format(
                "{\"wiki_page\":{\"text\":\"%s\",\"version\":%d,\"uploads\":[{\"token\":\"%s\",\"filename\":\"%s\",\"content_type\":\"%s\"}],\"comments\":\"%s\"}}",
                escapedText, version, token, filename, contentType, escapedComment);

        if (logger != null)
            logger.accept("DEBUG: Uploading Attachment to Wiki Page: " + pageTitle + " payload: " + json);

        HttpUtils.put(uri, apiKey, json);
    }

    @Override
    public SimpleEntity fetchCurrentUser() throws Exception {
        String url = baseUrl + "/users/current.json?key=" + apiKey;
        String json = HttpUtils.get(url);

        SimpleEntity user = JsonParser.parseCurrentUser(json);

        if (user != null) {
            redmineconnector.util.LoggerUtil.logDebug("HttpDataService",
                    "Fetched current user: " + user.name + " (ID: " + user.id + ")");
            // Keep original console logger for now if needed, or just rely on LoggerUtil
            if (logger != null)
                logger.accept("Current user: " + user.name + " (ID: " + user.id + ")");
            return user;
        }

        throw new RuntimeException("Could not parse current user from response");
    }

    @Override
    public SimpleEntity fetchProject(String identifier) throws Exception {
        String url = baseUrl + "/projects/" + identifier + ".json?key=" + apiKey;
        if (logger != null)
            logger.accept("DEBUG: Fetching project info: " + url);
        String json = HttpUtils.get(url);
        return JsonParser.parseProject(json);
    }

    @Override
    public List<Task> fetchTasksByIds(List<Integer> ids) throws Exception {
        List<Task> result = new ArrayList<>();
        if (ids == null || ids.isEmpty())
            return result;

        // Batch IDs to avoid URL length limits
        int batchSize = redmineconnector.util.AppConstants.MAX_BULK_BATCH_SIZE;
        for (int i = 0; i < ids.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ids.size());
            List<Integer> subList = ids.subList(i, end);
            String idsStr = subList.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");

            String uri = String.format(
                    "%s/issues.json?issue_id=%s&status_id=*&limit=%d&key=%s",
                    baseUrl, idsStr, batchSize, apiKey);

            if (logger != null)
                logger.accept("DEBUG: Bulk Fetch Tasks: IDs=" + idsStr);
            result.addAll(JsonParser.parseIssues(HttpUtils.get(uri)));
        }
        return result;
    }

    @Override
    public List<SimpleEntity> fetchAllowedStatuses(String pid, int trackerId, int issueId) throws Exception {
        String uri;
        if (issueId > 0) {
            uri = String.format("%s/issues/%d.json?include=allowed_statuses&key=%s",
                    baseUrl, issueId, apiKey);
        } else {
            uri = String.format("%s/issues/new.json?issue[project_id]=%s&issue[tracker_id]=%d&key=%s",
                    baseUrl, pid, trackerId, apiKey);
        }

        if (logger != null)
            logger.accept("DEBUG: Obteniendo estados permitidos (ID=" + issueId + ", TID=" + trackerId + "): " + uri);
        String json = HttpUtils.get(uri);
        return JsonParser.parseAllowedStatuses(json);
    }

    @Override
    public redmineconnector.model.ContextMetadata fetchContextMetadata(String projectId, int trackerId, int issueId)
            throws Exception {
        String uri;
        if (issueId > 0) {
            // For existing issue, we might want to check editing context, but usually
            // /issues/new logic works for "allowed values"
            // However, Redmine's /issues/ID.json logic is better if we want to preserve
            // existing values?
            // Actually, to get "Available Custom Fields" for a specific tracker/project
            // combination, /issues/new is the specific endpoint providing this form data.
            uri = String.format("%s/issues/new.json?issue[project_id]=%s&issue[tracker_id]=%d&key=%s",
                    baseUrl, projectId, trackerId, apiKey);
        } else {
            uri = String.format("%s/issues/new.json?issue[project_id]=%s&issue[tracker_id]=%d&key=%s",
                    baseUrl, projectId, trackerId, apiKey);
        }

        if (logger != null)
            logger.accept("DEBUG: Fetching Context Metadata (Statuses & CFs): " + uri);

        String json = HttpUtils.get(uri);
        return JsonParser.parseContextMetadata(json);
    }
}
