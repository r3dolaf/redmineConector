# Testing Guide - Redmine Connector

## Overview

The Redmine Connector  project uses a custom testing framework (`SimpleTestRunner`) that requires no external dependencies. This guide explains how to write, run, and maintain tests.

---

## Test Framework

### SimpleTestRunner

Custom test runner providing:
- Test execution and result tracking
- Basic assertions (`assertTrue`, `assertEquals`, `assertNotNull`, etc.)
- Test grouping and reporting
- 100% Java Standard Library (no JUnit/TestNG)

**Why custom?** Zero dependencies = easier deployment and long-term stability.

---

## Running Tests

### From Eclipse
1. Navigate to `redmineconnector.test.RunAllTests`
2. Right-click → **Run As** → **Java Application**
3. View results in Console

### From Command Line
```bash
cd RefactoredProject
java -cp build/classes redmineconnector.test.RunAllTests
```

### Expected Output
```
Starting Test Suite...
=====================================
Running JsonParser.extractId... ✅ PASS
Running Task.Constructor (Copy)... ✅ PASS
...
Tests passed: 167
Tests failed: 0
```

---

## Writing Tests

### Basic Test Structure

```java
package redmineconnector.test;

public class MyComponentTest {
    
    public static void runTests(SimpleTestRunner runner) {
        
        runner.run("Component - Basic functionality", () -> {
            // Arrange
            MyComponent comp = new MyComponent("test");
            
            // Act
            String result = comp.process();
            
            // Assert
            SimpleTestRunner.assertNotNull(result, "Result should not be null");
            SimpleTestRunner.assertTrue("expected".equals(result), "Should return 'expected'");
        });
    }
}
```

### Register in RunAllTests

```java
public class RunAllTests {
    public static void main(String[] args) {
        SimpleTestRunner runner = new SimpleTestRunner();
        
        // ... existing tests ...
        MyComponentTest.runTests(runner);
        
        runner.printSummary();
    }
}
```

---

## Available Assertions

| Method | Purpose | Example |
|--------|---------|---------|
| `assertTrue(condition, msg)` | Assert true | `assertTrue(x > 0, "X must be positive")` |
| `assertEquals(expected, actual, msg)` | Assert equality | `assertEquals(10, result, "Should be 10")` |
| `assertNotNull(obj, msg)` | Assert not null | `assertNotNull(user, "User required")` |
| `assertNull(obj, msg)` | Assert null | `assertNull(error, "No error expected")` |
| `assertNotEquals(val1, val2, msg)` | Assert inequality | `assertNotEquals(0, count, "Count should change")` |

---

## Test Categories

### 1. Unit Tests
Test individual classes in isolation.

**Example:** `LoggerUtilTest.java`
```java
runner.run("LoggerUtil.logInfo - Format includes level", () -> {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out));
    
    try {
        LoggerUtil.logInfo("TestSource", "Test message");
        String output = out.toString();
        
        SimpleTestRunner.assertTrue(output.contains("[INFO]"), "Should contain INFO level");
    } finally {
        System.setOut(System.out); // Restore
    }
});
```

### 2. Integration Tests
Test multiple components working together.

**Example:** `HttpDataServiceTest.java`
```java
runner.run("HttpDataService.fetchTasks - Success with mock", () -> {
    MockHttpServer mock = new MockHttpServer(18080);
    
    try {
        mock.addResponse("GET", "/issues.json", "{\"issues\":[...]}");
        mock.start();
        
        HttpDataService service = new HttpDataService(mock.getUrl(), "key", null);
        List<Task> tasks = service.fetchTasks("project", false, 10);
        
        SimpleTestRunner.assertTrue(tasks.size() == 1, "Should return 1 task");
    } finally {
        mock.stop();
    }
});
```

### 3. UI Tests
Test Swing components without full app startup.

**Example:** `ComponentTest.java`
```java
runner.run("JButton - Creation with action", () -> {
    final boolean[] clicked = {false};
    
    JButton button = new JButton("Click Me");
    button.addActionListener(e -> clicked[0] = true);
    
    UITestHelper.clickButton(button);
    UITestHelper.sleep(100);
    
    SimpleTestRunner.assertTrue(clicked[0], "Button should trigger action");
});
```

---

## MockHttpServer

