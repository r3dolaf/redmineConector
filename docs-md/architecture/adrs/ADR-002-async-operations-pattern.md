# ADR-002: Async Operations with CompletableFuture

**Status:** Accepted  
**Date:** 2024  
**Decision Makers:** Redmine Connector Team  
**Category:** Architecture / Performance

---

## Context and Problem Statement

Many operations in the application involve network calls to Redmine API which can be slow (200ms - 5s per request). Performing these synchronously blocks the UI thread, creating a poor user experience.

**Key Problem Areas:**
1. **Initial Load:** Fetching tasks + metadata on startup takes 2-10 seconds
2. **Bulk Operations:** Closing/updating multiple tasks sequentially is very slow
3. **Twin Operations:** Clone + match + close cycles involve multiple API calls
4. **User Perception:** Users cannot interact with UI while operations run

---

## Decision Drivers

- **Responsiveness:** UI must remain interactive during network operations
- **Throughput:** Bulk operations should run in parallel when possible
- **Error Handling:** Must handle individual failures without breaking entire operation
- **Progress Feedback:** Users need to see what's happening during long operations
- **Thread Safety:** Must avoid race conditions with shared UI state

---

## Considered Options

### Option 1: SwingWorker
Java Swing's built-in async pattern.

**Pros:**
- Built into Swing
- Simple for single background tasks
- Good UI integration

**Cons:**
- Clunky API for chaining operations
- Difficult to compose multiple async operations
- No built-in support for parallel execution of multiple tasks
- Exception handling is verbose

### Option 2: Thread Pools + Callbacks
Manual thread management with callback interfaces.

**Pros:**
- Full control over threading
- Flexible callback patterns

**Cons:**
- Very verbose (callback hell)
- Error-prone (threading bugs)
- Difficult to reason about flow
- Manual lifecycle management

### Option 3: CompletableFuture (CHOSEN)
Java 8+ asynchronous programming API.

**Pros:**
- Clean, composable API
- Built-in exception handling
- Parallel execution support with `allOf()`
- Functional programming style
- Non-blocking

**Cons:**
- Requires Java 8+
- Learning curve for developers unfamiliar with functional programming
- Debugging can be tricky (stack traces span threads)

---

## Decision Outcome

**Chosen Option:** Option 3 (CompletableFuture) wrapped in a service layer.

### Architecture Pattern

```
┌─────────────────┐
│  UI Controller  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ AsyncDataService│ ← Wrapper layer
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   DataService   │ ← Sync implementation
└─────────────────┘
       (Redmine API)
```

### Implementation

#### AsyncDataService Wrapper
```java
public class AsyncDataService {
    private final DataService syncService;
    private final ExecutorService executor;
    
    public CompletableFuture<List<Task>> fetchTasksAsync(String pid, boolean closed, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return syncService.fetchTasks(pid, closed, limit);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    // ... similar wrappers for all sync methods
}
```

#### Parallel Bulk Operations
```java
public void bulkUpdateTasks(List<Task> tasks) {
    List<CompletableFuture<String>> futures = tasks.stream()
        .map(task -> CompletableFuture.supplyAsync(() -> {
            try {
                syncService.updateTask(task);
                return "OK: #" + task.id;
            } catch (Exception e) {
                return "ERROR: #" + task.id + " - " + e.getMessage();
            }
        }, executor))
        .collect(Collectors.toList());
    
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenAccept(v -> {
            // All tasks updated (or failed individually)
            updateUI();
        });
}
```

#### UI Integration with SwingUtilities
```java
public void loadData() {
    asyncService.fetchTasksAsync(projectId, false, 100)
        .thenApply(tasks -> filterTasks(tasks))
        .thenApply(tasks -> enrichWithMetadata(tasks))
        .thenAccept(tasks -> SwingUtilities.invokeLater(() -> {
            // Update UI on EDT
            tableModel.setTasks(tasks);
        }))
        .exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                showError("Failed to load tasks: " + ex.getMessage());
            });
            return null;
        });
}
```

---

## Consequences

### Positive

