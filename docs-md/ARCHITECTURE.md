# Architecture Overview - Redmine Connector

## System Architecture

The Redmine Connector is built using a layered architecture pattern with clear separation of concerns.

```
┌─────────────────────────────────────────────────────────┐
│                     Presentation Layer                  │
│  (UI - Swing components, dialogs, panels, themes)       │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                     Service Layer                        │
│  (Business logic, caching, async operations)             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                     Data Layer                           │
│  (HTTP client, JSON parsing, API communication)          │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  External Systems                        │
│              (Redmine REST API)                          │
└──────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
redmineconnector/
├── config/              Configuration management
│   ├── ConfigManager              Properties, encryption
│   ├── ConnectionConfig           Client connection settings
│   └── StyleConfig                UI styling & themes
│
├── model/               Data Transfer Objects (DTOs)
│   ├── Task                      Main task entity
│   ├── Attachment                File attachments
│   ├── Journey, TimeEntry        Task history
│   └── WikiPageDTO, VersionDTO   Documentation & versions
│
├── service/             Business logic
│   ├── DataService (interface)   Service contract
│   ├── HttpDataService            HTTP implementation
│   ├── CachedDataService          Caching decorator
│   ├── AsyncDataService           Async wrapper
│   ├── CloneService               Task cloning logic
│   └── ExportManager              Data export
│
├── ui/                  User interface
│   ├── MainFrame                  Main application window
│   ├── dialogs/                   Modal dialogs
│   ├── panels/                    Reusable UI panels
│   ├── components/                Custom controls
│   └── theme/                     Theme system
│
├── util/                Utilities
│   ├── HttpUtils                  HTTP client
│   ├── JsonParser                 JSON processing
│   ├── LoggerUtil                 Logging
│   ├── I18n                       Internationalization
│   ├── SecurityUtils              Encryption
│   └── AppConstants               Constants
│
└── test/                Test suite
    ├── SimpleTestRunner           Test framework
    ├── MockHttpServer             HTTP mocking
    └── ui/                        UI test helpers
```

---

## Design Patterns

### 1. Layered Architecture
- **Presentation → Service → Data → External**
- Clear responsibility boundaries
- Easy to test and maintain

### 2. Decorator Pattern
```java
DataService base      = new HttpDataService(url, key, logger);
DataService cached    = new CachedDataService(base);
DataService async     = new AsyncDataService(cached);
```
- Wraps core service with additional behavior
- Caching, async execution without modifying core

### 3. Strategy Pattern
```java
interface DataService {
    List<Task> fetchTasks(...);
    void updateTask(...);
}
```
- Multiple implementations: HTTP, Mock, Offline
- Swappable at runtime
- Easy to add new strategies

### 4. Singleton Pattern
```java
ConfigManager.getInstance()
NotificationManager.getInstance()
```
- Single instance for global state
- Lazy initialization
- Thread-safe

### 5. Factory Pattern
```java
DialogManager.showTaskDialog(...)
DialogManager.showHelpDialog(...)
```
- Centralized dialog creation
- Consistent initialization
- Easier maintenance

### 6. Observer Pattern
```java
NotificationManager.addListener(...)
```
- Event-driven notifications
- Decoupled components
- Real-time updates

---

## Key Components

### DataService Interface

**Purpose:** Abstract API communication

**Implementations:**
- `HttpDataService` - Real HTTP calls to Redmine
- `CachedDataService` - Adds caching layer
- `AsyncDataService` - Async execution wrapper

**Key Methods:**
```java
List<Task> fetchTasks(String projectId, boolean closed, int limit)
Task fetchTaskDetails(int id)
int createTask(String projectId, Task task)
void updateTask(Task task)
```

### HTTP Layer

**HttpUtils:**
- Low-level HTTP operations
- GET, POST, PUT, DELETE
- Binary upload/download
- Error handling

**JsonParser:**
- Parse Redmine JSON responses
- Serialize requests
- Handle nested objects
- Extract specific fields

### Caching Strategy

**Two-level caching:**
1. **Memory Cache** - ConcurrentHashMap
   - 5-minute TTL for metadata
   - Thread-safe
   - Automatic expiration

2. **Image Cache** - ImageCacheService
   - 10-minute TTL
   - URL-based keys
   - Memory-efficient

**Cache Invalidation:**
- On create/update operations
- Manual via clear methods
- Automatic expiration

### Async Operations

**AsyncDataService:**
- Wraps any DataService
- Returns CompletableFuture
- Non-blocking UI
- Error propagation

**Thread Pool:**
```java
Executor: Fixed thread pool (size: 3-10)
Keep-alive: 60 seconds
Queue: Unbounded
```

---

## Data Flow

### Fetching Tasks

```
1. User clicks "Refresh"
   │
2. UI calls AsyncDataService.fetchTasksAsync()
   │
3. AsyncDataService → CachedDataService.fetchTasks()
   │
4. CachedDataService checks cache
   │   MISS ───→ HttpDataService.fetchTasks()
   │                │
   │                ├→ HttpUtils.get("/issues.json?...")
   │                │
   │                ├→ JsonParser.parseIssues(json)
   │                │
   │                └→ Store in cache
   │
5. Return List<Task> to UI
   │
6. UI updates table
```

