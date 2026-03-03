package redmineconnector.ui;

import java.util.List;
import java.util.stream.Collectors;
import redmineconnector.model.SimpleEntity;
import redmineconnector.model.Task;

/**
 * Handles Redmine metadata (users, trackers, statuses, priority, etc.)
 * for a specific connection.
 */
public class MetadataManager {
    private List<SimpleEntity> users;
    private List<SimpleEntity> trackers;
    private List<SimpleEntity> priorities;
    private List<SimpleEntity> statuses;
    private List<SimpleEntity> categories;
    private List<SimpleEntity> versions;
    private List<SimpleEntity> activities;
    private SimpleEntity currentUser;
    private boolean loaded = false;

    public void setMetadata(List<SimpleEntity> users, List<SimpleEntity> trackers,
            List<SimpleEntity> priorities, List<SimpleEntity> statuses,
            List<SimpleEntity> categories, List<SimpleEntity> versions,
            List<SimpleEntity> activities) {
        this.users = users;
        this.trackers = trackers;
        this.priorities = priorities;
        this.statuses = statuses;
        this.categories = categories;
        this.versions = versions;
        this.activities = activities;
        this.loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public List<SimpleEntity> getUsers() {
        return users;
    }

    public SimpleEntity getUserById(int id) {
        if (users == null)
            return null;
        for (SimpleEntity u : users) {
            if (u.id == id)
                return u;
        }
        return null;
    }

    public List<SimpleEntity> getTrackers() {
        return trackers;
    }

    public List<SimpleEntity> getPriorities() {
        return priorities;
    }

    public List<SimpleEntity> getStatuses() {
        return statuses;
    }

    public List<SimpleEntity> getCategories() {
        return categories;
    }

    public List<SimpleEntity> getVersions() {
        return versions;
    }

    public void enrichMetadataFromTasks(List<Task> tasks) {
        // Add missing categories
        java.util.Set<Integer> catIds = categories.stream().map(c -> c.id).collect(java.util.stream.Collectors.toSet());
        for (Task t : tasks) {
            if (t.categoryId > 0 && t.category != null && !t.category.isEmpty() && !catIds.contains(t.categoryId)) {
                categories.add(new SimpleEntity(t.categoryId, t.category));
                catIds.add(t.categoryId);
            }
        }
        categories.sort(java.util.Comparator.comparing(c -> c.name));

        // Add missing statuses
        java.util.Set<Integer> statIds = statuses.stream().map(s -> s.id).collect(java.util.stream.Collectors.toSet());
        for (Task t : tasks) {
            if (t.statusId > 0 && t.status != null && !t.status.isEmpty() && !statIds.contains(t.statusId)) {
                statuses.add(new SimpleEntity(t.statusId, t.status));
                statIds.add(t.statusId);
            }
        }
        statuses.sort(java.util.Comparator.comparing(s -> s.name));

        // Sort others
        if (versions != null)
            versions.sort(java.util.Comparator.comparing(v -> v.name));
        if (activities != null)
            activities.sort(java.util.Comparator.comparing(a -> a.name));
        if (users != null)
            users.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
    }

    public List<SimpleEntity> getActiveStatuses(List<Task> tasks) {
        List<SimpleEntity> activeStatuses = new java.util.ArrayList<>();
        java.util.Set<Integer> activeIds = new java.util.HashSet<>();
        for (Task t : tasks) {
            if (t.statusId > 0 && activeIds.add(t.statusId)) {
                activeStatuses.add(new SimpleEntity(t.statusId, t.status));
            }
        }
        activeStatuses.sort(java.util.Comparator.comparing(s -> s.name));
        return activeStatuses;
    }

    public List<SimpleEntity> getActivities() {
        return activities;
    }

    public SimpleEntity getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(SimpleEntity currentUser) {
        this.currentUser = currentUser;
    }

    public List<String> getUserNames() {
        if (users == null)
            return java.util.Collections.emptyList();
        return users.stream().map(e -> e.name).collect(Collectors.toList());
    }

    public List<String> getTrackerNames() {
        if (trackers == null)
            return java.util.Collections.emptyList();
        return trackers.stream().map(e -> e.name).collect(Collectors.toList());
    }

    public List<String> getStatusNames() {
        if (statuses == null)
            return java.util.Collections.emptyList();
        return statuses.stream().map(e -> e.name).collect(Collectors.toList());
    }

    public List<String> getCategoryNames() {
        if (categories == null)
            return java.util.Collections.emptyList();
        return categories.stream().map(e -> e.name).collect(Collectors.toList());
    }

    public List<String> getPriorityNames() {
        if (priorities == null)
            return java.util.Collections.emptyList();
        return priorities.stream().map(e -> e.name).collect(Collectors.toList());
    }
}
