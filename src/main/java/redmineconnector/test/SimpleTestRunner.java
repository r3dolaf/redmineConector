package redmineconnector.test;

public class SimpleTestRunner {
    private int passed = 0;
    private int failed = 0;

    public void run(String testName, Runnable test) {
        try {
            System.out.print("Running " + testName + "... ");
            test.run();
            System.out.println("✅ PASS");
            passed++;
        } catch (AssertionError | Exception e) {
            System.out.println("❌ FAIL");
            System.out.println("    error: " + e.getMessage());
            // e.printStackTrace();
            failed++;
        }
    }

    public void printSummary() {
        System.out.println("===============================");
        System.out.println("Tests passed: " + passed);
        System.out.println("Tests failed: " + failed);
        System.out.println("===============================");
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null)
            return;
        if (expected != null && !expected.equals(actual)) {
            throw new AssertionError(message + " Expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    public static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message + " Expected not null");
        }
    }

    public static void assertNull(Object actual, String message) {
        if (actual != null) {
            throw new AssertionError(message + " Expected null but was: <" + actual + ">");
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual, String message) {
        if (unexpected == null && actual == null) {
            throw new AssertionError(message + " Expected not to be null");
        }
        if (unexpected != null && unexpected.equals(actual)) {
            throw new AssertionError(message + " Expected not to be: <" + unexpected + ">");
        }
    }
}
