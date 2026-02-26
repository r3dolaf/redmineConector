package redmineconnector.test;

public class RunAllTests {
    public static void main(String[] args) {
        SimpleTestRunner runner = new SimpleTestRunner();

        System.out.println("Starting Test Suite...");
        System.out.println("=====================================\n");

        // Original tests
        JsonParserTest.runTests(runner);
        TaskTest.runTests(runner);
        StyleConfigTest.runTests(runner);
        ConnectionConfigTest.runTests(runner);

        // New tests for async and caching
        AsyncDataServiceTest.runTests(runner);
        CacheServiceTest.runTests(runner);
        CachedDataServiceTest.runTests(runner);
        SecurityUtilsTest.runTests(runner);

        // Phase 5 - Day 1: Critical Utilities
        System.out.println("\n=== Phase 5: Day 1 Tests ===");
        LoggerUtilTest.runTests(runner);
        I18nTest.runTests(runner);
        ConfigManagerTest.runTests(runner);

        // Phase 5 - Day 3: HttpDataService Integration
        System.out.println("\n=== Phase 5: Day 3 Tests ===");
        HttpDataServiceTest.runTests(runner);

        // Phase 5 - Day 2: Models
        System.out.println("\n=== Phase 5: Day 2 Tests ===");
        AttachmentTest.runTests(runner);
        JournalTest.runTests(runner);
        TimeEntryTest.runTests(runner);
        VersionDTOTest.runTests(runner);
        WikiPageDTOTest.runTests(runner);

        // Phase 5 - UI Tests
        System.out.println("\n=== UI Component Tests ===");
        redmineconnector.test.ui.DialogTest.runTests(runner);
        redmineconnector.test.ui.ComponentTest.runTests(runner);
        redmineconnector.test.ui.TableAndListTest.runTests(runner);

        // Phase 6 - Configuration Tests
        System.out.println("\n=== Configuration Tests ===");
        redmineconnector.test.AppConstantsTest.runTests(runner);

        System.out.println("\n=====================================");
        runner.printSummary();
    }
}
