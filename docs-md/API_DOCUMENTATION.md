# API Documentation - DataService Interface

**Version:** 9.0  
**Package:** `redmineconnector.service`  
**Type:** Interface

---

## Overview

`DataService` is the core interface for all Redmine API interactions in the application. It provides a comprehensive set of methods for task management, version control, wiki operations, and time tracking.

### Key Characteristics

- **Thread-Safe:** Implementations must support concurrent access
- **Synchronous:** All methods are blocking (use `AsyncDataService` wrapper for async operations)
- **Exception-Based:** All methods throw `Exception` for maximum flexibility
- **Stateless:** No maintained connection state (each call is independent)

### Implementations

| Implementation | Description | Package |
|----------------|-------------|---------|
| `RedmineDataServiceImpl` | Production implementation using Redmine REST API | `redmineconnector.service.impl` |
| `AsyncDataService` | Async wrapper using `CompletableFuture` | `redmineconnector.service` |
| `MockDataService` | Test implementation with fake data | `redmineconnector.service.mock` (tests only) |

---

## Quick Start

### Basic Usage

```java
// 1. Create service instance
DataService service = new RedmineDataServiceImpl(
    "https://redmine.example.com",
    "your-api-key-here"
);

// 2. Fetch tasks
List<Task> tasks = service.fetchTasks("project-id", false, 100);

// 3. Get task details
Task task = service.fetchTaskDetails(123);

// 4. Update task
task.status = "In Progress";
task.notes = "Started working on this";
service.updateTask(task);

// 5. Close connection (cleanup)
service.shutdown();
```

### Async Usage (Recommended for UI)

```java
AsyncDataService asyncService = new AsyncDataService(syncService);

asyncService.fetchTasksAsync("project-id", false, 100)
    .thenAccept(tasks -> SwingUtilities.invokeLater(() -> {
        // Update UI on EDT
        updateTaskTable(tasks);
    }))
    .exceptionally(ex -> {
        LoggerUtil.logError("Controller", "Failed to load tasks", (Exception) ex);
        return null;
    });
```

---

## Methods by Category

### 📋 Task Management

#### fetchTasks
```java
List<Task> fetchTasks(String pid, boolean closed, int limit) throws Exception
```

Fetches tasks from a project with filtering.

**Parameters:**
- `pid` - Project ID or identifier (e.g., "my-project" or "123")
- `closed` - Include closed tasks (true) or only open (false)
- `limit` - Maximum tasks to return (API typically limits to 100)

**Returns:**
- List of tasks with basic fields populated

**Throws:**
- `Exception` if project doesn't exist, API key is invalid, or network failure

**Example:**
```java
// Get first 50 open tasks from project
List<Task> openTasks = service.fetchTasks("crm-system", false, 50);

// Get all tasks including closed (up to API limit)
List<Task> allTasks = service.fetchTasks("crm-system", true, 100);
```

**Performance:**
- Typical response time: 200ms - 2s
- Depends on: task count, network latency, server load

---

#### fetchTaskDetails
```java
Task fetchTaskDetails(int id) throws Exception
```

Retrieves complete information for a single task.

**Parameters:**
- `id` - Unique task ID

**Returns:**
- Task object with all fields, including:
  - Full description
  - Journal entries (history/notes)
  - Attachments
  - Custom fields
  - Related tasks

**Throws:**
- `Exception` if task doesn't exist or access is denied

**Example:**
```java
Task fullTask = service.fetchTaskDetails(12345);

System.out.println("Description: " + fullTask.description);
System.out.println("History entries: " + fullTask.journals.size());
System.out.println("Attachments: " + fullTask.attachments.size());
```

**Note:** This is **slower** than `fetchTasks()` because it retrieves more data. Use sparingly (e.g., on-demand when user opens a task).

---

#### createTask
```java
int createTask(String pid, Task task) throws Exception
```

Creates a new task in the specified project.