Custom HTTP mock server for testing without real network calls.

### Basic Usage

```java
MockHttpServer mock = new MockHttpServer(8080);

// Add response
mock.addResponse("GET", "/issues.json", "{\"issues\":[]}");

// Start server
mock.start();

// Use in tests
HttpDataService service = new HttpDataService(mock.getUrl(), "key", null);
List<Task> tasks = service.fetchTasks("project", false, 10);

// Always stop
mock.stop();
```

### Advanced Features

**Custom status codes:**
```java
mock.addResponse("GET", "/issues.json", 404, "{\"error\":\"Not Found\"}");
```

**Wildcard paths:**
```java
mock.addResponse("GET", "/issues/*.json", "{...}");  // Matches /issues/123.json
```

---

## UITestHelper

Utilities for testing Swing components.

### Finding Components

```java
// By name
Component comp = UITestHelper.findComponentByName(dialog, "okButton");

// By type
List<JButton> buttons = UITestHelper.findComponentsByType(panel, JButton.class);

// By text
JButton btn = UITestHelper.findButtonByText(panel, "Submit");
```

### Simulating User Actions

```java
// Click button
UITestHelper.clickButton(button);

// Type text
UITestHelper.setText(textField, "input");

// Select combo item
UITestHelper.selectComboBoxItem(comboBox, "Option 2");
```

### Waiting

```java
// Wait for Event Dispatch Thread
UITestHelper.waitForEDT();

// Explicit sleep
UITestHelper.sleep(500); // milliseconds
```

---

## Best Practices

### ✅ DO

- **Test one thing per test** - Keep tests focused
- **Use descriptive names** - "LoggerUtil.logError - With exception"
- **Clean up resources** - Use try-finally blocks
- **Test edge cases** - Null, empty, large values
- **Keep tests fast** - Avoid unnecessary delays

### ❌ DON'T

- **Don't test external services** - Use mocks
- **Don't share state** - Each test should be independent
- **Don't use hardcoded paths** - Use relative paths or temp directories
- **Don't ignore failures** - Fix or document why skipped

---

## Coverage Goals

| Category | Target | Current |
|----------|--------|---------|
| Utilities | 80%+ | ✅ 85% |
| Models | 70%+ | ✅ 75% |
| Services | 65%+ | ✅ 68% |
| UI | 30%+ | ✅ 32% |
| **Overall** | **65%+** | ✅ **67%** |

---

## Troubleshooting

### Tests Hang
**Cause:** System.out/err redirection not restored  
**Fix:** Use try-finally blocks

```java
PrintStream originalOut = System.out;
try {
    System.setOut(new PrintStream(capture));
    // test code
} finally {
    System.setOut(originalOut); // Always restore!
}
```

### UI Tests Fail on Headless Systems
**Cause:** No display available  
**Fix:** Run with headless support or skip UI tests in CI

```bash
java -D java.awt.headless=true ...
```

### MockHttpServer Port Conflicts
**Cause:** Port already in use  
**Fix:** Use different port or ensure cleanup

```java
static final int TEST_PORT = 18080 + (int)(Math.random() * 1000);
```

---

## Continuous Integration

### Adding to CI Pipeline

```yaml
# Example GitHub Actions
- name: Run Tests
  run: |
    cd RefactoredProject
    javac -d build/classes src/main/java/**/*.java
    java -cp build/classes redmineconnector.test.RunAllTests
```

### Test Reports

Currently: Console output  
Future: JUnit XML format for CI integration

---

## Adding New Test Categories

1. **Create test file** in `redmineconnector.test`
2. **Follow naming convention** - `[Component]Test.java`
3. **Implement `runTests(runner)` method**
4. **Add to `RunAllTests.java`**
5. **Run and verify** all tests still pass

---

## Resources

- **SimpleTestRunner API**: `src/main/java/redmineconnector/test/SimpleTestRunner.java`
- **Example Tests**: All files in `src/main/java/redmineconnector/test/`
- **Mock Server**: `src/main/java/redmineconnector/test/MockHttpServer.java`
- **UI Helpers**: `src/main/java/redmineconnector/test/ui/UITestHelper.java`

---

**Last Updated:** 2025-12-29  
**Test Count:** 167  
**Pass Rate:** 100%
