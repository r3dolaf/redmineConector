package redmineconnector.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redmineconnector.model.SimpleEntity;
import redmineconnector.util.JsonParser;
import redmineconnector.util.LoggerUtil;

public class HeuristicManager {

    private static final String FILENAME = ".redmine_heuristic.json";

    private File getFile() {
        return new File(System.getProperty("user.home"), FILENAME);
    }

    public Map<Integer, List<SimpleEntity>> load() {
        Map<Integer, List<SimpleEntity>> result = new HashMap<>();
        File f = getFile();
        if (!f.exists()) {
            return result;
        }

        try {
            String json = new String(Files.readAllBytes(f.toPath()), "UTF-8");
            Object root = JsonParser.parse(json);
            if (root instanceof Map) {
                Map<String, Object> rootMap = (Map<String, Object>) root;
                for (Map.Entry<String, Object> entry : rootMap.entrySet()) {
                    try {
                        Integer trackerId = Integer.parseInt(entry.getKey());
                        List<SimpleEntity> statuses = new ArrayList<>();

                        if (entry.getValue() instanceof List) {
                            List<Object> list = (List<Object>) entry.getValue();
                            for (Object o : list) {
                                if (o instanceof Map) {
                                    Map<String, Object> map = (Map<String, Object>) o;
                                    SimpleEntity s = new SimpleEntity();
                                    if (map.containsKey("id"))
                                        s.id = ((Number) map.get("id")).intValue();
                                    if (map.containsKey("name"))
                                        s.name = (String) map.get("name");
                                    statuses.add(s);
                                }
                            }
                        }
                        result.put(trackerId, statuses);
                    } catch (NumberFormatException e) {
                        // ignore non-integer keys
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.logError("HeuristicManager", "Error loading heuristic: " + e.getMessage());
        }
        return result;
    }

    public void save(Map<Integer, List<SimpleEntity>> data) {
        if (data == null)
            return;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<Integer, List<SimpleEntity>> entry : data.entrySet()) {
                sb.append("  \"").append(entry.getKey()).append("\": [\n");
                List<SimpleEntity> list = entry.getValue();
                for (int j = 0; j < list.size(); j++) {
                    SimpleEntity s = list.get(j);
                    sb.append("    { \"id\": ").append(s.id).append(", \"name\": \"").append(escape(s.name))
                            .append("\" }");
                    if (j < list.size() - 1)
                        sb.append(",");
                    sb.append("\n");
                }
                sb.append("  ]");
                if (i < data.size() - 1)
                    sb.append(",");
                sb.append("\n");
                i++;
            }
            sb.append("}");

            Files.write(getFile().toPath(), sb.toString().getBytes("UTF-8"),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (Exception e) {
            LoggerUtil.logError("HeuristicManager", "Error saving heuristic: " + e.getMessage());
        }
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
