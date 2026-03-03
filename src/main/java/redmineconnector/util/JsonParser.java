package redmineconnector.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redmineconnector.model.*;

public class JsonParser {

    // --- Core Parsing Logic ---

    private static class SimpleParser {
        private final String json;
        private int pos;
        private final int len;

        public SimpleParser(String json) {
            this.json = json;
            this.len = json != null ? json.length() : 0;
            this.pos = 0;
        }

        public Object parse() {
            skipSpace();
            if (pos >= len)
                return null;
            char c = json.charAt(pos);
            if (c == '{')
                return parseObject();
            if (c == '[')
                return parseArray();
            if (c == '"')
                return parseString();
            if (Character.isDigit(c) || c == '-')
                return parseNumber();
            if (c == 't')
                return parseTrue();
            if (c == 'f')
                return parseFalse();
            if (c == 'n')
                return parseNull();
            return null; // error or unsupported
        }

        private void skipSpace() {
            while (pos < len && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new HashMap<>();
            pos++; // skip '{'
            skipSpace();
            if (pos < len && json.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < len) {
                String key = parseString();
                skipSpace();
                if (pos < len && json.charAt(pos) == ':')
                    pos++;
                Object value = parse();
                map.put(key, value);
                skipSpace();
                if (pos < len) {
                    char c = json.charAt(pos);
                    if (c == '}') {
                        pos++;
                        break;
                    }
                    if (c == ',') {
                        pos++;
                        skipSpace();
                    }
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            skipSpace();
            if (pos < len && json.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < len) {
                list.add(parse());
                skipSpace();
                if (pos < len) {
                    char c = json.charAt(pos);
                    if (c == ']') {
                        pos++;
                        break;
                    }
                    if (c == ',') {
                        pos++;
                        skipSpace();
                    }
                }
            }
            return list;
        }

        private String parseString() {
            if (pos >= len || json.charAt(pos) != '"')
                return "";
            pos++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < len) {
                char c = json.charAt(pos++);
                if (c == '"')
                    break;
                if (c == '\\') {
                    if (pos >= len)
                        break;
                    char next = json.charAt(pos++);
                    switch (next) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            if (pos + 4 <= len) {
                                String hex = json.substring(pos, pos + 4);
                                try {
                                    sb.append((char) Integer.parseInt(hex, 16));
                                } catch (Exception ignored) {
                                }
                                pos += 4;
                            }
                            break;
                        default:
                            sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            if (pos < len && json.charAt(pos) == '-')
                pos++;
            while (pos < len && Character.isDigit(json.charAt(pos)))
                pos++;
            if (pos < len && json.charAt(pos) == '.') {
                pos++;
                while (pos < len && Character.isDigit(json.charAt(pos)))
                    pos++;
            }
            // Exponent support omitted for simplicity unless needed, assuming Redmine basic
            // JSON
            String numStr = json.substring(start, pos);
            try {
                if (numStr.contains("."))
                    return Double.parseDouble(numStr);
                return Long.parseLong(numStr);
            } catch (Exception e) {
                return 0;
            }
        }

        private Boolean parseTrue() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return true;
            }
            return null;
        }

        private Boolean parseFalse() {
            if (json.startsWith("false", pos)) {
                pos += 5;
                return false;
            }
            return null;
        }

        private Object parseNull() {
            if (json.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            return null;
        }
    }

    public static Object parse(String json) {
        return new SimpleParser(json).parse();
    }

    // --- Data Extraction Helpers ---

    private static String asString(Object o) {
        return o instanceof String ? (String) o : (o != null ? String.valueOf(o) : "");
    }

    private static int asInt(Object o) {
        if (o instanceof Number)
            return ((Number) o).intValue();
        if (o instanceof String) {
            try {
                return Integer.parseInt((String) o);
            } catch (Exception e) {
            }
        }
        return 0;
    }

    private static double asDouble(Object o) {
        if (o instanceof Number)
            return ((Number) o).doubleValue();
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (Exception e) {
            }
        }
        return 0.0;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        if (o instanceof Map)
            return (Map<String, Object>) o;
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        if (o instanceof List)
            return (List<Object>) o;
        return new ArrayList<>();
    }

    // --- Domain Parsing Methods ---

    public static List<Task> parseIssues(String json) {
        List<Task> list = new ArrayList<>();
        Object root = parse(json);
        if (!(root instanceof Map))
            return list;

        Map<String, Object> rootMap = asMap(root);
        List<Object> issues = null;
        if (rootMap.containsKey("issues"))
            issues = asList(rootMap.get("issues"));
        else if (rootMap.containsKey("issue")) { // Single issue wrapped as issue
            Object single = rootMap.get("issue");
            if (single instanceof Map) {
                Task t = parseTaskMap(asMap(single));
                if (t != null && t.id > 0)
                    list.add(t);
                return list;
            }
        }

        if (issues != null) {
            for (Object o : issues) {
                if (o instanceof Map) {
                    Task t = parseTaskMap(asMap(o));
                    if (t != null && t.id > 0)
                        list.add(t);
                }
            }
        }
        return list;
    }

    private static Task parseTaskMap(Map<String, Object> map) {
        try {
            Task t = new Task();
            t.id = asInt(map.get("id"));
            t.subject = asString(map.get("subject"));
            t.description = asString(map.get("description"));

            Map<String, Object> project = asMap(map.get("project"));
            t.projectId = asInt(project.get("id"));

            Map<String, Object> status = asMap(map.get("status"));
            t.status = asString(status.get("name"));
            t.statusId = asInt(status.get("id"));

            Map<String, Object> priority = asMap(map.get("priority"));
            t.priority = asString(priority.get("name"));
            t.priorityId = asInt(priority.get("id"));

            Map<String, Object> tracker = asMap(map.get("tracker"));
            t.tracker = asString(tracker.get("name"));
            t.trackerId = asInt(tracker.get("id"));

            Map<String, Object> assigned = asMap(map.get("assigned_to"));
            t.assignedTo = asString(assigned.get("name"));
            t.assignedToId = asInt(assigned.get("id"));

            Map<String, Object> category = asMap(map.get("category"));
            t.category = asString(category.get("name"));
            t.categoryId = asInt(category.get("id"));

            Map<String, Object> version = asMap(map.get("fixed_version"));
            t.targetVersion = asString(version.get("name"));
            t.targetVersionId = asInt(version.get("id"));

            Map<String, Object> author = asMap(map.get("author"));
            t.author = asString(author.get("name"));
            t.authorId = asInt(author.get("id"));
            // authorEmail not always present in minimal view, check if map has it?
            // "mail" is usually not in issue view unless custom query

            // Parent
            if (map.containsKey("parent")) {
                Map<String, Object> parent = asMap(map.get("parent"));
                t.parentId = asInt(parent.get("id"));
                t.parentName = asString(parent.get("subject")); // Maybe name? Redmine uses subject for issues? No,
                                                                // issue links usually show id.
                // Wait, original code used sub("parent") => extract name.
                // Redmine API for parent issue: {"id": 1, "subject": "..."} ? No, it usually
                // just has id.
                // But let's check legacy: sub(s, "parent") -> extractObjBlock -> get(block,
                // "name").
                // If parent object has "name", use it. Issues usually have "subject".
                // I will try "name" then "subject".
                String pName = asString(parent.get("name"));
                if (pName.isEmpty())
                    pName = asString(parent.get("subject")); // Fallback?
                t.parentName = pName;
            }

            t.spentHours = asDouble(map.get("spent_hours"));
            t.doneRatio = asInt(map.get("done_ratio"));

            String createdOn = asString(map.get("created_on"));
            if (createdOn == null || createdOn.isEmpty()) {
                t.createdOn = new Date();
            } else {
                try {
                    // Try standard Redmine format first
                    t.createdOn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(createdOn);
                } catch (Exception e1) {
                    try {
                        // Try format with milliseconds (some plugins/versions adds this)
                        t.createdOn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(createdOn);
                    } catch (Exception e2) {
                        try {
                            // Try format without Z (local time fallback)
                            t.createdOn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(createdOn);
                        } catch (Exception e3) {
                            try {
                                // Try simple date (YYYY-MM-DD)
                                t.createdOn = new SimpleDateFormat("yyyy-MM-dd").parse(createdOn);
                            } catch (Exception e4) {
                                redmineconnector.util.LoggerUtil.logError("JsonParser",
                                        "Unparseable date: '" + createdOn + "' for task " + t.id, e1);
                                t.createdOn = new Date(); // Fallback to now
                            }
                        }
                    }
                }
            }

            // Sub-collections
            t.attachments = parseAttachmentsList(asList(map.get("attachments")));
            t.journals = parseJournalsList(asList(map.get("journals")));
            t.changesets = parseChangesetsList(asList(map.get("changesets")));
            t.customFields = parseCustomFieldsList(asList(map.get("custom_fields")));

            return t;
        } catch (Exception e) {
            redmineconnector.util.LoggerUtil.logError("JsonParser",
                    "Failed to parse task map", e);
            return null;
        }
    }

    public static List<Attachment> parseAttachments(String json) {
        Object root = parse(json);
        List<Object> list = null;
        if (root instanceof Map)
            list = asList(asMap(root).get("attachments"));
        return parseAttachmentsList(list != null ? list : new ArrayList<>());
    }

    private static List<Attachment> parseAttachmentsList(List<Object> list) {
        List<Attachment> result = new ArrayList<>();
        for (Object o : list) {
            Map<String, Object> m = asMap(o);
            result.add(new Attachment(
                    asInt(m.get("id")),
                    asString(m.get("filename")),
                    asString(m.get("content_url")),
                    asString(m.get("content_type")),
                    asInt(m.get("filesize"))));
        }
        return result;
    }

    public static List<Journal> parseJournals(String json) {
        Object root = parse(json);
        List<Object> list = null;
        if (root instanceof Map)
            list = asList(asMap(root).get("journals"));
        return parseJournalsList(list != null ? list : new ArrayList<>());
    }

    private static List<Journal> parseJournalsList(List<Object> list) {
        List<Journal> result = new ArrayList<>();
        for (Object o : list) {
            Map<String, Object> m = asMap(o);
            Map<String, Object> user = asMap(m.get("user"));
            result.add(new Journal(
                    asString(user.get("name")),
                    asString(m.get("notes")),
                    asString(m.get("created_on"))));
        }
        return result;
    }

    public static List<Changeset> parseChangesets(String json) {
        Object root = parse(json);
        List<Object> list = null;
        if (root instanceof Map)
            list = asList(asMap(root).get("changesets"));
        return parseChangesetsList(list != null ? list : new ArrayList<>());
    }

    private static List<Changeset> parseChangesetsList(List<Object> list) {
        List<Changeset> result = new ArrayList<>();
        for (Object o : list) {
            Map<String, Object> m = asMap(o);
            Map<String, Object> user = asMap(m.get("user"));
            result.add(new Changeset(
                    asString(m.get("revision")),
                    asString(user.get("name")),
                    asString(m.get("comments")),
                    asString(m.get("committed_on"))));
        }
        return result;
    }

    public static List<SimpleEntity> parseEntities(String json, String key) {
        List<SimpleEntity> l = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            List<Object> list = asList(asMap(root).get(key));
            for (Object o : list) {
                Map<String, Object> m = asMap(o);
                int id = asInt(m.get("id"));
                String name = asString(m.get("name"));
                // Check is_closed in logic or property? Original verified global string or
                // local content.
                // Here we just check property if present.
                boolean isClosed = Boolean.TRUE.equals(m.get("is_closed"));
                // Note: Redmine statuses usually have "is_closed" boolean.
                if (id > 0 && !name.isEmpty())
                    l.add(new SimpleEntity(id, name, isClosed));
            }
        }
        return l;
    }

