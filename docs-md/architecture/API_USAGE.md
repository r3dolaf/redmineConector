# API Usage Guide

Complete guide for using the Redmine Connector service layer API.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Service Layer Overview](#service-layer-overview)
3. [Synchronous Operations](#synchronous-operations)
4. [Asynchronous Operations](#asynchronous-operations)
5. [Caching](#caching)
6. [Error Handling](#error-handling)
7. [Common Patterns](#common-patterns)
8. [Complete Examples](#complete-examples)

---

## Getting Started

### Basic Service Setup

```java
import redmineconnector.service.*;
import redmineconnector.model.*;

// Create base HTTP service
String url = "https://redmine.example.com";
String apiKey = "your_api_key_here";
Consumer<String> logger = System.out::println;

DataService httpService = new HttpDataService(url, apiKey, logger);
```

### With Caching

```java
// Add caching layer
CacheService cache = new SimpleCacheService();
DataService cachedService = new CachedDataService(httpService, cache);
```

### With Async Support

```java
// Add async wrapper
AsyncDataService asyncService = new AsyncDataService(cachedService);
```

### Recommended Setup (All Features)

```java
// Full stack: HTTP → Cache → Async
DataService httpService = new HttpDataService(url, apiKey, logger);
CacheService cache = new SimpleCacheService();
DataService cachedService = new CachedDataService(httpService, cache);
AsyncDataService asyncService = new AsyncDataService(cachedService);
```

---

## Service Layer Overview

### DataService Interface

All service implementations follow this interface:

```java
public interface DataService {
    // Tasks
    List<Task> fetchTasks(String pid, boolean closed, int limit) throws Exception;
    Task fetchTaskDetails(int id) throws Exception;
    int createTask(String pid, Task task) throws Exception;
    void updateTask(Task task) throws Exception;
    
    // Metadata
    List<SimpleEntity> fetchMetadata(String type, String pid) throws Exception;
    
    // Files
    String uploadFile(byte[] data, String contentType) throws Exception;
    byte[] downloadAttachment(Attachment att) throws Exception;
    
    // Time Tracking
    void logTime(int issueId, String date, double hours, int userId, int activityId, String comment) throws Exception;
    List<TimeEntry> fetchTimeEntries(String pid, String dateFrom, String dateTo) throws Exception;
    
    // Versions
    List<VersionDTO> fetchVersionsFull(String pid) throws Exception;
    void createVersion(String pid, String name, String status, String startDate, String dueDate) throws Exception;
    void updateVersion(int id, String name, String status, String startDate, String dueDate) throws Exception;
    void deleteVersion(int id) throws Exception;
    List<Task> fetchTasksByVersion(String pid, int versionId) throws Exception;
    List<Task> fetchClosedTasks(String pid, String dateFrom, String dateTo) throws Exception;
    
    // Wiki
    List<WikiPageDTO> fetchWikiPages(String projectId) throws Exception;
    WikiPageDTO fetchWikiPageContent(String projectId, String pageTitle) throws Exception;
    void createOrUpdateWikiPage(String projectId, String pageTitle, String content, String comment) throws Exception;
    void deleteWikiPage(String projectId, String pageTitle) throws Exception;
}
```

---

## Synchronous Operations

### Fetching Tasks

```java
// Fetch open tasks (limit 100)
List<Task> openTasks = service.fetchTasks("my-project", false, 100);

// Fetch all tasks (including closed)
List<Task> allTasks = service.fetchTasks("my-project", true, 0);

// Fetch specific number of tasks
List<Task> recentTasks = service.fetchTasks("my-project", false, 10);
```

### Task Details

```java
// Get full task details with attachments and history
Task task = service.fetchTaskDetails(123);

System.out.println("Subject: " + task.subject);
System.out.println("Status: " + task.statusName);
System.out.println("Attachments: " + task.attachments.size());
System.out.println("History: " + task.journals.size());
```

### Creating Tasks

```java
Task newTask = new Task();
newTask.subject = "New feature request";
newTask.description = "Detailed description here";
newTask.trackerId = 2; // Feature
newTask.priorityId = 3; // Normal
newTask.assignedToId = 5; // User ID

int taskId = service.createTask("my-project", newTask);
System.out.println("Created task #" + taskId);
```

### Updating Tasks

```java
Task task = service.fetchTaskDetails(123);
task.subject = "Updated title";
task.statusId = 3; // Resolved
task.notes = "Marking as resolved";

service.updateTask(task);
```

### Fetching Metadata

```java
// Get users (project members)
List<SimpleEntity> users = service.fetchMetadata("users", "my-project");

// Get trackers
List<SimpleEntity> trackers = service.fetchMetadata("trackers", "my-project");

// Get priorities (global)
List<SimpleEntity> priorities = service.fetchMetadata("priorities", null);

// Get statuses (global)
List<SimpleEntity> statuses = service.fetchMetadata("statuses", null);

// Get categories (project-specific)
List<SimpleEntity> categories = service.fetchMetadata("categories", "my-project");

// Get versions (project-specific)
List<SimpleEntity> versions = service.fetchMetadata("versions", "my-project");

// Get activities (for time tracking)
List<SimpleEntity> activities = service.fetchMetadata("activities", null);
```

---

## Asynchronous Operations

### Basic Async Pattern

```java
asyncService.fetchTasksAsync("my-project", false, 100)
    .thenAccept(tasks -> {
        // This runs in worker thread
        System.out.println("Fetched " + tasks.size() + " tasks");
    })
    .exceptionally(ex -> {
        System.err.println("Error: " + ex.getMessage());
        return null;
    });
```

### Updating UI from Async Operations

```java
import javax.swing.SwingUtilities;

asyncService.fetchTasksAsync("my-project", false, 100)
    .thenAccept(tasks -> {
        // Update UI on EDT
        SwingUtilities.invokeLater(() -> {
            taskTableModel.setTasks(tasks);
            statusLabel.setText("Loaded " + tasks.size() + " tasks");
        });
    })
    .exceptionally(ex -> {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame, 
                "Failed to load tasks: " + ex.getMessage(),
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        });
        return null;
    });
```

### Chaining Async Operations

```java
// Fetch task, then fetch its details
asyncService.fetchTasksAsync("my-project", false, 1)
    .thenCompose(tasks -> {
        if (tasks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return asyncService.fetchTaskDetailsAsync(tasks.get(0).id);
    })
    .thenAccept(taskDetails -> {
        if (taskDetails != null) {
            System.out.println("Task: " + taskDetails.subject);
            System.out.println("Description: " + taskDetails.description);
        }
    });
```

### Parallel Async Operations

```java
// Fetch multiple metadata types in parallel
CompletableFuture<List<SimpleEntity>> usersFuture = 
    asyncService.fetchMetadataAsync("users", "my-project");
    
CompletableFuture<List<SimpleEntity>> trackersFuture = 
    asyncService.fetchMetadataAsync("trackers", "my-project");
    
CompletableFuture<List<SimpleEntity>> prioritiesFuture = 
    asyncService.fetchMetadataAsync("priorities", null);

// Wait for all to complete
CompletableFuture.allOf(usersFuture, trackersFuture, prioritiesFuture)
    .thenRun(() -> {
        try {
            List<SimpleEntity> users = usersFuture.get();
            List<SimpleEntity> trackers = trackersFuture.get();
            List<SimpleEntity> priorities = prioritiesFuture.get();
            
            System.out.println("Loaded all metadata:");
            System.out.println("  Users: " + users.size());
            System.out.println("  Trackers: " + trackers.size());
            System.out.println("  Priorities: " + priorities.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
```

### Async with Timeout

```java
import java.util.concurrent.TimeUnit;

asyncService.fetchTasksAsync("my-project", false, 100)
    .orTimeout(10, TimeUnit.SECONDS)
    .thenAccept(tasks -> {
        System.out.println("Fetched " + tasks.size() + " tasks");
    })
    .exceptionally(ex -> {
        if (ex instanceof TimeoutException) {
            System.err.println("Request timed out after 10 seconds");
        } else {
            System.err.println("Error: " + ex.getMessage());
        }
        return null;
    });
```

---

## Caching

### Cache Configuration

```java
// Default cache (5 minute TTL)
CacheService cache = new SimpleCacheService();
DataService cachedService = new CachedDataService(httpService, cache);

// Custom TTL (10 minutes)
DataService cachedService = new CachedDataService(httpService, cache, 600);
```

### What Gets Cached?

| Operation | Cached? | Reason |
|-----------|---------|--------|
| `fetchMetadata()` | ✅ Yes | Metadata changes infrequently |
| `fetchVersionsFull()` | ✅ Yes | Versions change infrequently |
| `fetchWikiPages()` | ✅ Yes | Wiki index changes infrequently |
| `fetchWikiPageContent()` | ✅ Yes | Wiki content changes infrequently |
| `fetchTasks()` | ❌ No | Tasks change frequently |
| `fetchTaskDetails()` | ❌ No | Task details change frequently |
| `fetchTimeEntries()` | ❌ No | Time entries change frequently |

### Manual Cache Control

```java
CachedDataService cachedService = new CachedDataService(httpService, cache);

// Get cache instance
CacheService cache = cachedService.getCache();

// Check if cached
boolean hasCachedUsers = cache.contains("metadata:users:my-project");

// Invalidate specific entry
cache.invalidate("metadata:users:my-project");

// Invalidate all users caches
cache.invalidatePattern("metadata:users:*");

// Clear all cache
cache.invalidateAll();

// Check cache size
int entries = cache.size();
```

### Cache Invalidation on Mutations

Cache is automatically invalidated when you modify data:

```java
// Creating a task invalidates metadata cache for that project
int taskId = cachedService.createTask("my-project", newTask);
// Next fetchMetadata() will hit HTTP (cache invalidated)

// Creating/updating/deleting versions invalidates version cache
cachedService.createVersion("my-project", "v1.0", "open", null, "2025-12-31");
// Next fetchVersionsFull() will hit HTTP

// Updating wiki invalidates wiki cache
cachedService.createOrUpdateWikiPage("my-project", "HomePage", "content", "Updated");
// Next fetchWikiPages() will hit HTTP
```

---

## Error Handling

### Try-Catch Pattern

```java
try {
    List<Task> tasks = service.fetchTasks("my-project", false, 100);
    // Process tasks
} catch (Exception e) {
    System.err.println("Failed to fetch tasks: " + e.getMessage());
    e.printStackTrace();
}
```

### Async Error Handling

```java
asyncService.fetchTasksAsync("my-project", false, 100)
    .thenAccept(tasks -> {
        // Success path
    })
    .exceptionally(ex -> {
        // Error path
        Throwable cause = ex.getCause(); // Unwrap RuntimeException
        System.err.println("Error: " + cause.getMessage());
        return null;
    });
```

### Graceful Degradation

```java
List<SimpleEntity> users;
try {
    users = service.fetchMetadata("users", "my-project");
} catch (Exception e) {
    System.err.println("Failed to load users: " + e.getMessage());
    users = new ArrayList<>(); // Use empty list as fallback
}
```

---

## Common Patterns

### Pattern 1: Load and Display Tasks

```java
public void loadTasks() {
    asyncService.fetchTasksAsync(projectId, showClosed, 100)
        .thenAccept(tasks -> SwingUtilities.invokeLater(() -> {
            taskTableModel.setTasks(tasks);
            statusLabel.setText("Loaded " + tasks.size() + " tasks");
        }))
        .exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Error: " + ex.getMessage(), 
                    "Load Failed", 
                    JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
}
```

### Pattern 2: Create Task with Validation

```java
public void createTask(String subject, String description) {
    if (subject == null || subject.trim().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Subject is required");
        return;
    }
    
    Task task = new Task();
    task.subject = subject;
    task.description = description;
    task.trackerId = selectedTrackerId;
    task.priorityId = selectedPriorityId;
    
    asyncService.createTaskAsync(projectId, task)
        .thenAccept(taskId -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Created task #" + taskId);
            loadTasks(); // Refresh list
        }))
        .exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Failed to create task: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
}
```

### Pattern 3: Download Task with Attachments

```java
public void downloadTask(int taskId, File targetDir) {
    asyncService.fetchTaskDetailsAsync(taskId)
        .thenCompose(task -> {
            // Download all attachments in parallel
            List<CompletableFuture<Void>> downloads = task.attachments.stream()
                .map(att -> asyncService.downloadAttachmentAsync(att)
                    .thenAccept(bytes -> {
                        File file = new File(targetDir, att.filename);
                        Files.write(file.toPath(), bytes);
                    }))
                .collect(Collectors.toList());
            
            return CompletableFuture.allOf(downloads.toArray(new CompletableFuture[0]));
        })
        .thenRun(() -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Download complete");
        }))
        .exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Download failed: " + ex.getMessage());
            });
            return null;
        });
}
```

---

## Complete Examples

### Example 1: Task Manager Application

```java
public class TaskManager {
    private final AsyncDataService asyncService;
    private final String projectId;
    
    public TaskManager(String url, String apiKey, String projectId) {
        DataService httpService = new HttpDataService(url, apiKey, System.out::println);
        CacheService cache = new SimpleCacheService();
        DataService cachedService = new CachedDataService(httpService, cache);
        this.asyncService = new AsyncDataService(cachedService);
        this.projectId = projectId;
    }
    
    public CompletableFuture<Void> displayTasks() {
        return asyncService.fetchTasksAsync(projectId, false, 50)
            .thenAccept(tasks -> {
                System.out.println("\n=== Open Tasks ===");
                tasks.forEach(task -> {
                    System.out.printf("#%d: %s [%s]\n", 
                        task.id, task.subject, task.statusName);
                });
            });
    }
    
    public CompletableFuture<Integer> createBugReport(String subject, String description) {
        Task task = new Task();
        task.subject = subject;
        task.description = description;
        task.trackerId = 1; // Bug
        task.priorityId = 4; // High
        
        return asyncService.createTaskAsync(projectId, task);
    }
    
    public static void main(String[] args) {
        TaskManager manager = new TaskManager(
            "https://redmine.example.com",
            "your_api_key",
            "my-project"
        );
        
        manager.displayTasks()
            .thenCompose(v -> manager.createBugReport(
                "Critical bug",
                "Application crashes on startup"))
            .thenAccept(taskId -> {
                System.out.println("Created bug report #" + taskId);
            })
            .join(); // Wait for completion
    }
}
```

### Example 2: Time Tracking Tool

```java
public class TimeTracker {
    private final AsyncDataService asyncService;
    
    public TimeTracker(AsyncDataService asyncService) {
        this.asyncService = asyncService;
    }
    
    public CompletableFuture<Void> logWork(int taskId, double hours, String comment) {
        String today = LocalDate.now().toString(); // YYYY-MM-DD
        int userId = getCurrentUserId();
        int activityId = 9; // Development
        
        return asyncService.logTimeAsync(taskId, today, hours, userId, activityId, comment)
            .thenRun(() -> System.out.println("Logged " + hours + " hours on task #" + taskId));
    }
    
    public CompletableFuture<Void> showWeeklyReport(String projectId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        
        return asyncService.fetchTimeEntriesAsync(
                projectId, 
                weekAgo.toString(), 
                today.toString())
            .thenAccept(entries -> {
                double totalHours = entries.stream()
                    .mapToDouble(e -> e.hours)
                    .sum();
                
                System.out.println("\n=== Weekly Time Report ===");
                System.out.println("Total hours: " + totalHours);
                entries.forEach(e -> {
                    System.out.printf("%s: %.2fh - %s\n", 
                        e.spentOn, e.hours, e.comments);
                });
            });
    }
}
```

---

## Best Practices

1. **Always use async for UI applications** - Prevents UI freezing
2. **Cache metadata** - Reduces HTTP requests significantly
3. **Handle errors gracefully** - Provide user feedback
4. **Use SwingUtilities.invokeLater()** - For UI updates from worker threads
5. **Set timeouts** - Prevent hanging on slow networks
6. **Validate input** - Before making API calls
7. **Log operations** - For debugging and monitoring

---

## Performance Tips

1. **Batch operations** - Fetch multiple items at once
2. **Use caching** - Especially for metadata
3. **Parallel requests** - Use `CompletableFuture.allOf()`
4. **Limit results** - Don't fetch more than needed
5. **Reuse services** - Don't create new instances for each operation

---

For architecture details, see [ARCHITECTURE.md](ARCHITECTURE.md).