**Parameters:**
- `pid` - Project ID where task will be created
- `task` - Task object with fields to set

**Required Task Fields:**
- `subject` (String) - Task title
- `trackerId` (int) - Type of task (Bug=1, Feature=2, etc.)

**Optional Task Fields:**
- `description` - Detailed description
- `statusId` - Initial status (defaults to "New")
- `priorityId` - Priority level
- `assignedToId` - User ID to assign
- `categoryId` - Task category
- `versionId` - Target version/milestone
- `parentId` - Parent task ID (for subtasks)
- `estimatedHours` - Time estimate
- `doneRatio` - Completion percentage (0-100)

**Returns:**
- ID of the newly created task

**Throws:**
- `Exception` if validation fails (e.g., missing subject, invalid tracker)

**Example:**
```java
Task newTask = new Task();
newTask.subject = "Implement login feature";
newTask.description = "Users should be able to log in with email/password";
newTask.trackerId = 2; // Feature
newTask.priorityId = 3; // High
newTask.assignedToId = 42; // John Doe

int taskId = service.createTask("web-app", newTask);
System.out.println("Created task #" + taskId);
```

---

#### updateTask
```java
void updateTask(Task task) throws Exception
```

Updates an existing task with modified values.

**Parameters:**
- `task` - Task object with `id` set and fields to update

**Update Behavior:**
- Only non-null fields are updated
- To add a note, set `task.notes` field
- Status changes must comply with workflow rules

**Throws:**
- `Exception` if task doesn't exist, validation fails, or workflow prevents update

**Example:**
```java
// Simple status update
Task task = service.fetchTaskDetails(123);
task.statusId = 3; // In Progress
task.notes = "Started working on this feature";
service.updateTask(task);

// Bulk field update
task.assignedToId = 99;
task.doneRatio = 50;
task.estimatedHours = 8.0;
service.updateTask(task);
```

**Workflow Validation:**
```java
// ⚠️ This might fail if workflow doesn't allow Bug → Closed directly
Task bug = service.fetchTaskDetails(456);
bug.statusId = 5; // Closed
service.updateTask(bug); // May throw exception

// ✅ Better: Check allowed statuses first
List<SimpleEntity> allowedStatuses = service.fetchAllowedStatuses(
    "project-id", bug.trackerId, bug.id
);
```

---

#### fetchTasksByIds
```java
List<Task> fetchTasksByIds(List<Integer> ids) throws Exception
```

Bulk-fetches full details for multiple tasks by ID.

**Parameters:**
- `ids` - List of task IDs to fetch

**Returns:**
- List of tasks with full details
- Tasks that don't exist or aren't accessible are silently omitted

**Throws:**
- `Exception` on API failure

**Example:**
```java
List<Integer> taskIds = Arrays.asList(101, 102, 103, 104, 105);
List<Task> tasks = service.fetchTasksByIds(taskIds);

System.out.println("Requested: " + taskIds.size());
System.out.println("Retrieved: " + tasks.size()); // May be less if some tasks don't exist
```

**Performance:**
- Much faster than calling `fetchTaskDetails()` in a loop
- Recommended for loading details of known task sets

**Use Cases:**
- Loading twin tasks across servers
- Fetching tasks in a specific version
- Refreshing a subset of visible tasks

---

### 🏷️ Metadata

#### fetchMetadata
```java
List<SimpleEntity> fetchMetadata(String type, String pid) throws Exception
```

Retrieves project metadata like statuses, priorities, users, etc.

**Supported Types:**

| Type | Description | Example Values |
|------|-------------|----------------|
| `"statuses"` | Task statuses | New, In Progress, Resolved, Closed |
| `"priorities"` | Priority levels | Low, Normal, High, Urgent |
| `"trackers"` | Task types | Bug, Feature, Support |
| `"users"` | Project members | John Doe, Jane Smith |
| `"categories"` | Task categories | Frontend, Backend, Database |
| `"versions"` | Versions/milestones | v1.0, v1.1, Sprint 10 |

