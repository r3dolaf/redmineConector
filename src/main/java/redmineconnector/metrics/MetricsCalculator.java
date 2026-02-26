package redmineconnector.metrics;

import redmineconnector.model.Task;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calculates various metrics from task lists.
 * Provides data for charts and reports.
 */
public class MetricsCalculator {

    /**
     * Counts tasks by status.
     */
    public static Map<String, Integer> countByStatus(List<Task> tasks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Task task : tasks) {
            String status = task.status != null ? task.status : "Sin Estado";
            counts.put(status, counts.getOrDefault(status, 0) + 1);
        }
        return counts;
    }

    /**
     * Counts tasks by priority.
     */
    public static Map<String, Integer> countByPriority(List<Task> tasks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Task task : tasks) {
            String priority = task.priority != null ? task.priority : "Sin Prioridad";
            counts.put(priority, counts.getOrDefault(priority, 0) + 1);
        }
        return counts;
    }

    /**
     * Counts tasks by assigned user.
     */
    public static Map<String, Integer> countByAssignedUser(List<Task> tasks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Task task : tasks) {
            String user = task.assignedTo != null && !task.assignedTo.isEmpty()
                    ? task.assignedTo
                    : "Sin Asignar";
            counts.put(user, counts.getOrDefault(user, 0) + 1);
        }
        return counts;
    }

    /**
     * Counts tasks by tracker (type).
     */
    public static Map<String, Integer> countByTracker(List<Task> tasks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Task task : tasks) {
            String tracker = task.tracker != null ? task.tracker : "Sin Tipo";
            counts.put(tracker, counts.getOrDefault(tracker, 0) + 1);
        }
        return counts;
    }

    /**
     * Calculates total hours spent.
     */
    public static double getTotalHoursSpent(List<Task> tasks) {
        return tasks.stream()
                .mapToDouble(t -> t.spentHours)
                .sum();
    }

    /**
     * Calculates average completion percentage.
     */
    public static double getAverageCompletion(List<Task> tasks) {
        if (tasks.isEmpty())
            return 0.0;
        return tasks.stream()
                .mapToInt(t -> t.doneRatio)
                .average()
                .orElse(0.0);
    }

    /**
     * Gets top N users by task count.
     */
    public static List<Map.Entry<String, Integer>> getTopUsers(List<Task> tasks, int topN) {
        return countByAssignedUser(tasks).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * Counts completed vs pending tasks.
     */
    public static Map<String, Integer> getCompletionStats(List<Task> tasks) {
        Map<String, Integer> stats = new HashMap<>();
        int completed = 0;
        int pending = 0;

        for (Task task : tasks) {
            if (task.doneRatio >= 100) {
                completed++;
            } else {
                pending++;
            }
        }

        stats.put("Completadas", completed);
        stats.put("Pendientes", pending);
        return stats;
    }

    /**
     * Groups tasks by creation date (day).
     */
    public static Map<String, Integer> countByCreationDate(List<Task> tasks) {
        Map<String, Integer> counts = new HashMap<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        for (Task task : tasks) {
            if (task.createdOn != null) {
                String date = sdf.format(task.createdOn);
                counts.put(date, counts.getOrDefault(date, 0) + 1);
            }
        }
        return counts;
    }
}