    public static List<SimpleEntity> parseProjectTrackers(String json) {
        List<SimpleEntity> l = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            Map<String, Object> rootMap = asMap(root);
            // Check if wrapped in "project"
            if (rootMap.containsKey("project")) {
                Map<String, Object> project = asMap(rootMap.get("project"));
                if (project.containsKey("trackers")) {
                    List<Object> list = asList(project.get("trackers"));
                    for (Object o : list) {
                        Map<String, Object> m = asMap(o);
                        int id = asInt(m.get("id"));
                        String name = asString(m.get("name"));
                        if (id > 0 && !name.isEmpty())
                            l.add(new SimpleEntity(id, name));
                    }
                }
            }
        }
        return l;
    }

    public static List<SimpleEntity> parseOpenVersions(String json) {
        List<SimpleEntity> l = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            List<Object> list = asList(asMap(root).get("versions"));
            for (Object o : list) {
                Map<String, Object> m = asMap(o);
                String status = asString(m.get("status"));
                boolean isClosed = "closed".equalsIgnoreCase(status);
                if ("open".equalsIgnoreCase(status)) {
                    l.add(new SimpleEntity(asInt(m.get("id")), asString(m.get("name")), isClosed));
                }
            }
        }
        return l;
    }

    public static List<VersionDTO> parseVersionsFull(String json) {
        List<VersionDTO> l = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            List<Object> list = asList(asMap(root).get("versions"));
            for (Object o : list) {
                Map<String, Object> m = asMap(o);
                String startDate = asString(m.get("start_date"));
                if (startDate.isEmpty())
                    startDate = asString(m.get("created_on"));
                l.add(new VersionDTO(
                        asInt(m.get("id")),
                        asString(m.get("name")),
                        asString(m.get("status")),
                        startDate,
                        asString(m.get("due_date"))));
            }
        }
        return l;
    }

    public static List<SimpleEntity> parseMembers(String json) {
        List<SimpleEntity> l = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            List<Object> list = asList(asMap(root).get("memberships"));
            for (Object o : list) {
                Map<String, Object> m = asMap(o);
                Map<String, Object> user = asMap(m.get("user"));
                int uid = asInt(user.get("id"));
                String uname = asString(user.get("name"));
                if (uid > 0 && !uname.isEmpty())
                    l.add(new SimpleEntity(uid, uname));
            }
        }
        return l;
    }

    public static List<TimeEntry> parseTimeEntries(String json) {
        List<TimeEntry> list = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            List<Object> entries = asList(asMap(root).get("time_entries"));
            for (Object o : entries) {
                Map<String, Object> m = asMap(o);
                Map<String, Object> issue = asMap(m.get("issue"));
                Map<String, Object> user = asMap(m.get("user"));

                list.add(new TimeEntry(
                        asInt(m.get("id")),
                        asInt(issue.get("id")),
                        asString(issue.get("name")), // Or subject? TimeEntry issue object usually has id/name?
                        asString(user.get("name")),
                        asDouble(m.get("hours")),
                        asString(m.get("spent_on")),
                        asString(m.get("comments"))));
            }
        }
        return list;
    }

    public static List<WikiPageDTO> parseWikiPagesIndex(String json) {
        List<WikiPageDTO> list = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            List<Object> pages = asList(asMap(root).get("wiki_pages"));
            for (Object o : pages) {
                Map<String, Object> m = asMap(o);
                list.add(new WikiPageDTO(asString(m.get("title"))));
            }
        }
        return list;
    }

    public static WikiPageDTO parseWikiPageContent(String json) {
        Object root = parse(json);
        if (!(root instanceof Map))
            return null;
        Map<String, Object> m = asMap(asMap(root).get("wiki_page"));
        if (m.isEmpty())
            return null;

        String title = asString(m.get("title"));
        String text = asString(m.get("text"));
        String version = String.valueOf(asInt(m.get("version")));
        String updatedOn = asString(m.get("updated_on"));

        // Author extraction
        String author = "";
        if (m.containsKey("author")) {
            Map<String, Object> a = asMap(m.get("author"));
            author = asString(a.get("name"));
        }

        WikiPageDTO dto = new WikiPageDTO(title, text, version, updatedOn, author);
        dto.attachments = parseAttachmentsList(asList(m.get("attachments")));
        // Fallback: Check root for attachments if not found in wiki_page
        if (dto.attachments.isEmpty()) {
            dto.attachments = parseAttachmentsList(asList(asMap(root).get("attachments")));
        }
        return dto;
    }

    public static List<WikiVersionDTO> parseWikiHistory(String json) {
        List<WikiVersionDTO> list = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) { // Wrapped in revisions? Check original code.
            // Old code: json.indexOf("revisions").
            // Redmine API: {"revisions": [...]}? Or {"wiki_page": { "revisions": ... }}?
            // Usually root revisions.
            // If we assume root.
            List<Object> revisions = asList(asMap(root).get("revisions"));
            // If empty, maybe it's nested? No, standard API is /revisions.json -> {
            // revisions: [] }
            for (Object o : revisions) {
                Map<String, Object> m = asMap(o);
                Map<String, Object> author = asMap(m.get("author"));
                list.add(new WikiVersionDTO(
                        asInt(m.get("version")),
                        asString(m.get("updated_on")),
                        asString(author.get("name")),
                        asString(m.get("comments"))));
            }
        }
        return list;
    }

    public static SimpleEntity parseCurrentUser(String json) {
        Object root = parse(json);
        if (root instanceof Map) {
            Map<String, Object> rootMap = asMap(root);
            if (rootMap.containsKey("user")) {
                Map<String, Object> user = asMap(rootMap.get("user"));
                return new SimpleEntity(asInt(user.get("id")),
                        asString(user.get("firstname")) + " " + asString(user.get("lastname")));
            }
        }
        return null;
    }

    public static SimpleEntity parseProject(String json) {
        Object root = parse(json);
        if (root instanceof Map) {
            Map<String, Object> rootMap = asMap(root);
            if (rootMap.containsKey("project")) {
                Map<String, Object> proj = asMap(rootMap.get("project"));
                return new SimpleEntity(asInt(proj.get("id")), asString(proj.get("name")));
            }
        }
        return null;
    }

    public static int extractId(String json) {
        Object root = parse(json);
        if (root instanceof Map) {
            Map<String, Object> m = asMap(root);
            if (m.containsKey("id"))
                return asInt(m.get("id"));
            if (m.containsKey("issue")) {
                Map<String, Object> issue = asMap(m.get("issue"));
                return asInt(issue.get("id"));
            }
        }
        return 0;
    }

    public static String extractToken(String json) {
        // Redmine returns: {"upload": {"token": "...", "id": ..., "filename": "..."}}
        // We need to extract upload.token, not just token at root level
        Object root = parse(json);
        if (root instanceof Map) {
            Map<String, Object> rootMap = asMap(root);
            Object upload = rootMap.get("upload");
            if (upload instanceof Map) {
                return asString(asMap(upload).get("token"));
            }
            // Fallback: try root level (for backwards compatibility)
            return asString(rootMap.get("token"));
        }
        return "";
    }

    // --- Compatible String-based Helpers (Now using Parser internally) ---

    /**
     * Finds the value of a key in the JSON string.
     * Note: This parses the string to find the key. For performance on repeated
     * calls, refactor to parse once.
     * Use with caution.
     */
    public static String get(String json, String key) {
        Object root = parse(json);
        if (root instanceof Map)
            return asString(asMap(root).get(key));
        return "";
    }

    public static int getInt(String json, String key) {
        Object root = parse(json);
        if (root instanceof Map)
            return asInt(asMap(root).get(key));
        return 0;
    }

    public static double getDouble(String json, String key) {
        Object root = parse(json);
        if (root instanceof Map)
            return asDouble(asMap(root).get(key));
        return 0.0;
    }

    // --- Serialization (Builders) ---
    // Kept as is, but make sure they don't depend on removed methods.

    public static String escapeJsonString(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Kept older escape for compatibility if needed internally, but mapped to new
    // one
    private static String escape(String s) {
        return escapeJsonString(s);
        // Original was simpler, but full escape is safer.
    }

    public static String serializeWikiPage(String content, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"wiki_page\":{");
        sb.append("\"text\":\"").append(escape(content)).append("\"");
        if (comment != null && !comment.trim().isEmpty()) {
            sb.append(",\"comments\":\"").append(escape(comment)).append("\"");
        }
        sb.append("}}");
        return sb.toString();
    }

    public static String serializeTaskForCreate(String pid, Task t) {
        return buildJson(pid, t, false);
    }

    public static String serializeTaskForUpdate(Task t) {
        return buildJson(null, t, true);
    }

    public static String serializeVersion(String name, String status, String startDate, String dueDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"version\":{");
        sb.append("\"name\":\"").append(escape(name)).append("\",");
        sb.append("\"status\":\"").append(status).append("\"");
        if (startDate != null && !startDate.trim().isEmpty()) {
            sb.append(",\"start_date\":\"").append(startDate).append("\"");
        }
        if (dueDate != null && !dueDate.trim().isEmpty()) {
            sb.append(",\"due_date\":\"").append(dueDate).append("\"");
        }
        sb.append("}}");
        return sb.toString();
    }

    public static String serializeTimeEntry(int issueId, String date, double hours, int userId, int activityId,
            String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"time_entry\":{");
        sb.append("\"issue_id\":").append(issueId);
        sb.append(",\"spent_on\":\"").append(date).append("\"");
        sb.append(",\"hours\":").append(hours);
        if (userId > 0)
            sb.append(",\"user_id\":").append(userId);
        if (activityId > 0)
            sb.append(",\"activity_id\":").append(activityId);
        if (comment != null && !comment.trim().isEmpty())
            sb.append(",\"comments\":\"").append(escape(comment)).append("\"");
        sb.append("}}");
        return sb.toString();
    }

    private static String buildJson(String pid, Task t, boolean update) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"issue\":{");
        boolean firstField = true;

        if (pid != null) {
            sb.append("\"project_id\":\"").append(pid).append("\"");
            firstField = false;
        }

        if (!firstField)
            sb.append(",");
        sb.append("\"subject\":\"").append(escape(t.subject)).append("\"");
        firstField = false;

        if (!firstField)
            sb.append(",");
        sb.append("\"description\":\"").append(escape(t.description)).append("\"");
        firstField = false;

        if (t.priorityId > 0) {
            if (!firstField)
                sb.append(",");
            sb.append("\"priority_id\":").append(t.priorityId);
            firstField = false;
        }
        if (t.trackerId > 0) {
            if (!firstField)
                sb.append(",");
            sb.append("\"tracker_id\":").append(t.trackerId);
            firstField = false;
        }
        if (t.statusId > 0) {
            if (!firstField)
                sb.append(",");
            sb.append("\"status_id\":").append(t.statusId);
            firstField = false;
        }

        if (t.assignedToId > 0) {
            if (!firstField)
                sb.append(",");
            sb.append("\"assigned_to_id\":").append(t.assignedToId);
            firstField = false;
        } else if (update) {
            if (!firstField)
                sb.append(",");
            sb.append("\"assigned_to_id\":\"\"");
            firstField = false;
        }

        if (t.categoryId > 0) {
            if (!firstField)
                sb.append(",");
            sb.append("\"category_id\":").append(t.categoryId);
            firstField = false;
        }

        if (t.targetVersionId > 0) {
            if (!firstField)
                sb.append(",");
            sb.append("\"fixed_version_id\":").append(t.targetVersionId);
            firstField = false;
        } else if (update) {
            if (!firstField)
                sb.append(",");
            sb.append("\"fixed_version_id\":\"\"");
            firstField = false;
        }

        if (t.parentId > 0) {
            if (!firstField)
                sb.append(",");
            sb.append("\"parent_issue_id\":").append(t.parentId);
            firstField = false;
        }

        if (t.comment != null && !t.comment.trim().isEmpty()) {
            if (!firstField)
                sb.append(",");
            sb.append("\"notes\":\"").append(escape(t.comment)).append("\"");
            firstField = false;
        }

        if (!firstField)
            sb.append(",");
        sb.append("\"done_ratio\":").append(t.doneRatio);
        firstField = false;

        if (t.pendingUploads != null && !t.pendingUploads.isEmpty()) {
            if (!firstField)
                sb.append(",");
            sb.append("\"uploads\":[");
            for (int i = 0; i < t.pendingUploads.size(); i++) {
                UploadToken ut = t.pendingUploads.get(i);
                sb.append(i > 0 ? "," : "").append("{\"token\":\"").append(ut.token).append("\",\"filename\":\"")
                        .append(ut.filename).append("\",\"content_type\":\"").append(ut.contentType).append("\"}");
            }
            sb.append("]");
        }

        // Custom Fields Serialization
        if (t.customFields != null && !t.customFields.isEmpty()) {
            if (!firstField)
                sb.append(",");
            sb.append("\"custom_fields\":[");
            boolean firstCf = true;
            for (redmineconnector.model.CustomField cf : t.customFields) {
                if (cf.value == null || cf.value.trim().isEmpty()) {
                    continue;
                }
                if (!firstCf)
                    sb.append(",");
                sb.append("{");
                sb.append("\"id\":").append(cf.id).append(",");
                sb.append("\"value\":\"").append(escape(cf.value)).append("\"");
                sb.append("}");
                firstCf = false;
            }
            sb.append("]");
            firstField = false;
        }

        sb.append("}}");
        return sb.toString();
    }

    public static List<SimpleEntity> parseAllowedStatuses(String json) {
        List<SimpleEntity> list = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            Map<String, Object> rootMap = asMap(root);

            // Redmine often returns { "issue": { "allowed_statuses": [...] } }
            // or sometimes just { "statuses": [...] } if querying global statuses?
            // But for /issues/new.json and /issues/ID.json, it is wrapped in "issue".

            Map<String, Object> targetMap = rootMap;
            if (rootMap.containsKey("issue") && rootMap.get("issue") instanceof Map) {
                targetMap = asMap(rootMap.get("issue"));
            }

            // Check allowed_statuses or statuses
            List<Object> statuses = null;
            if (targetMap.containsKey("allowed_statuses"))
                statuses = asList(targetMap.get("allowed_statuses"));
            else if (targetMap.containsKey("statuses")) // Fallback
                statuses = asList(targetMap.get("statuses"));

            if (statuses != null) {
                for (Object o : statuses) {
                    Map<String, Object> m = asMap(o);
                    int id = asInt(m.get("id"));
                    String name = asString(m.get("name"));
                    if (id > 0 && !name.isEmpty())
                        list.add(new SimpleEntity(id, name));
                }
            }
        }
        return list;
    }

    public static redmineconnector.model.ContextMetadata parseContextMetadata(String json) {
        List<redmineconnector.model.SimpleEntity> statuses = new ArrayList<>();
        List<Integer> cfIds = new ArrayList<>();
        List<redmineconnector.model.CustomFieldDefinition> definitions = new ArrayList<>();

        Object root = parse(json);
        if (root instanceof Map) {
            Map<String, Object> rootMap = asMap(root);
            Map<String, Object> issue = asMap(rootMap.get("issue"));
            if (issue != null) {
                // Parse Statuses
                if (issue.containsKey("allowed_statuses")) {
                    List<Object> stList = asList(issue.get("allowed_statuses"));
                    if (stList != null) {
                        for (Object o : stList) {
                            Map<String, Object> m = asMap(o);
                            int id = asInt(m.get("id"));
                            String name = asString(m.get("name"));
                            if (id > 0 && !name.isEmpty())
                                statuses.add(new redmineconnector.model.SimpleEntity(id, name));
                        }
                    }
                }

                // Parse Available Custom Fields (IDs AND Definitions)
                if (issue.containsKey("custom_field_values")) {
                    // In /issues/new.json, custom_field_values is a map or list?
                    // Actually, Redmine returns "custom_fields" key at top level or inside
                    // available
                    // fields?
                    // Correction: /issues/new.json returns "custom_fields" list with full
                    // definitions!
                    // Let's look for "custom_fields" in the top root map or inside issue.
                }
            }

            // Redmine /issues/new.json structure: { "issue": { ... }, "custom_fields": [ {
            // "id": 1, ... } ] }
            // The "custom_fields" list contains the available fields for this specific
            // project/tracker context.
            if (rootMap.containsKey("custom_fields")) {
                List<Object> cfList = asList(rootMap.get("custom_fields"));
                if (cfList != null) {
                    for (Object o : cfList) {
                        Map<String, Object> m = asMap(o);
                        int id = asInt(m.get("id"));
                        cfIds.add(id);

                        // Parse full definition
                        String name = asString(m.get("name"));
                        String type = asString(m.get("field_format"));
                        boolean isRequired = asBoolean(m.get("is_required"));
                        redmineconnector.model.CustomFieldDefinition def = new redmineconnector.model.CustomFieldDefinition(
                                id, name, type, isRequired);

                        // Parse Possible Values
                        if (m.containsKey("possible_values")) {
                            List<Object> pv = asList(m.get("possible_values"));
                            if (pv != null) {
                                for (Object val : pv) {
                                    if (val instanceof Map) {
                                        // {"value": "X", "label": "Y"} or just {"value": "X"}?
                                        Map<String, Object> valMap = asMap(val);
                                        if (valMap.containsKey("value")) {
                                            def.possibleValues.add(asString(valMap.get("value")));
                                        }
                                    } else {
                                        def.possibleValues.add(asString(val));
                                    }
                                }
                            }
                        }

                        definitions.add(def);
                    }
                }
            }
        }
        return new redmineconnector.model.ContextMetadata(statuses, cfIds, definitions);
    }

    private static List<redmineconnector.model.CustomField> parseCustomFieldsList(List<Object> list) {
        List<redmineconnector.model.CustomField> result = new ArrayList<>();
        if (list == null)
            return result;

        for (Object o : list) {
            Map<String, Object> m = asMap(o);
            int id = asInt(m.get("id"));
            String name = asString(m.get("name"));
            // Value can be string, null, or array? Redmine usually sends string or array of
            // strings.
            // For now, handle as string. If array, it might be toString()'d which is not
            // ideal but prevents crash.
            Object valObj = m.get("value");
            String val = "";
            if (valObj instanceof List) {
                // Convert list to comma separated string? Or just take first?
                // Let's rely on standard toString for now or join
                val = valObj.toString();
            } else {
                val = asString(valObj);
            }
            result.add(new redmineconnector.model.CustomField(id, name, val));
        }
        return result;
    }

    private static boolean asBoolean(Object o) {
        if (o == null)
            return false;
        if (o instanceof Boolean)
            return (Boolean) o;
        return Boolean.parseBoolean(o.toString());
    }

    public static List<redmineconnector.model.CustomFieldDefinition> parseCustomFieldDefinitions(String json) {
        List<redmineconnector.model.CustomFieldDefinition> list = new ArrayList<>();
        Object root = parse(json);
        if (root instanceof Map) {
            Map<String, Object> rootMap = asMap(root);
            List<Object> customFields = asList(rootMap.get("custom_fields"));
            if (customFields != null) {
                for (Object o : customFields) {
                    Map<String, Object> m = asMap(o);
                    int id = asInt(m.get("id"));
                    String name = asString(m.get("name"));
                    String type = asString(m.get("field_format"));
                    boolean isRequired = asBoolean(m.get("is_required"));

                    redmineconnector.model.CustomFieldDefinition def = new redmineconnector.model.CustomFieldDefinition(
                            id, name, type, isRequired);

                    if (m.containsKey("possible_values")) {
                        List<Object> pv = asList(m.get("possible_values"));
                        if (pv != null) {
                            for (Object val : pv) {
                                if (val instanceof Map) {
                                    Map<String, Object> valMap = asMap(val);
                                    if (valMap.containsKey("value")) {
                                        def.possibleValues.add(asString(valMap.get("value")));
                                    }
                                } else {
                                    def.possibleValues.add(asString(val));
                                }
                            }
                        }
                    } else if (type.equals("list") && m.containsKey("possible_values")) {
                        // Fallback for simple string lists if above structure didn't match (unlikely
                        // but safe)
                        // The loop above actually handles generic objects, so this else might be
                        // redundant if we just cast content
                    }

                    // Parse Trackers (Essential for filtering allowed fields per tracker)
                    if (m.containsKey("trackers")) {
                        List<Object> trackers = asList(m.get("trackers"));
                        if (trackers != null) {
                            for (Object t : trackers) {
                                Map<String, Object> tMap = asMap(t);
                                int tid = asInt(tMap.get("id"));
                                if (tid > 0) {
                                    def.trackerIds.add(tid);
                                }
                            }
                        }
                    }

                    // Parse Projects (Essential for filtering allowed fields per project/client)
                    // If "is_filter" or "visible" logic is complex, this explicit list helps.
                    // Usually if this list is present, the field is restricted to these projects.
                    if (m.containsKey("projects")) {
                        List<Object> projs = asList(m.get("projects"));
                        if (projs != null) {
                            for (Object p : projs) {
                                Map<String, Object> pMap = asMap(p);
                                int pid = asInt(pMap.get("id"));
                                if (pid > 0) {
                                    def.projectIds.add(pid);
                                }
                            }
                        }
                        redmineconnector.util.LoggerUtil.logDebug("JsonParser", "Field " + def.name + " (" + def.id
                                + ") restricted to projects: " + def.projectIds);
                    } else {
                        redmineconnector.util.LoggerUtil.logDebug("JsonParser", "Field " + def.name + " (" + def.id
                                + ") has NO 'projects' key (Assumed Global)");
                    }

                    list.add(def);
                }
            }
        }
        return list;
    }
}
