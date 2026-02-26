package redmineconnector.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import redmineconnector.model.CustomField;
import redmineconnector.model.CustomFieldDefinition;
import redmineconnector.model.Task;
import redmineconnector.util.JsonParser;
import redmineconnector.util.LoggerUtil;

/**
 * Manages local caching of Custom Field Definitions.
 * Uses a cache key (URL/Host) to isolate definitions per Redmine instance.
 */
public class CustomFieldsCache {

    private static final String CACHE_FILE_PREFIX = "cache/custom_fields_cache_";
    // Map<CacheKey, List<Definitions>>
    private static final Map<String, List<CustomFieldDefinition>> cacheMap = new ConcurrentHashMap<>();

    private static String getSafeFileName(String key) {
        if (key == null || key.isEmpty())
            return "cache/custom_fields_cache.json";
        // Hash the key to avoid invalid chars in filename
        return CACHE_FILE_PREFIX + Integer.toHexString(key.hashCode()) + ".json";
    }

    public static synchronized void load(String cacheKey) {
        if (cacheKey == null)
            return;

        File f = new File(getSafeFileName(cacheKey));
        if (f.exists()) {
            try {
                String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                List<CustomFieldDefinition> defs = JsonParser.parseCustomFieldDefinitions(json);
                cacheMap.put(cacheKey, defs);
                LoggerUtil.logDebug("CustomFieldsCache",
                        "Loaded " + defs.size() + " definitions for key: " + cacheKey);
            } catch (Exception e) {
                LoggerUtil.logError("CustomFieldsCache", "Error loading cache for " + cacheKey, e);
            }
        }
    }

    public static synchronized void save(String cacheKey) {
        if (cacheKey == null)
            return;
        List<CustomFieldDefinition> defs = cacheMap.get(cacheKey);
        if (defs == null)
            return;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{ \"custom_fields\": [\n");
            for (int i = 0; i < defs.size(); i++) {
                CustomFieldDefinition def = defs.get(i);
                sb.append("  {");
                sb.append(" \"id\": ").append(def.id).append(",");
                sb.append(" \"name\": \"").append(escape(def.name)).append("\",");
                sb.append(" \"field_format\": \"").append(def.type).append("\",");
                sb.append(" \"is_required\": ").append(def.isRequired);

                if (def.possibleValues != null && !def.possibleValues.isEmpty()) {
                    sb.append(", \"possible_values\": [");
                    for (int j = 0; j < def.possibleValues.size(); j++) {
                        sb.append("{\"value\": \"").append(escape(def.possibleValues.get(j))).append("\"}");
                        if (j < def.possibleValues.size() - 1)
                            sb.append(", ");
                    }
                    sb.append("]");
                }

                // Persist tracker IDs so filtering works after reload
                if (def.trackerIds != null && !def.trackerIds.isEmpty()) {
                    sb.append(", \"trackers\": [");
                    for (int j = 0; j < def.trackerIds.size(); j++) {
                        sb.append("{\"id\": ").append(def.trackerIds.get(j)).append("}");
                        if (j < def.trackerIds.size() - 1)
                            sb.append(", ");
                    }
                    sb.append("]");
                }

                // Persist project IDs (CRITICAL)
                if (def.projectIds != null && !def.projectIds.isEmpty()) {
                    sb.append(", \"projects\": [");
                    for (int j = 0; j < def.projectIds.size(); j++) {
                        sb.append("{\"id\": ").append(def.projectIds.get(j)).append("}");
                        if (j < def.projectIds.size() - 1)
                            sb.append(", ");
                    }
                    sb.append("]");
                }

                sb.append(" }");
                if (i < defs.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("] }");

            File f = new File(getSafeFileName(cacheKey));
            if (f.getParentFile() != null && !f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (

        Exception e) {
            LoggerUtil.logError("CustomFieldsCache", "Error saving cache for " + cacheKey, e);
        }
    }

    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    /**
     * Merges definitions found in tasks into the cache for the specific instance.
     */
    public static synchronized void learnFromTasks(String cacheKey, List<Task> tasks) {
        if (cacheKey == null)
            return;

        List<CustomFieldDefinition> definitions = cacheMap.computeIfAbsent(cacheKey, k -> new ArrayList<>());
        boolean changed = false;

        Map<Integer, CustomFieldDefinition> map = new HashMap<>();
        for (CustomFieldDefinition d : definitions) {
            map.put(d.id, d);
        }

        for (Task t : tasks) {
            if (t.customFields != null) {
                for (CustomField cf : t.customFields) {
                    CustomFieldDefinition def = map.get(cf.id);

                    // 1. Learn new Field
                    if (def == null) {
                        def = new CustomFieldDefinition(cf.id, cf.name, "string", false);
                        def.possibleValues = new ArrayList<>();
                        map.put(cf.id, def);
                        definitions.add(def);
                        changed = true;
                        LoggerUtil.logInfo("CustomFieldsCache", "[" + cacheKey + "] Learned new field: " + cf.name);
                    }

                    // 2. Learn Tracker Association
                    if (t.trackerId > 0) {
                        if (!def.trackerIds.contains(t.trackerId)) {
                            def.trackerIds.add(t.trackerId);
                            changed = true;
                        }
                    }

                    // 3. Learn Project Association
                    if (t.projectId > 0) {
                        if (!def.projectIds.contains(t.projectId)) {
                            def.projectIds.add(t.projectId);
                            changed = true;
                        }
                    }

                    // 3. Learn new Value
                    if (cf.value != null && !cf.value.trim().isEmpty()) {
                        if (def.possibleValues == null) {
                            def.possibleValues = new ArrayList<>();
                        }
                        if (!def.possibleValues.contains(cf.value)) {
                            def.possibleValues.add(cf.value);
                            changed = true;
                        }
                    }

                    // 3. Learn Type (Date)
                    if (cf.value != null && cf.value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                        if (!"date".equals(def.type)) {
                            def.type = "date";
                            changed = true;
                            LoggerUtil.logInfo("CustomFieldsCache", "Inferred type 'date' for field: " + cf.name);
                        }
                    }
                }
            }
        }

        if (changed) {
            save(cacheKey);
        }
    }

    public static synchronized List<CustomFieldDefinition> getDefinitions(String cacheKey) {
        List<CustomFieldDefinition> defs = cacheMap.get(cacheKey);
        return defs != null ? new ArrayList<>(defs) : new ArrayList<>();
    }

    // Legacy support to avoid immediate compilation errors, will return empty or
    // default
    public static synchronized List<CustomFieldDefinition> getDefinitions() {
        if (cacheMap.isEmpty())
            return new ArrayList<>();
        return new ArrayList<>(cacheMap.values().iterator().next());
    }
}
