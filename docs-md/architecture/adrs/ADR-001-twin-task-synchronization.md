# ADR-001: Twin Task Synchronization Architecture

**Status:** Accepted  
**Date:** 2024  
**Decision Makers:** Redmine Connector Team  
**Category:** Architecture

---

## Context and Problem Statement

The application needs to manage tasks across multiple Redmine instances (servers) where tasks in different servers are actually related or represent the same work item. Users need a way to:

1. Clone tasks from one server to another with reference tracking
2. Automatically match related tasks across servers
3. Synchronize closure operations (closing a task in one server should close its twin)
4. Maintain bidirectional relationships

**Example Scenario:**
- Server A (Development): Task #123 "Implement login feature"
- Server B (Client): Task #456 "Implement login feature [Ref #123]"
- When #456 is closed → #123 should also be closed

---

## Decision Drivers

- **Traceability:** Need to track which tasks are related across servers
- **User Productivity:** Avoid manual synchronization of related tasks
- **Data Integrity:** Prevent accidental mismatches
- **Flexibility:** Support customizable naming patterns
- **Non-Invasive:** Work with standard Redmine API (no server modifications)

---

## Considered Options

### Option 1: Database-Based Relationship Storage
Store twin relationships in a local database table.

**Pros:**
- Reliable, persistent storage
- Fast lookups
- Support for many-to-many relationships

**Cons:**
- Adds database dependency
- Requires migration/backup logic
- Complexity increase
- No portability across installations

### Option 2: Custom Field in Redmine
Use Redmine custom fields to store twin task IDs.

**Pros:**
- Server-side storage
- Visible in Redmine UI
- Syncs with Redmine backups

**Cons:**
- Requires Redmine admin configuration
- Pollutes task data
- Not all Redmine instances may allow custom fields
- Circular reference issues

### Option 3: Pattern-Based Detection (CHOSEN)
Use configurable patterns in task description to identify twins.

**Pros:**
- No external dependencies
- Works with vanilla Redmine
- Visible and editable by users
- Portable across installations
- Self-documenting (pattern visible in description)

**Cons:**
- Relies on description format consistency
- Requires user discipline to not modify references
- Pattern matching has edge cases

---

## Decision Outcome

**Chosen Option:** Option 3 (Pattern-Based Detection) with the following design:

### Configuration
Each client connection defines a clone pattern:
```properties
client.A.clonePattern=[Ref #{id}]
client.B.clonePattern=[External #{id}]
```

### Clone Operation
When cloning task #123 from Server A to Server B:

1. Create new task on Server B
2. Prepend pattern to description: `[Ref #123] Original description...`
3. Original task stays unmodified (unidirectional reference initially)

### Twin Matching (Smart Match)
When looking for twin on Server B given task #123 from Server A:

1. Build search pattern: `[Ref #123]` (using Server B's configured pattern)
2. Search tasks on Server B by subject + description
3. First match with exact pattern is considered the twin

### Bidirectional Enhancement
Optionally, can update original task's description to reference back:
```
Original description...
[Synced with ServerB #456]
```

---

## Consequences

### Positive

- ✅ **Zero Infrastructure:** No database, no custom fields
- ✅ **Transparency:** Users can see references in plain text
- ✅ **Flexibility:** Pattern is configurable per client
- ✅ **Portability:** Works identically across all installations
- ✅ **Debugging:** References visible in Redmine web UI without special tools

### Negative

- ⚠️ **Manual Editing Risk:** Users can accidentally remove reference patterns
- ⚠️ **Pattern Conflicts:** Different patterns across clients could cause confusion
- ⚠️ **Search Performance:** Relies on Redmine API search (may be slow for large projects)

### Neutral

- 🔄 **Pattern Evolution:** Changing pattern requires updating existing tasks manually
- 🔄 **One-Time Setup:** Each client must configure their pattern

---

## Implementation Details

### Key Classes

- **`InstanceController`:** Orchestrates twin operations
- **`TaskOperations`:** Handles clone and multi-close logic
- **`DataService`:** Provides search capabilities
- **`ConnectionConfig`:** Stores clone pattern per client

### Twin Detection Algorithm

```java
public Task findTwin(Task sourceTask, String targetClientId) {
    // 1. Get target client's pattern
    String pattern = targetConfig.get clonePattern();
    
    // 2. Build search query
    String searchText = pattern.replace("{id}", String.valueOf(sourceTask.id));
    
    // 3. Search in target server by description
    List<Task> candidates = targetService.fetchTasks(projectId, false, 100);
    
    // 4. Filter by exact pattern match
    for (Task task : candidates) {
        if (task.description != null && task.description.contains(searchText)) {
            return task; // First match wins
        }
    }
    
    return null; // No twin found
}
```

### Closure Synchronization

When closing task #456 on Server B:

1. Detect it has pattern `[Ref #123]`
2. Extract source ID: 123
3. Look up which client uses this pattern
4. Fetch task #123 from source client
5. Ask user: "Close twin task #123 too?"
6. If yes: Close both with synchronized status + notes

---

## Validation

### Testing Approach

1. **Unit Tests:** Pattern extraction and matching logic
2. **Integration Tests:** Clone → Match → Close cycle across 2 mock servers
3. **User Acceptance:** Manual testing with real Redmine instances

### Success Criteria

- ✅ Clone creates task with reference in description
- ✅ Smart Match finds cloned task from original
- ✅ Closure of twin prompts for synchronized closure
- ✅ Pattern is configurable and persisted
- ✅ Works with standard Redmine (no plugins required)

---

## Alternatives Considered

We also briefly considered:

- **Git-Style Commit References:** Using keywords like "Relates to #123" but this is less structured and harder to parse
- **URL-Based References:** Embedding full Redmine URLs, but these are lengthy and fragile if server URLs change
- **JSON Metadata in Notes:** Hiding JSON in comments, but this pollutes note history

---

## Related Decisions

- **ADR-002:** Async Operations Pattern (for performance during twin operations)
- **ADR-003:** Configuration Management (for storing clone patterns)

---

## References

- Redmine API Documentation: https://www.redmine.org/projects/redmine/wiki/Rest_api
- Issue Discussion: Internal ticket #REF-001
- User Feedback: Collected in v7.x versions

---

##  Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-Q2 | Initial decision |
| 1.1 | 2024-Q4 | Added bidirectional reference option |
| 2.0 | 2024-12 | Refined after v9.0 refactoring |

---

**Next Review:** 2025-Q2 or when twin matching failures exceed 5% of operations.
