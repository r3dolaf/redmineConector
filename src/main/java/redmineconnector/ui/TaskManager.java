package redmineconnector.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import redmineconnector.model.Task;

/**
 * Handles the collection of tasks and their identification/tracking.
 */
public class TaskManager {
    private List<Task> currentTasks = new ArrayList<>();
    private List<Task> allLoadedTasks = new ArrayList<>();
    private List<Task> epicTasks = new ArrayList<>();
    private final Set<Integer> knownTaskIds = new HashSet<>();
    private final Set<Integer> pinnedTaskIds = new HashSet<>();

    public boolean isEpic(Task t) {
        if (t == null)
            return false;
        boolean isEpicTracker = t.tracker != null && (t.tracker.toLowerCase().contains("épica")
                || t.tracker.toLowerCase().contains("epica")
                || t.tracker.toLowerCase().contains("epic"));
        boolean isEpicSubject = t.subject != null && (t.subject.toLowerCase().contains("épica"));
        return isEpicTracker || isEpicSubject;
    }

    public void classifyTasks(List<Task> rawTasks, boolean includeEpicsInMainList) {
        this.allLoadedTasks = rawTasks;
        this.epicTasks = new java.util.ArrayList<>();
        List<Task> effective = new java.util.ArrayList<>();

        for (Task t : rawTasks) {
            boolean epic = isEpic(t);
            if (epic) {
                epicTasks.add(t);
            }
            if (includeEpicsInMainList || !epic) {
                effective.add(t);
            }
        }
        this.currentTasks = effective;
    }

    public void setTasks(List<Task> tasks) {
        this.allLoadedTasks = tasks;
        this.currentTasks = new ArrayList<>(tasks);
    }

    public List<Task> getCurrentTasks() {
        return currentTasks;
    }

    public List<Task> getAllLoadedTasks() {
        return allLoadedTasks;
    }

    public List<Task> getEpicTasks() {
        return epicTasks;
    }

    public Set<Integer> getKnownTaskIds() {
        return knownTaskIds;
    }

    public Set<Integer> getPinnedTaskIds() {
        return pinnedTaskIds;
    }

    public void setEpicTasks(List<Task> epics) {
        this.epicTasks = epics;
    }

    public boolean isPinned(int taskId) {
        return pinnedTaskIds.contains(taskId);
    }

    public void togglePin(int taskId) {
        if (pinnedTaskIds.contains(taskId)) {
            pinnedTaskIds.remove(taskId);
        } else {
            pinnedTaskIds.add(taskId);
        }
    }

    public void addKnownTask(int id) {
        knownTaskIds.add(id);
    }

    public List<Task> findNewTasks(List<Task> latest, boolean firstLoad) {
        if (firstLoad) {
            latest.forEach(t -> knownTaskIds.add(t.id));
            return new java.util.ArrayList<>();
        }
        List<Task> news = new java.util.ArrayList<>();
        for (Task t : latest) {
            if (!knownTaskIds.contains(t.id)) {
                news.add(t);
                knownTaskIds.add(t.id);
            }
        }
        return news;
    }

    public void clearTasks() {
        allLoadedTasks.clear();
        currentTasks.clear();
    }
}