**Parameters:**
- `type` - Metadata type (case-sensitive)
- `pid` - Project ID

**Returns:**
- List of `SimpleEntity` objects with `id` and `name`

**Example:**
```java
// Get all available statuses
List<SimpleEntity> statuses = service.fetchMetadata("statuses", "project-id");
for (SimpleEntity status : statuses) {
    System.out.println(status.id + ": " + status.name);
}
// Output:
//   1: New
//   2: In Progress
//   3: Resolved
//   5: Closed

// Get project members for assignment dropdown
List<SimpleEntity> users = service.fetchMetadata("users", "project-id");
ComboBoxModel<SimpleEntity> userModel = new DefaultComboBoxModel<>(users.toArray(new SimpleEntity[0]));
```

---

#### fetchAllowedStatuses
```java
List<SimpleEntity> fetchAllowedStatuses(String pid, int trackerId, int issueId) throws Exception
```

Gets valid status transitions based on workflow rules.

**Parameters:**
- `pid` - Project ID
- `trackerId` - Task type/tracker ID
- `issueId` - Task ID (use 0 for new tasks)

**Returns:**
- List of statuses that can be transitioned to

**Example:**
```java
// For existing task
List<SimpleEntity> allowed = service.fetchAllowedStatuses("project", 1, 123);
// Returns: [In Progress, Resolved] (if current status is "New")

// For new task
List<SimpleEntity> initialStatuses = service.fetchAllowedStatuses("project", 1, 0);
// Returns: [New] (typically only one status for new tasks)
```

**Use Case:**
```java
// Build status dropdown with only valid options
Task task = getCurrentTask();
List<SimpleEntity> statuses = service.fetchAllowedStatuses(
    projectId, 
    task.trackerId,
    task.id
);

JComboBox<SimpleEntity> statusCombo = new JComboBox<>(statuses.toArray(new SimpleEntity[0]));
```

---

#### fetchCurrentUser
```java
SimpleEntity fetchCurrentUser() throws Exception
```

Identifies the authenticated user.

**Returns:**
- `SimpleEntity` with user ID and name

**Throws:**
- `Exception` if API key is invalid

**Example:**
```java
SimpleEntity currentUser = service.fetchCurrentUser();
System.out.println("Logged in as: " + currentUser.name + " (ID: " + currentUser.id + ")");

// Auto-assign new tasks to current user
Task task = new Task();
task.subject = "My task";
task.assignedToId = currentUser.id;
```

**Caching Recommendation:**
```java
// Cache user to avoid repeated API calls
private SimpleEntity cachedUser;

public SimpleEntity getCurrentUser() throws Exception {
    if (cachedUser == null) {
        cachedUser = service.fetchCurrentUser();
    }
    return cachedUser;
}
```

---

### 📎 Attachments

#### uploadFile
```java
String uploadFile(byte[] data, String contentType) throws Exception
```

Uploads file content and returns a token for attaching to tasks/wiki.

**Parameters:**
- `data` - Raw file bytes
- `contentType` - MIME type (e.g., "image/png", "application/pdf", "text/plain")

**Returns:**
- Upload token (temporary identifier)

**Throws:**
- `Exception` if upload fails or file size exceeds limit

**Example:**
```java
// Read file
File file = new File("screenshot.png");
byte[] fileBytes = Files.readAllBytes(file.toPath());

// Upload
String token = service.uploadFile(fileBytes, "image/png");

// Attach to task
Task task = service.fetchTaskDetails(123);
task.uploads = Arrays.asList(
    new Upload(token, file.getName(), "image/png")
);
service.updateTask(task);
```

**Two-Step Process:**
1. `uploadFile()` → Get token
2. Use token in task/wiki update to create attachment

---

#### downloadAttachment
```java
byte[] downloadAttachment(Attachment att) throws Exception
```

Downloads attachment content.

