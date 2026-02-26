package redmineconnector.test;

import redmineconnector.model.Task;

public class TaskTest {
    public static void runTests(SimpleTestRunner runner) {
        runner.run("Task.Constructor (Copy)", () -> {
            Task original = new Task();
            original.id = 100;
            original.subject = "Original";
            original.priority = "High";

            Task copy = new Task(original);

            SimpleTestRunner.assertEquals(0, copy.id, "Copy ID should be 0"); // Copy constructor resets ID
            SimpleTestRunner.assertEquals("Original", copy.subject, "Subject mismatch");
            SimpleTestRunner.assertEquals("High", copy.priority, "Priority mismatch");
            SimpleTestRunner.assertEquals("Nueva", copy.status, "Status should be 'Nueva' for copy");
        });
    }
}