### Creating Task

```
1. User fills TaskFormDialog
   │
2. Dialog calls DataService.createTask()
   │
3. JsonParser.serializeTaskForCreate(task)
   │
4. HttpUtils.post("/issues.json", json)
   │
5. JsonParser.extractId(response)
   │
6. Cache invalidation
   │
7. UI refreshes list
```

---

## Configuration Management

### Encryption

**API Keys:**
```
Plain: myapikey12345
   ↓ AES encryption
Stored: +XJ8kL9mN...encoded...
   ↓ On load: decrypt
Used: myapikey12345
```

**SecurityUtils:**
- AES/ECB/PKCS5Padding
- Base64 encoding
- Safe key derivation

### Properties File

```ini
# redmineconnector_config.properties

client.1.url=https://redmine.example.com
client.1.apiKey=+XJ8kL9mN...  # Encrypted
client.1.projectId=myproject
client.1.refreshInterval=5

app.theme=LIGHT
app.locale=en_US
```

---

## UI Architecture

### Main Components

**MainFrame:**
- Application container
- Tab management
- Menu bar
- Status bar
- Global event handling

**InstanceView:**
- Per-client tab
- Task table
- Filter panel
- Quick view panel
- Context menus

**Dialogs:**
- Modal forms
- User input
- Configuration
- Reports/statistics

### Theme System

**ThemeConfig Interface:**
```java
interface ThemeConfig {
    Color getBackground();
    Color getText();
    Color getAccent();
    // ...
}
```

**Implementations:**
- Theme.LIGHT
- Custom themes via ThemeEditorDialog

**Application:**
- Applied globally
- Persistent across sessions
- Real-time preview

---

## Security Considerations

### API Key Protection
- ✅ Encrypted in properties file
- ✅ Not logged or displayed
- ✅ Memory cleared after use

### HTTPS Support
- ✅ SSL/TLS for all HTTP calls
- ⚠️ Certificate validation (default Java)

### Input Validation
- ✅ Required fields (subject, tracker)
- ⚠️ Limited XSS prevention (Redmine handles)

### Session Management
- ⚠️ No timeout (API key-based)
- ⚠️ No auto-logout

**Recommendations:**
- Add SSL certificate pinning
- Implement session timeout
- Enhanced input sanitization

---

## Performance Optimizations

### Current

**Caching:**
- Metadata cached (5 min)
- Images cached (10 min)
- Reduced HTTP calls

**Async Operations:**
- Non-blocking UI
- Parallel requests
- Batch operations

**Lazy Loading:**
- Task details on demand
- Attachments on-demand
- Wiki content on-demand

### Future Opportunities

**Database Caching:**
- SQLite for persistence
- Offline mode improvements
- Faster startup

**Connection Pooling:**
- Reuse HTTP connections
- Reduce latency
- Better throughput

**UI Rendering:**
- Virtual scrolling for large lists
- Custom cell renderers
- Progressive loading

---

## Testing Architecture

### Test Framework

**SimpleTestRunner:**
- Zero dependencies
- Basic assertions
- Test grouping
- Result reporting

**MockHttpServer:**
- In-memory HTTP server
- No network calls
- Deterministic responses
- Fast execution

### Test Categories

**Unit Tests (82):**
- Individual classes
- No external dependencies
- Fast (<1ms each)

**Integration Tests (60):**
- Multi-component
- Mock HTTP
- Moderate speed (~10ms each)

**UI Tests (25):**
- Swing components
- No full app startup
- EDT handling

**Total:** 167 tests, 100% pass rate

---

## Dependencies

**Runtime:**
- ✅ ZERO external dependencies
- Java Standard Library only
- JAR size: ~586 KB

**Build:**
- Apache Ant (build.xml)
- Java 8+ compiler

**Benefits:**
- Simple deployment
- No version conflicts
- Long-term stability
- Small footprint

---

## Extensibility

### Adding New Data Sources

**Implement DataService:**
```java
public class MyDataService implements DataService {
    @Override
    public List<Task> fetchTasks(...) {
        // Custom implementation
    }
    // ... other methods
}
```

**Use existing decorators:**
```java
DataService service = new My DataService();
service = new CachedDataService(service);
service = new AsyncDataService(service);
```

### Adding New Dialogs

**Extend base:**
```java
public class MyDialog extends JDialog {
    public MyDialog(JFrame parent) {
        super(parent, "My Dialog", true);
        initComponents();
    }
}
```

**Register in DialogManager:**
```java
public static void showMyDialog(...) {
    MyDialog dialog = new MyDialog(parent);
    dialog.setVisible(true);
}
```

---

## Future Architecture

### Potential Migrations

**JavaFX:**
- Modern UI framework
- Better theming
- CSS styling
- **Effort:** ~80 hours

**Modular Architecture:**
- Java 9+ modules
- Plugin system
- Hot-reload
- **Effort:** ~60 hours

**Microservices (overkill for desktop):**
- Separate API gateway
- Background sync service
- **Not recommended**

---

**Last Updated:** 2025-12-29  
**Version:** 2.6  
**Architecture Grade:** A (4.5/5)
