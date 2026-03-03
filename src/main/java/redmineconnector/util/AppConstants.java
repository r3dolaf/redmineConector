package redmineconnector.util;

/**
 * Centralized application constants to eliminate magic numbers and improve
 * maintainability.
 * 
 * <p>
 * This class contains all configurable constants used throughout the Redmine
 * Connector application,
 * organized by functional area. Using these constants instead of hardcoded
 * values makes the codebase
 * more maintainable and allows for easier tuning of application behavior.
 * 
 * <h2>Usage Examples:</h2>
 * 
 * <pre>
 * // Cache configuration
 * CachedEntry entry = cache.get(key);
 * if (entry.isExpired(AppConstants.CACHE_TTL_MS)) {
 *     // Refresh cache
 * }
 * 
 * // HTTP batch operations
 * List&lt;Task&gt; tasks = fetchTasks(project, false, AppConstants.DEFAULT_FETCH_BATCH_SIZE);
 * 
 * // Thread pool configuration
 * ThreadPoolExecutor executor = new ThreadPoolExecutor(
 *         AppConstants.ASYNC_CORE_POOL_SIZE,
 *         AppConstants.ASYNC_MAX_POOL_SIZE,
 *         AppConstants.THREAD_KEEP_ALIVE_SEC, TimeUnit.SECONDS,
 *         new LinkedBlockingQueue&lt;&gt;());
 * </pre>
 * 
 * <h2>Implementation References:</h2>
 * <ul>
 * <li>{@code CACHE_TTL_MS} - Used in: HttpDataService.fetchMetadata()</li>
 * <li>{@code DEFAULT_FETCH_BATCH_SIZE} - Used in: HttpDataService.fetchTasks(),
 * fetchTimeEntries(), fetchClosedTasks()</li>
 * <li>{@code MAX_BULK_BATCH_SIZE} - Used in:
 * HttpDataService.fetchTasksByIds()</li>
 * <li>{@code ASYNC_*_POOL_SIZE} - Used in: AsyncDataService constructor</li>
 * <li>{@code MAX_SUBJECT_DISPLAY_LENGTH} - Used in:
 * TaskOperations.downloadTasksWithProgress()</li>
 * </ul>
 * 
 * @author Redmine Connector Team
 * @version 2.6
 * @since 2.6
 */
public final class AppConstants {

