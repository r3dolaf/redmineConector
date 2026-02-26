package redmineconnector.test;

import redmineconnector.test.SimpleTestRunner;
import redmineconnector.util.AppConstants;

/**
 * Tests for AppConstants to validate constant values and relationships.
 */
public class AppConstantsTest {

    public static void runTests(SimpleTestRunner runner) {

        runner.run("AppConstants - Cache TTL values", () -> {
            SimpleTestRunner.assertTrue(
                    AppConstants.CACHE_TTL_MS > 0,
                    "Cache TTL should be positive");
            SimpleTestRunner.assertTrue(
                    AppConstants.IMAGE_CACHE_TTL_MS > AppConstants.CACHE_TTL_MS,
                    "Image cache should have longer TTL than general cache");
        });

        runner.run("AppConstants - HTTP configuration", () -> {
            SimpleTestRunner.assertTrue(
                    AppConstants.HTTP_TIMEOUT_MS >= 10000,
                    "HTTP timeout should be at least 10 seconds");
            SimpleTestRunner.assertTrue(
                    AppConstants.DEFAULT_FETCH_BATCH_SIZE > 0 && AppConstants.DEFAULT_FETCH_BATCH_SIZE <= 1000,
                    "Batch size should be reasonable (1-1000)");
            SimpleTestRunner.assertTrue(
                    AppConstants.MAX_BULK_BATCH_SIZE <= AppConstants.DEFAULT_FETCH_BATCH_SIZE,
                    "Bulk batch size should not exceed default batch size");
        });

        runner.run("AppConstants - Dialog dimensions", () -> {
            // Width progression
            SimpleTestRunner.assertTrue(
                    AppConstants.DIALOG_WIDTH_SMALL < AppConstants.DIALOG_WIDTH_MEDIUM,
                    "Medium dialog should be wider than small");
            SimpleTestRunner.assertTrue(
                    AppConstants.DIALOG_WIDTH_MEDIUM < AppConstants.DIALOG_WIDTH_LARGE,
                    "Large dialog should be wider than medium");
            SimpleTestRunner.assertTrue(
                    AppConstants.DIALOG_WIDTH_LARGE < AppConstants.DIALOG_WIDTH_XL,
                    "XL dialog should be wider than large");

            // Height progression
            SimpleTestRunner.assertTrue(
                    AppConstants.DIALOG_HEIGHT_SMALL < AppConstants.DIALOG_HEIGHT_MEDIUM,
                    "Medium dialog should be taller than small");
            SimpleTestRunner.assertTrue(
                    AppConstants.DIALOG_HEIGHT_MEDIUM < AppConstants.DIALOG_HEIGHT_LARGE,
                    "Large dialog should be taller than medium");

            // Reasonable sizes
            SimpleTestRunner.assertTrue(
                    AppConstants.DIALOG_WIDTH_SMALL >= 300,
                    "Small dialog should be at least 300px wide");
            SimpleTestRunner.assertTrue(
                    AppConstants.DIALOG_HEIGHT_SMALL >= 200,
                    "Small dialog should be at least 200px tall");
        });

        runner.run("AppConstants - Column widths", () -> {
            SimpleTestRunner.assertTrue(
                    AppConstants.COLUMN_WIDTH_SMALL < AppConstants.COLUMN_WIDTH_MEDIUM,
                    "Medium column should be wider than small");
            SimpleTestRunner.assertTrue(
                    AppConstants.COLUMN_WIDTH_MEDIUM < AppConstants.COLUMN_WIDTH_LARGE,
                    "Large column should be wider than medium");
        });

        runner.run("AppConstants - UI limits", () -> {
            SimpleTestRunner.assertTrue(
                    AppConstants.MAX_RECENT_ITEMS > 0 && AppConstants.MAX_RECENT_ITEMS <= 50,
                    "Recent items should be reasonable (1-50)");
            SimpleTestRunner.assertTrue(
                    AppConstants.TABLE_ROW_HEIGHT > 15 && AppConstants.TABLE_ROW_HEIGHT < 100,
                    "Row height should be reasonable (15-100px)");
            SimpleTestRunner.assertTrue(
                    AppConstants.QUICK_VIEW_MIN_HEIGHT < AppConstants.QUICK_VIEW_MAX_HEIGHT,
                    "Quick view max should be greater than min");
        });

        runner.run("AppConstants - Content limits", () -> {
            SimpleTestRunner.assertTrue(
                    AppConstants.MAX_SUBJECT_DISPLAY_LENGTH > 0,
                    "Subject display length should be positive");
            SimpleTestRunner.assertTrue(
                    AppConstants.MAX_DESCRIPTION_PREVIEW_LENGTH > AppConstants.MAX_SUBJECT_DISPLAY_LENGTH,
                    "Description preview should be longer than subject");
            SimpleTestRunner.assertTrue(
                    AppConstants.MAX_INLINE_ATTACHMENTS > 0 && AppConstants.MAX_INLINE_ATTACHMENTS <= 50,
                    "Inline attachments should be reasonable (1-50)");
        });

        runner.run("AppConstants - Validation limits", () -> {
            SimpleTestRunner.assertTrue(
                    AppConstants.MIN_API_KEY_LENGTH >= 8,
                    "API key minimum length should be at least 8 characters");
            SimpleTestRunner.assertTrue(
                    AppConstants.MAX_CONCURRENT_REQUESTS > 0 && AppConstants.MAX_CONCURRENT_REQUESTS <= 20,
                    "Concurrent requests should be reasonable (1-20)");
            SimpleTestRunner.assertTrue(
                    AppConstants.MAX_RETRY_ATTEMPTS > 0 && AppConstants.MAX_RETRY_ATTEMPTS <= 10,
                    "Retry attempts should be reasonable (1-10)");
            SimpleTestRunner.assertTrue(
                    AppConstants.RETRY_DELAY_MS >= 100,
                    "Retry delay should be at least 100ms");
        });

        runner.run("AppConstants - Thread pool configuration", () -> {
            SimpleTestRunner.assertTrue(
                    AppConstants.ASYNC_CORE_POOL_SIZE > 0,
                    "Core pool size should be positive");
            SimpleTestRunner.assertTrue(
                    AppConstants.ASYNC_MAX_POOL_SIZE >= AppConstants.ASYNC_CORE_POOL_SIZE,
                    "Max pool size should be >= core pool size");
            SimpleTestRunner.assertTrue(
                    AppConstants.THREAD_KEEP_ALIVE_SEC > 0,
                    "Thread keep-alive should be positive");
        });

        runner.run("AppConstants - String constants not null", () -> {
            SimpleTestRunner.assertNotNull(AppConstants.CSV_SEPARATOR, "CSV separator should not be null");
            SimpleTestRunner.assertNotNull(AppConstants.CSV_LINE_SEPARATOR, "CSV line separator should not be null");
            SimpleTestRunner.assertNotNull(AppConstants.EXPORT_ENCODING, "Export encoding should not be null");
            SimpleTestRunner.assertNotNull(AppConstants.DEFAULT_DOWNLOAD_FOLDER, "Download folder should not be null");
            SimpleTestRunner.assertNotNull(AppConstants.CONFIG_FILE_NAME, "Config file name should not be null");
            SimpleTestRunner.assertNotNull(AppConstants.LOG_FILE_NAME, "Log file name should not be null");
        });

        runner.run("AppConstants - Application metadata", () -> {
            SimpleTestRunner.assertNotNull(AppConstants.APP_NAME, "App name should not be null");
            SimpleTestRunner.assertNotNull(AppConstants.APP_VERSION, "App version should not be null");
            SimpleTestRunner.assertNotNull(AppConstants.USER_AGENT, "User agent should not be null");
            SimpleTestRunner.assertTrue(
                    AppConstants.USER_AGENT.contains(AppConstants.APP_NAME),
                    "User agent should contain app name");
            SimpleTestRunner.assertTrue(
                    AppConstants.USER_AGENT.contains(AppConstants.APP_VERSION),
                    "User agent should contain app version");
        });

        runner.run("AppConstants - Notification configuration", () -> {
            SimpleTestRunner.assertTrue(
                    AppConstants.NOTIFICATION_DURATION_MS > 0,
                    "Notification duration should be positive");
            SimpleTestRunner.assertTrue(
                    AppConstants.NOTIFICATION_CHECK_INTERVAL_MIN > 0,
                    "Notification check interval should be positive");
            SimpleTestRunner.assertTrue(
                    AppConstants.MAX_SIMULTANEOUS_NOTIFICATIONS > 0,
                    "Max simultaneous notifications should be positive");
        });
    }
}