**Parameters:**
- `att` - Attachment object with content URL

**Returns:**
- Raw file bytes

**Example:**
```java
Task task = service.fetchTaskDetails(123);
for (Attachment att : task.attachments) {
    System.out.println("Downloading: " + att.filename);
    byte[] content = service.downloadAttachment(att);
    
    // Save to file
    File outputFile = new File("downloads/" + att.filename);
    Files.write(outputFile.toPath(), content);
}
```

---

### ⏱️ Time Tracking

#### logTime
```java
void logTime(int issueId, String date, double hours, int userId, int activityId, String comment) throws Exception
```

Logs time spent on a task.

**Parameters:**
- `issueId` - Task ID
- `date` - Date of work (format: "yyyy-MM-dd")
- `hours` - Time spent (decimal, e.g., 2.5 = 2h 30min)
- `userId` - User who performed the work
- `activityId` - Activity type (Development=9, Design=10, Testing=11, etc.)
- `comment` - Description of work (can be null)

**Example:**
```java
// Log 3.5 hours of development work today
String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
service.logTime(
    123,                    // task ID
    today,                  // date
    3.5,                    // hours
    getCurrentUser().id,    //user
    9,                      // Development activity
    "Implemented REST API endpoints"
);
```

---

#### fetchTimeEntries
```java
List<TimeEntry> fetchTimeEntries(String pid, String dateFrom, String dateTo) throws Exception
```

Retrieves time entries for reporting.

**Parameters:**
- `pid` - Project ID
- `dateFrom` - Start date inclusive (format: "yyyy-MM-dd")
- `dateTo` - End date inclusive (format: "yyyy-MM-dd")

**Returns:**
- List of `TimeEntry` objects with:
  - `user` - Who logged the time
  - `issueId` - Task ID
  - `hours` - Time spent
  - `spentOn` - Date of work
  - `activity` - Activity type
  - `comments` - Work description

**Example:**
```java
// Get time entries for current month
Calendar cal = Calendar.getInstance();
cal.set(Calendar.DAY_OF_MONTH, 1);
String monthStart = sdf.format(cal.getTime());
cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
String monthEnd = sdf.format(cal.getTime());

List<TimeEntry> entries = service.fetchTimeEntries("project-id", monthStart, monthEnd);

// Calculate total hours per user
Map<String, Double> hoursPerUser = new HashMap<>();
for (TimeEntry entry : entries) {
    hoursPerUser.merge(entry.user, entry.hours, Double::sum);
}
```

---

### 📦 Version Management

#### fetchVersionsFull
```java
List<VersionDTO> fetchVersionsFull(String pid) throws Exception
```

Gets all versions/milestones with details.

**Example:**
```java
List<VersionDTO> versions = service.fetchVersionsFull("project-id");
for (VersionDTO ver : versions) {
    System.out.printf("%s [%s] Due: %s%n", ver.name, ver.status, ver.dueDate);
}
```

---

#### createVersion / updateVersion / deleteVersion

See Javadoc in `DataService.java` for complete documentation.

---

### 📚 Wiki Management

#### fetchWikiPages / fetchWikiPageContent / createOrUpdateWikiPage

See Javadoc in `DataService.java` for complete documentation.

---

## Error Handling

### Common Exceptions

| Exception Type | Cause | Solution |
|----------------|-------|----------|
| `ConnectException` | Network failure | Check internet connection, server URL |
| `SocketTimeoutException` | Slow server | Increase timeout in `HttpUtils` |
| `FileNotFoundException` (HTTP 404) | Resource doesn't exist | Verify task/project ID |
| `IOException` (HTTP 401) | Invalid API key | Check configuration |
| `IOException` (HTTP 403) | Access denied | Check permissions |
| `IOException` (HTTP 422) | Validation error | Check required fields |

### Recommended Pattern

