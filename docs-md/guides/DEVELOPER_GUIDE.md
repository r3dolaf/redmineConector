# 🚀 Developer Quick Start Guide

## Getting Started in 5 Minutes

### Prerequisites
- Java JDK 8+
- IDE (Eclipse, IntelliJ, VS Code)
- Git (optional)

### 1. Project Structure
```
RefactoredProject/
├── src/main/java/redmineconnector/
│   ├── RedmineConnectorApp.java    # Entry point
│   ├── config/                      # Configuration classes
│   ├── model/                       # Data models (Task, User, etc.)
│   ├── service/                     # Data access layer
│   ├── ui/                          # Swing UI components
│   ├── notifications/               # Notification system
│   ├── util/                        # Utilities
│   └── test/                        # Unit tests
├── redmine_config.properties        # Configuration file
└── README.md                        # User documentation
```

### 2. Build & Run
```bash
# Compile
javac -d bin -sourcepath src/main/java src/main/java/redmineconnector/**/*.java

# Run
java -cp bin redmineconnector.RedmineConnectorApp

# Or use your IDE's run configuration
```

### 3. Key Entry Points

**Main Application**:
```java
// src/main/java/redmineconnector/RedmineConnectorApp.java
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        new MainFrame().setVisible(true);
    });
}
```

**Add New Feature to Menu**:
```java
// src/main/java/redmineconnector/ui/MainFrame.java
private JMenu createClientMenu(InstanceController c, String title) {
    JMenu menu = new JMenu(title);
    
    // Add your menu item
    JMenuItem myFeature = new JMenuItem("My Feature");
    myFeature.addActionListener(e -> c.myFeatureAction());
    menu.add(myFeature);
    
    return menu;
}
```

**Add New Service Method**:
```java
// 1. Define in interface
// src/main/java/redmineconnector/service/DataService.java
public interface DataService {
    List<MyData> fetchMyData(ConnectionConfig cfg) throws Exception;
}

// 2. Implement in HttpDataService
// src/main/java/redmineconnector/service/HttpDataService.java
@Override
public List<MyData> fetchMyData(ConnectionConfig cfg) throws Exception {
    String url = cfg.url + "my_endpoint.json";
    String json = doGet(url, cfg);
    return JsonParser.parseMyDataList(json);
}

// 3. Use in controller
// src/main/java/redmineconnector/ui/InstanceController.java
public void loadMyData() {
    dataService.fetchMyData(config).thenAccept(data -> {
        SwingUtilities.invokeLater(() -> view.displayMyData(data));
    });
}
```

### 4. Common Tasks

**Add New Dialog**:
```java
// src/main/java/redmineconnector/ui/dialogs/MyDialog.java
public class MyDialog extends JDialog {
    public MyDialog(JFrame parent) {
        super(parent, "My Dialog", true);
        setLayout(new BorderLayout());
        
        // Add components
        JPanel panel = new JPanel();
        panel.add(new JLabel("Hello"));
        add(panel, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(parent);
    }
}

// Use in controller
new MyDialog(mainFrame).setVisible(true);
```

**Add New Notification Type**:
```java
// 1. Add to enum
// src/main/java/redmineconnector/model/Notification.java
public enum Type {
    NEW_TASK, ASSIGNMENT, MY_NEW_TYPE
}

// 2. Trigger notification
// src/main/java/redmineconnector/ui/InstanceController.java
NotificationManager.createNotification(
    Notification.Type.MY_NEW_TYPE,
    "My notification message",
    taskId
);
```

### 5. Testing

**Run All Tests**:
```bash
java -cp bin redmineconnector.test.RunAllTests
```

**Add New Test**:
```java
// src/main/java/redmineconnector/test/MyTest.java
public class MyTest {
    public static void runTests(SimpleTestRunner runner) {
        runner.test("My test case", () -> {
            // Test code
            assert myFunction() == expectedValue;
        });
    }
}

// Add to RunAllTests.java
MyTest.runTests(runner);
```

### 6. Debugging Tips

**Enable Debug Logging**:
```java
System.setProperty("redmine.debug", "true");
```

**Common Issues**:
- **NullPointerException**: Check EDT thread for UI updates
- **Connection timeout**: Increase timeout in HttpDataService
- **Cache not working**: Verify CachedDataService is being used

### 7. Code Style Guidelines

- **Naming**: camelCase for variables/methods, PascalCase for classes
- **Threading**: All UI updates on EDT via `SwingUtilities.invokeLater()`
- **Error Handling**: Catch exceptions, log to LogPanel
- **Documentation**: Javadoc for public methods
- **Formatting**: 4 spaces indent, 120 char line limit

### 8. Architecture Patterns

**Service Layer** (Decorator Pattern):
```
DataService (interface)
  ├── HttpDataService (HTTP implementation)
  ├── AsyncDataService (async wrapper)
  └── CachedDataService (caching decorator)
```

**UI Components** (MVC):
```
Model: Task, User, etc.
View: InstanceView, dialogs
Controller: InstanceController
```

### 9. Useful Resources

- **Full Technical Docs**: `TECHNICAL_ARCHITECTURE.md`
- **User Manual**: `MANUAL_ES.md` / `README.md`
- **Configuration**: `CONFIGURATION_GUIDE.md`
- **Redmine API**: https://www.redmine.org/projects/redmine/wiki/Rest_api

### 10. Contributing

1. Fork repository
2. Create feature branch
3. Make changes
4. Run tests
5. Submit pull request

---

**Happy Coding! 🎉**