    // Prevent instantiation
    private AppConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }

    // ==================== Cache Configuration ====================

    /** Default cache TTL in milliseconds (5 minutes) */
    public static final long CACHE_TTL_MS = 5 * 60 * 1000;

    /** Image cache TTL in milliseconds (10 minutes) */
    public static final long IMAGE_CACHE_TTL_MS = 10 * 60 * 1000;

    // ==================== HTTP Configuration ====================

    /** Default HTTP timeout in milliseconds (30 seconds) */
    public static final int HTTP_TIMEOUT_MS = 30 * 1000;

    /** Default batch size for fetching tasks */
    public static final int DEFAULT_FETCH_BATCH_SIZE = 100;

    /** Maximum batch size for bulk operations */
    public static final int MAX_BULK_BATCH_SIZE = 50;

    // ==================== UI Configuration ====================

    /** Default refresh interval in minutes */
    public static final int DEFAULT_REFRESH_INTERVAL_MIN = 5;

    /** Maximum number of recent items in history */
    public static final int MAX_RECENT_ITEMS = 10;

    /** Default table row height in pixels */
    public static final int TABLE_ROW_HEIGHT = 25;

    /** Quick view panel minimum height */
    public static final int QUICK_VIEW_MIN_HEIGHT = 200;

    /** Quick view panel maximum height */
    public static final int QUICK_VIEW_MAX_HEIGHT = 600;

    // ==================== File/Path Configuration ====================

    /** Default download folder name */
    public static final String DEFAULT_DOWNLOAD_FOLDER = "RedmineDownloads";

    /** Configuration file name */
    public static final String CONFIG_FILE_NAME = "redmine_config.properties";

    /** Log file name */
    public static final String LOG_FILE_NAME = "redmine_connector.log";

    /** Backup file suffix */
    public static final String BACKUP_SUFFIX = ".backup";

    // ==================== Notification Configuration ====================

    /** Notification display duration in milliseconds (5 seconds) */
    public static final int NOTIFICATION_DURATION_MS = 5 * 1000;

    /** Notification check interval in minutes */
    public static final int NOTIFICATION_CHECK_INTERVAL_MIN = 2;

    /** Maximum number of simultaneous notifications */
    public static final int MAX_SIMULTANEOUS_NOTIFICATIONS = 3;

    // ==================== Dialog Dimensions ====================

    /** Default dialog width - small dialogs */
    public static final int DIALOG_WIDTH_SMALL = 400;

    /** Default dialog width - medium dialogs */
    public static final int DIALOG_WIDTH_MEDIUM = 600;

    /** Default dialog width - large dialogs */
    public static final int DIALOG_WIDTH_LARGE = 820;

    /** Default dialog width - extra large dialogs */
    public static final int DIALOG_WIDTH_XL = 1000;

    /** Default dialog height - small dialogs */
    public static final int DIALOG_HEIGHT_SMALL = 300;

    /** Default dialog height - medium dialogs */
    public static final int DIALOG_HEIGHT_MEDIUM = 500;

    /** Default dialog height - large dialogs */
    public static final int DIALOG_HEIGHT_LARGE = 650;

    /** Table column width - ID/small columns */
    public static final int COLUMN_WIDTH_SMALL = 50;

    /** Table column width - medium columns */
    public static final int COLUMN_WIDTH_MEDIUM = 200;

    /** Table column width - large columns (descriptions) */
    public static final int COLUMN_WIDTH_LARGE = 400;

    /** Split pane divider default location */
    public static final int SPLIT_PANE_DIVIDER_LOCATION = 300;

    // ==================== Task/Content Limits ====================

    /** Maximum subject length for display truncation */
    public static final int MAX_SUBJECT_DISPLAY_LENGTH = 100;

    /** Maximum description preview length */
    public static final int MAX_DESCRIPTION_PREVIEW_LENGTH = 500;

    /** Maximum number of attachments to display inline */
    public static final int MAX_INLINE_ATTACHMENTS = 10;

    // ==================== Validation/Limits ====================

    /** Minimum password/API key length */
    public static final int MIN_API_KEY_LENGTH = 16;

    /** Maximum concurrent HTTP requests */
    public static final int MAX_CONCURRENT_REQUESTS = 5;

    /** Retry attempts for failed operations */
    public static final int MAX_RETRY_ATTEMPTS = 3;

    /** Retry delay in milliseconds */
    public static final int RETRY_DELAY_MS = 1000;

    // ==================== Export Configuration ====================

    /** CSV field separator */
    public static final String CSV_SEPARATOR = ",";

    /** CSV line separator */
    public static final String CSV_LINE_SEPARATOR = System.lineSeparator();

    /** Default export file encoding */
    public static final String EXPORT_ENCODING = "UTF-8";

    // ==================== Thread Pool Configuration ====================

    /** Core thread pool size for async operations */
    public static final int ASYNC_CORE_POOL_SIZE = 3;

    /** Maximum thread pool size */
    public static final int ASYNC_MAX_POOL_SIZE = 10;

    /** Thread keep-alive time in seconds */
    public static final int THREAD_KEEP_ALIVE_SEC = 60;

    // ==================== Color Constants ====================

    /** Primary accent color (Cornflower Blue) */
    public static final java.awt.Color COLOR_PRIMARY = new java.awt.Color(100, 149, 237);

    /** Success color (Green) */
    public static final java.awt.Color COLOR_SUCCESS = new java.awt.Color(76, 175, 80);

    /** Warning color (Orange) */
    public static final java.awt.Color COLOR_WARNING = new java.awt.Color(255, 152, 0);

    /** Error color (Red) */
    public static final java.awt.Color COLOR_ERROR = new java.awt.Color(244, 67, 54);

    /** Info color (Blue) */
    public static final java.awt.Color COLOR_INFO = new java.awt.Color(33, 150, 243);

    /** Light gray for borders */
    public static final java.awt.Color COLOR_BORDER = new java.awt.Color(200, 200, 200);

    /** Background color for panels */
    public static final java.awt.Color COLOR_PANEL_BG = new java.awt.Color(250, 250, 250);

    /** Background color for selected items */
    public static final java.awt.Color COLOR_SELECTED = new java.awt.Color(230, 240, 255);

    /** Text gray for secondary information */
    public static final java.awt.Color COLOR_TEXT_GRAY = new java.awt.Color(100, 100, 100);

    /** Background color for empty/missing data */
    public static final java.awt.Color COLOR_EMPTY = new java.awt.Color(255, 200, 100);

    /** Background color for deviation/alert */
    public static final java.awt.Color COLOR_DEVIATION = new java.awt.Color(255, 200, 200);

    // ==================== Default Values ====================

    /** Default task status for new tasks */
    public static final String DEFAULT_NEW_TASK_STATUS = "Nuevo";

    /** Default task priority */
    public static final String DEFAULT_TASK_PRIORITY = "Normal";

    /** Default tracker type */
    public static final String DEFAULT_TRACKER = "Bug";

    // ==================== Application Metadata ====================

    /** Application name */
    public static final String APP_NAME = "Redmine Connector Pro";

    /** Application version */
    public static final String APP_VERSION = "2.6";

    /** User agent for HTTP requests */
    public static final String USER_AGENT = APP_NAME + "/" + APP_VERSION;
}