```java
try {
    List<Task> tasks = service.fetchTasks("project", false, 100);
    // process tasks
} catch (Exception e) {
    LoggerUtil.logError("Controller", "Failed to fetch tasks", e);
    
    if (e.getMessage().contains("404")) {
        showError("Project not found. Check configuration.");
    } else if (e.getMessage().contains("401")) {
        showError("Invalid API key. Update configuration.");
    } else {
        showError("Network error: " + e.getMessage());
    }
}
```

---

## Performance Guidelines

### Caching Strategy

**Cache these (rarely change):**
- Project metadata (statuses, priorities, trackers)
- Current user
- Project versions

**Don't cache these (frequently change):**
- Task lists
- Task details
- Time entries

### Batch Operations

**❌ Slow (sequential):**
```java
for (int id : taskIds) {
    Task task = service.fetchTaskDetails(id); // N API calls
}
```

**✅ Fast (bulk):**
```java
List<Task> tasks = service.fetchTasksByIds(taskIds); // 1 API call
```

### Async for UI

**Always** use `AsyncDataService` in UI code to prevent freezing:

```java
asyncService.fetchTasksAsync(pid, false, 100)
    .thenAccept(tasks -> updateUI(tasks))
    .exceptionally(ex -> { handleError(ex); return null; });
```

---

## Thread Safety

### Implementation Requirements

Implementations **must be thread-safe** because:
- Async operations call methods from worker threads
- Background refresh may run concurrently with user actions
- Bulk operations execute in parallel

### Recommended Pattern

```java
@ThreadSafe
public class RedmineDataServiceImpl implements DataService {
    private final String apiUrl;
    private final String apiKey;
    
    // Stateless: safe for concurrent calls
    @Override
    public List<Task> fetchTasks(String pid, boolean closed, int limit) {
        // Each call is independent, no shared state
    }
}
```

---

## Extension Guide

### Creating a Custom Implementation

```java
public class CustomDataService implements DataService {
    
    @Override
    public List<Task> fetchTasks(String pid, boolean closed, int limit) throws Exception {
        // Your custom logic here
        // Examples:
        // - Wrap another service with caching
        // - Add logging/metrics
        // - Implement offline mode
        // - Mock data for testing
    }
    
    // Implement all other interface methods...
}
```

### Wrapper Pattern (Decorator)

```java
public class CachedDataService implements DataService {
    private final DataService delegate;
    private final Map<String, List<Task>> taskCache = new ConcurrentHashMap<>();
    
    public CachedDataService(DataService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public List<Task> fetchTasks(String pid, boolean closed, int limit) throws Exception {
        String cacheKey = pid + ":" + closed;
        return taskCache.computeIfAbsent(cacheKey, k -> {
            try {
                return delegate.fetchTasks(pid, closed, limit);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

---

## Testing

### Unit Test Example

```java
@Test
public void testFetchTasks() throws Exception {
    DataService service = new MockDataService();
    List<Task> tasks = service.fetchTasks("test-project", false, 10);
    
    assertNotNull(tasks);
    assertTrue(tasks.size() <= 10);
    for (Task task : tasks) {
        assertNotNull(task.subject);
        assertNotEquals(0, task.id);
    }
}
```

### Integration Test

```java
@Test
@Ignore("Requires live Redmine server")
public void testRealAPI() throws Exception {
    DataService service = new RedmineDataServiceImpl(
        "https://demo.redmine.org",
        "API_KEY_HERE"
    );
    
    List<Task> tasks = service.fetchTasks("demo", false, 5);
    assertTrue("Should fetch at least some tasks", tasks.size() > 0);
}
```

---

## See Also

- **Javadoc:** `DataService.java` (complete method documentation)
- **ADR-001:** Twin Task Synchronization Architecture
- **ADR-002:** Async Operations Pattern
- **Implementation:** `RedmineDataServiceImpl.java`
- **Async Wrapper:** `AsyncDataService.java`

---

**Last Updated:** December 29, 2025  
**Contributors:** Redmine Connector Team
