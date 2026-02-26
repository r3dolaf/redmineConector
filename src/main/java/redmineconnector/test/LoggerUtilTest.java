package redmineconnector.test;

import redmineconnector.util.LoggerUtil;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Unit tests for LoggerUtil - Centralized logging utility.
 * Tests log formatting, levels, and debug toggle functionality.
 * 
 * IMPORTANT: Uses try-finally to ensure System.out/err are always restored.
 */
public class LoggerUtilTest {

        public static void runTests(SimpleTestRunner runner) {

                runner.run("LoggerUtil.logInfo - Format includes level and timestamp", () -> {
                        PrintStream originalOut = System.out;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try {
                                System.setOut(new PrintStream(out));
                                LoggerUtil.logInfo("TestClass", "Test message");
                                String output = out.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("[INFO]"),
                                                "Should contain INFO level");
                                SimpleTestRunner.assertTrue(
                                                output.contains("TestClass"),
                                                "Should contain source");
                                SimpleTestRunner.assertTrue(
                                                output.contains("Test message"),
                                                "Should contain message");
                                SimpleTestRunner.assertTrue(
                                                output.indexOf('[') >= 0 && output.indexOf(':') > 0,
                                                "Should contain timestamp");
                        } finally {
                                System.setOut(originalOut);
                        }
                });

                runner.run("LoggerUtil.logWarning - Format correct", () -> {
                        PrintStream originalOut = System.out;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try {
                                System.setOut(new PrintStream(out));
                                LoggerUtil.logWarning("WarnSource", "Warning message");
                                String output = out.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("[WARNING]"),
                                                "Should contain WARNING level");
                                SimpleTestRunner.assertTrue(
                                                output.contains("WarnSource"),
                                                "Should contain source");
                        } finally {
                                System.setOut(originalOut);
                        }
                });

                runner.run("LoggerUtil.logError - Outputs to stderr", () -> {
                        PrintStream originalErr = System.err;
                        ByteArrayOutputStream err = new ByteArrayOutputStream();

                        try {
                                System.setErr(new PrintStream(err));
                                LoggerUtil.logError("ErrorSource", "Error message");
                                String output = err.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("[ERROR]"),
                                                "Should contain ERROR level");
                                SimpleTestRunner.assertTrue(
                                                output.contains("ErrorSource"),
                                                "Should contain source");
                                SimpleTestRunner.assertTrue(
                                                output.contains("Error message"),
                                                "Should contain message");
                        } finally {
                                System.setErr(originalErr);
                        }
                });

                runner.run("LoggerUtil.logError - With exception", () -> {
                        PrintStream originalErr = System.err;
                        ByteArrayOutputStream err = new ByteArrayOutputStream();

                        try {
                                System.setErr(new PrintStream(err));
                                Exception testException = new RuntimeException("Test exception");
                                LoggerUtil.logError("ExceptionSource", "Operation failed", testException);
                                String output = err.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("Operation failed"),
                                                "Should contain error message");
                                SimpleTestRunner.assertTrue(
                                                output.contains("Test exception"),
                                                "Should contain exception message");
                                SimpleTestRunner.assertTrue(
                                                output.contains("RuntimeException"),
                                                "Should contain exception type");
                        } finally {
                                System.setErr(originalErr);
                        }
                });

                runner.run("LoggerUtil.logDebug - Disabled by default", () -> {
                        PrintStream originalOut = System.out;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try {
                                LoggerUtil.setDebugEnabled(false);
                                System.setOut(new PrintStream(out));
                                LoggerUtil.logDebug("DebugSource", "Debug message");
                                String output = out.toString();

                                SimpleTestRunner.assertTrue(
                                                output.isEmpty(),
                                                "Debug should be disabled by default");
                        } finally {
                                System.setOut(originalOut);
                        }
                });

                runner.run("LoggerUtil.logDebug - Enabled when set", () -> {
                        PrintStream originalOut = System.out;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try {
                                LoggerUtil.setDebugEnabled(true);
                                System.setOut(new PrintStream(out));
                                LoggerUtil.logDebug("DebugSource", "Debug message");
                                String output = out.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("[DEBUG]"),
                                                "Should contain DEBUG level when enabled");
                                SimpleTestRunner.assertTrue(
                                                output.contains("Debug message"),
                                                "Should contain debug message");
                        } finally {
                                System.setOut(originalOut);
                                LoggerUtil.setDebugEnabled(false); // Reset
                        }
                });

                runner.run("LoggerUtil.isDebugEnabled - Toggle works", () -> {
                        LoggerUtil.setDebugEnabled(false);
                        SimpleTestRunner.assertTrue(
                                        !LoggerUtil.isDebugEnabled(),
                                        "Debug should be disabled");

                        LoggerUtil.setDebugEnabled(true);
                        SimpleTestRunner.assertTrue(
                                        LoggerUtil.isDebugEnabled(),
                                        "Debug should be enabled");

                        LoggerUtil.setDebugEnabled(false); // Reset
                });

                runner.run("LoggerUtil.logOperationStart - Format correct", () -> {
                        PrintStream originalOut = System.out;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try {
                                LoggerUtil.setDebugEnabled(true);
                                System.setOut(new PrintStream(out));
                                LoggerUtil.logOperationStart("OpSource", "fetchData");
                                String output = out.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("Iniciando: fetchData"),
                                                "Should contain operation start message");
                        } finally {
                                System.setOut(originalOut);
                                LoggerUtil.setDebugEnabled(false);
                        }
                });

                runner.run("LoggerUtil.logOperationSuccess - Format correct", () -> {
                        PrintStream originalOut = System.out;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try {
                                LoggerUtil.setDebugEnabled(true);
                                System.setOut(new PrintStream(out));
                                LoggerUtil.logOperationSuccess("OpSource", "fetchData");
                                String output = out.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("Completado: fetchData"),
                                                "Should contain operation success message");
                        } finally {
                                System.setOut(originalOut);
                                LoggerUtil.setDebugEnabled(false);
                        }
                });

                runner.run("LoggerUtil.logOperationFailure - With exception", () -> {
                        PrintStream originalErr = System.err;
                        ByteArrayOutputStream err = new ByteArrayOutputStream();

                        try {
                                System.setErr(new PrintStream(err));
                                Exception ex = new Exception("Network error");
                                LoggerUtil.logOperationFailure("OpSource", "fetchData", ex);
                                String output = err.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("Falló: fetchData"),
                                                "Should contain operation failure message");
                                SimpleTestRunner.assertTrue(
                                                output.contains("Network error"),
                                                "Should contain exception message");
                        } finally {
                                System.setErr(originalErr);
                        }
                });

                runner.run("LoggerUtil - Null source handling", () -> {
                        PrintStream originalOut = System.out;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try {
                                System.setOut(new PrintStream(out));
                                LoggerUtil.logInfo(null, "No source message");
                                String output = out.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("null"),
                                                "Should handle null source");
                                SimpleTestRunner.assertTrue(
                                                output.contains("No source message"),
                                                "Should still contain message");
                        } finally {
                                System.setOut(originalOut);
                        }
                });

                runner.run("LoggerUtil - Empty message handling", () -> {
                        PrintStream originalOut = System.out;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        try {
                                System.setOut(new PrintStream(out));
                                LoggerUtil.logInfo("Source", "");
                                String output = out.toString();

                                SimpleTestRunner.assertTrue(
                                                output.contains("[INFO]"),
                                                "Should log even with empty message");
                        } finally {
                                System.setOut(originalOut);
                        }
                });
        }
}