- ✅ **Responsive UI:** No UI freezes during network calls
- ✅ **Better Performance:** Parallel execution reduces total time (10 tasks: 10s → 1-2s)
- ✅ **Clean Code:** Functional composition is readable and maintainable
- ✅ **Error Isolation:** Individual task failures don't break bulk operations
- ✅ **Cancellation Support:** CompletableFuture supports cancellation

### Negative

- ⚠️ **Thread Safety:** Must carefully manage UI updates (use SwingUtilities.invokeLater)
- ⚠️ **Debugging Complexity:** Async stack traces can be confusing
- ⚠️ **Resource Usage:** Multiple parallel operations consume more threads/memory

### Neutral

- 🔄 **Learning Curve:** Developers must understand CompletableFuture API
- 🔄 **Error Handling:** Exception handling is different from sync code

---

## Implementation Guidelines

### 1. Always Update UI on EDT
```java
// ❌ WRONG
completableFuture.thenAccept(result -> {
    uiComponent.setText(result); // UI update on worker thread!
});

// ✅ CORRECT
completableFuture.thenAccept(result -> {
    SwingUtilities.invokeLater(() -> {
        uiComponent.setText(result); // UI update on EDT
    });
});
```

### 2. Handle Exceptions Gracefully
```java
completableFuture
    .thenApply(this::processData)
    .exceptionally(ex -> {
        LoggerUtil.logError("Controller", "Operation failed", (Exception) ex);
        return fallbackValue;
    });
```

### 3. Use allOf() for Parallel Operations
```java
List<CompletableFuture<Task>> futures = ids.stream()
    .map(id -> asyncService.fetchTaskAsync(id))
    .collect(Collectors.toList());

Comple tableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .thenRun(() -> {
        // All tasks loaded
    });
```

### 4. Don't Forget Executor Shutdown
```java
@Override
public void shutdown() {
    executor.shutdown();
    try {
        executor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        executor.shutdownNow();
    }
}
```

---

## Validation

### Performance Metrics (Before/After)

| Operation | Sync (Before) | Async (After) | Improvement |
|-----------|---------------|---------------|-------------|
| Initial Load (100 tasks) | 8.5s | 2.1s | **75%** faster |
| Bulk Close (10 tasks) | 12s | 1.8s | **85%** faster |
| Clone + Match + Close | 6s | 2.5s | **58%** faster |

### Testing Approach

1. **Unit Tests:** Mock DataService, verify async wrappers
2. **Integration Tests:** Real API calls with timeouts
3. **Load Tests:** 1000 tasks with concurrent operations
4. **UI Tests:** Verify EDT violations (using EDT checker tools)

---

## Trade-offs Accepted

1. **Complexity vs Performance:** Added complexity is justified by 60-85% performance gains
2. **Resource Usage vs Responsiveness:** Higher thread usage is acceptable for better UX
3. **Code Duplication:** Async wrappers duplicate method signatures (acceptable overhead)

---

## Migration Path

### Phase 1: Wrapper Creation (Completed)
- Created AsyncDataService with CompletableFuture wrappers
- Maintained backward compatibility with sync DataService

### Phase 2: Controller Migration (Completed)
- Migrated InstanceController to use async operations
- Added proper EDT synchronization

### Phase 3: Bulk Operations (Completed)
- Implemented parallel bulk updates
- Added progress tracking

### Phase 4: Error Handling Enhancement (Future)
- Consider retry logic for transient failures
- Add timeout management

---

## Related Decisions

- **ADR-001:** Twin Task Synchronization (uses async for performance)
- **ADR-003:** Configuration Management (reload without blocking UI)

---

## References

- Java CompletableFuture Documentation: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
- Effective Java 3rd Edition, Item 81: Prefer concurrency utilities to wait and notify
- Performance Analysis: Internal benchmark report v8.5

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-Q1 | Initial async implementation |
| 1.1 | 2024-Q3 | Added parallel bulk operations |
| 2.0 | 2024-12 | Refined after v9.0 refactoring |

---

**Next Review:** 2025-Q2 or if performance issues arise with parallel operations.
