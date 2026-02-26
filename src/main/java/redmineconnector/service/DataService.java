package redmineconnector.service;

import java.util.List;

import redmineconnector.model.Attachment;
import redmineconnector.model.CustomFieldDefinition;
import redmineconnector.model.SimpleEntity;
import redmineconnector.model.Task;
import redmineconnector.model.TimeEntry;
import redmineconnector.model.VersionDTO;
import redmineconnector.model.WikiPageDTO;
import redmineconnector.model.WikiVersionDTO;
import redmineconnector.model.TimeEntry;
import redmineconnector.model.VersionDTO;
import redmineconnector.model.WikiPageDTO;
import redmineconnector.model.WikiVersionDTO;

/**
 * Core data service interface for Redmine API interactions.
 * 
 * <p>
 * This interface defines all available operations for interacting with a
 * Redmine server,
 * including task management, version control, wiki operations, and time
 * tracking.
 * </p>
 * 
 * <p>
 * <b>Thread Safety:</b> Implementations should be thread-safe as methods may be
 * called
 * from multiple threads concurrently (e.g., async operations, background
 * refresh).
 * </p>
 * 
 * <p>
 * <b>Exception Handling:</b> All methods throw {@code Exception} to allow
 * implementations
 * flexibility in error handling. Callers should wrap calls with appropriate
 * error handling
 * and use {@link redmineconnector.util.LoggerUtil} for logging.
 * </p>
 * 
 * <h2>Implementation Notes</h2>
 * <ul>
 * <li>Default implementation:
 * {@link redmineconnector.service.impl.RedmineDataServiceImpl}</li>
 * <li>Async wrapper: {@link redmineconnector.service.AsyncDataService}</li>
 * <li>All date parameters use format: "yyyy-MM-dd" or "dd/MM/yyyy" as
 * specified</li>
 * <li>Project ID (pid) can be numeric ID or project identifier string</li>
 * </ul>
 * 
 * @author Redmine Connector Team
 * @version 9.0
 * @see redmineconnector.service.impl.RedmineDataServiceImpl
 * @see redmineconnector.service.AsyncDataService
 */
public interface DataService {

        // ========== TASK MANAGEMENT ==========

        /**
         * Fetches tasks from a Redmine project with optional filtering.
         * 
         * @param pid    Project ID or identifier
         * @param closed If true, includes closed tasks; if false, only open tasks
         * @param limit  Maximum number of tasks to fetch (API limit: typically 100)
         * @return List of tasks matching the criteria
         * @throws Exception if API call fails, authentication fails, or project doesn't
         *                   exist
         */
        List<Task> fetchTasks(String pid, boolean closed, int limit) throws Exception;

        /**
         * Fetches detailed information for a specific task.
         * 
         * <p>
         * This method retrieves complete task information including custom fields,
         * journals (history), and related data that are not available in list views.
         * </p>
         * 
         * @param id Unique task ID
         * @return Task with full details including history and custom fields
         * @throws Exception if task doesn't exist or access is denied
         */
        Task fetchTaskDetails(int id) throws Exception;

        /**
         * Creates a new task in the specified project.
         * 
         * <p>
         * <b>Required Task fields:</b>
         * </p>
         * <ul>
         * <li>{@code subject} - Task title</li>
         * <li>{@code trackerId} - Type of task (Bug, Feature, etc.)</li>
         * </ul>
         * 
         * <p>
         * <b>Optional fields:</b> description, statusId, priorityId, assignedToId,
         * categoryId, etc.
         * </p>
         * 
         * @param pid  Project ID where task will be created
         * @param task Task object with all desired fields populated
         * @return ID of the newly created task
         * @throws Exception if validation fails or project doesn't allow task creation
         */
        int createTask(String pid, Task task) throws Exception;

        /**
         * Updates an existing task with modified field values.
         * 
         * <p>
         * Only non-null fields in the Task object will be updated. To add notes,
         * populate the {@code notes} field.
         * </p>
         * 
         * <p>
         * <b>Note:</b> This operation may trigger workflow validations. Ensure status
         * transitions are allowed for the current tracker type.
         * </p>
         * 
         * @param task Task object with id and fields to update
         * @throws Exception if task doesn't exist, validation fails, or workflow
         *                   prevents update
         */
        void updateTask(Task task) throws Exception;

        /**
         * Fetches tasks by a list of specific IDs.
         * 
         * <p>
         * This is an optimized bulk fetch operation useful for loading details of
         * multiple known tasks without fetching entire project listings.
         * </p>
         * 
         * @param ids List of task IDs to fetch
         * @return List of tasks with full details (tasks not found or inaccessible are
         *         omitted)
         * @throws Exception if API call fails
         * @since 9.0
         */
        List<Task> fetchTasksByIds(List<Integer> ids) throws Exception;

        // ========== METADATA ==========

        /**
         * Fetches metadata entities for a project (statuses, priorities, trackers,
         * users, etc.).
         * 
         * <p>
         * <b>Supported types:</b>
         * </p>
         * <ul>
         * <li>{@code "statuses"} - Available task statuses</li>
         * <li>{@code "priorities"} - Task priority list</li>
         * <li>{@code "trackers"} - Task type/tracker list</li>
         * <li>{@code "users"} - Project members</li>
         * <li>{@code "categories"} - Task categories</li>
         * <li>{@code "versions"} - Project versions/milestones</li>
         * </ul>
         * 
         * @param type Metadata type to fetch (case-sensitive)
         * @param pid  Project ID for project-specific metadata
         * @return List of simple entities with id and name
         * @throws Exception if type is invalid or project doesn't exist
         */
        List<SimpleEntity> fetchMetadata(String type, String pid) throws Exception;

        /**
         * Fetches all available custom field definitions.
         * 
         * @return List of CustomFieldDefinition
         * @throws Exception if API call fails
         */
        List<CustomFieldDefinition> fetchCustomFieldDefinitions() throws Exception;

        /**
         * Fetches allowed status transitions for a specific task.
         * 
         * <p>
         * Redmine uses workflow rules to restrict status transitions based on
         * tracker type and current status. This method returns only valid target
         * statuses.
         * </p>
         * 
         * @param pid       Project ID
         * @param trackerId Tracker/type ID of the task
         * @param issueId   Task ID (0 for new tasks)
         * @return List of allowed statuses for transition
         * @throws Exception if workflow configuration is invalid
         * @since 8.5
         */
        List<SimpleEntity> fetchAllowedStatuses(String pid, int trackerId, int issueId) throws Exception;

        /**
         * Fetches the current authenticated user information.
         * 
         * <p>
         * Uses the API key to identify the logged-in user. Useful for auto-assigning
         * tasks and determining user permissions.
         * </p>
         * 
         * @return SimpleEntity with user ID and name
         * @throws Exception if API key is invalid or user doesn't exist
         */
        SimpleEntity fetchCurrentUser() throws Exception;

        /**
         * Fetches project details (ID and Name) by identifier.
         * 
         * @param identifier Project identifier slug
         * @return SimpleEntity with ID and Name
         */
        SimpleEntity fetchProject(String identifier) throws Exception;

        redmineconnector.model.ContextMetadata fetchContextMetadata(String projectId, int trackerId, int issueId)
                        throws Exception;

        // ========== ATTACHMENTS ==========

        /**
         * Uploads a file to Redmine and returns an upload token.
         * 
         * <p>
         * This is a two-step process in Redmine API:
         * </p>
         * <ol>
         * <li>Upload file content → receive token</li>
         * <li>Attach token to task/wiki via update operation</li>
         * </ol>
         * 
         * @param data        Raw file bytes
         * @param contentType MIME type (e.g., "image/png", "application/pdf")
         * @return Upload token to be used in task/wiki update
         * @throws Exception if upload fails or file size exceeds server limit
         */
        String uploadFile(byte[] data, String contentType) throws Exception;

        /**
         * Downloads attachment content from Redmine.
         * 
         * @param att Attachment object with content URL
         * @return Raw file bytes
         * @throws Exception if attachment doesn't exist or download fails
         */
        byte[] downloadAttachment(Attachment att) throws Exception;

        // ========== TIME TRACKING ==========

        /**
         * Logs time entry for a specific task.
         * 
         * @param issueId    Task ID where time is logged
         * @param date       Date of work in format "yyyy-MM-dd"
         * @param hours      Hours worked (decimal, e.g., 2.5 for 2h 30min)
         * @param userId     User ID who performed the work
         * @param activityId Activity type ID (Development, Design, Testing, etc.)
         * @param comment    Optional description of work performed (can be null)
         * @throws Exception if task doesn't exist or time tracking is disabled
         */
        void logTime(int issueId, String date, double hours, int userId, int activityId, String comment)
                        throws Exception;

        /**
         * Fetches all time entries for a project within a date range.
         * 
         * <p>
         * Used for generating time reports and statistics.
         * </p>
         * 
         * @param pid      Project ID
         * @param dateFrom Start date inclusive, format "yyyy-MM-dd"
         * @param dateTo   End date inclusive, format "yyyy-MM-dd"
         * @return List of time entries with user, task, and hours information
         * @throws Exception if date format is invalid or project doesn't exist
         */
        List<TimeEntry> fetchTimeEntries(String pid, String dateFrom, String dateTo) throws Exception;

        // ========== VERSION MANAGEMENT ==========

        /**
         * Fetches all versions/milestones for a project with full details.
         * 
         * @param pid Project ID
         * @return List of versions with status, dates, and metadata
         * @throws Exception if project doesn't exist
         */
        List<VersionDTO> fetchVersionsFull(String pid) throws Exception;

        /**
         * Fetches all tasks associated with a specific version.
         * 
         * @param pid       Project ID
         * @param versionId Version ID
         * @return List of tasks targeting this version
         * @throws Exception if version doesn't exist
         */
        List<Task> fetchTasksByVersion(String pid, int versionId) throws Exception;

        /**
         * Fetches closed tasks within a date range.
         * 
         * <p>
         * Useful for generating deployment reports and version summaries.
         * </p>
         * 
         * @param pid      Project ID
         * @param dateFrom Closure start date, format "yyyy-MM-dd"
         * @param dateTo   Closure end date, format "yyyy-MM-dd"
         * @return List of tasks closed in the specified period
         * @throws Exception if date format is invalid
         */
        List<Task> fetchClosedTasks(String pid, String dateFrom, String dateTo) throws Exception;

        /**
         * Creates a new version/milestone in the project.
         * 
         * @param pid       Project ID
         * @param name      Version name (e.g., "v1.0.0", "Sprint 23")
         * @param status    Version status: "open", "locked", or "closed"
         * @param startDate Start date in format "yyyy-MM-dd" (can be null)
         * @param dueDate   Due date in format "yyyy-MM-dd" (can be null)
         * @throws Exception if validation fails or project doesn't exist
         */
        void createVersion(String pid, String name, String status, String startDate, String dueDate) throws Exception;

        /**
         * Updates an existing version/milestone.
         * 
         * @param id        Version ID
         * @param name      New version name
         * @param status    New status: "open", "locked", or "closed"
         * @param startDate New start date (format: "yyyy-MM-dd", null to keep
         *                  unchanged)
         * @param dueDate   New due date (format: "yyyy-MM-dd", null to keep unchanged)
         * @throws Exception if version doesn't exist or validation fails
         */
        void updateVersion(int id, String name, String status, String startDate, String dueDate) throws Exception;

        /**
         * Deletes a version/milestone.
         * 
         * <p>
         * <b>Warning:</b> This operation may fail if tasks are still assigned to this
         * version.
         * Consider reassigning tasks first or using version locking instead of
         * deletion.
         * </p>
         * 
         * @param id Version ID to delete
         * @throws Exception if version doesn't exist or has dependent tasks
         */
        void deleteVersion(int id) throws Exception;

        // ========== WIKI MANAGEMENT ==========

        /**
         * Fetches all wiki pages for a project.
         * 
         * @param projectId Project ID or identifier
         * @return List of wiki pages (typically just title and version info)
         * @throws Exception if project doesn't exist or wiki is disabled
         */
        List<WikiPageDTO> fetchWikiPages(String projectId) throws Exception;

        /**
         * Fetches full content of a specific wiki page.
         * 
         * @param projectId Project ID or identifier
         * @param pageTitle Wiki page title (case-sensitive)
         * @return WikiPageDTO with full text content and metadata
         * @throws Exception if page doesn't exist
         */
        WikiPageDTO fetchWikiPageContent(String projectId, String pageTitle) throws Exception;

        /**
         * Fetches version history of a wiki page.
         * 
         * @param projectId Project ID or identifier
         * @param pageTitle Wiki page title
         * @return List of versions with author, date, and comments
         * @throws Exception if page doesn't exist
         */
        List<WikiVersionDTO> fetchWikiHistory(String projectId, String pageTitle) throws Exception;

        /**
         * Creates a new wiki page or updates an existing one.
         * 
         * <p>
         * Redmine wiki uses Textile markup format by default.
         * </p>
         * 
         * @param projectId Project ID or identifier
         * @param pageTitle Wiki page title (alphanumeric and underscores)
         * @param content   Page content in Textile/Markdown format
         * @param comment   Optional change comment for history (can be null)
         * @throws Exception if title is invalid or project doesn't exist
         */
        void createOrUpdateWikiPage(String projectId, String pageTitle, String content, String comment)
                        throws Exception;

        /**
         * Reverts a wiki page to a previous version.
         * 
         * <p>
         * Creates a new version with content from the specified historical version.
         * </p>
         * 
         * @param projectId Project ID or identifier
         * @param pageTitle Wiki page title
         * @param version   Historical version number to revert to
         * @throws Exception if page or version doesn't exist
         */
        void revertWikiPage(String projectId, String pageTitle, int version) throws Exception;

        /**
         * Deletes a wiki page permanently.
         * 
         * <p>
         * <b>Warning:</b> This operation cannot be undone.
         * </p>
         * 
         * @param projectId Project ID or identifier
         * @param pageTitle Wiki page title to delete
         * @throws Exception if page doesn't exist or user lacks permissions
         */
        void deleteWikiPage(String projectId, String pageTitle) throws Exception;

        /**
         * Uploads an attachment to a wiki page.
         * 
         * <p>
         * <b>Important:</b> Redmine wiki API requires current page text and version
         * to prevent "Text cannot be blank" and optimistic locking errors.
         * </p>
         * 
         * @param projectId   Project ID or identifier
         * @param pageTitle   Wiki page title receiving the attachment
         * @param token       Upload token from {@link #uploadFile(byte[], String)}
         * @param filename    Original filename with extension
         * @param contentType MIME type of the file
         * @param currentText Current wiki page content (required for API consistency)
         * @param version     Current page version number (for optimistic locking)
         * @throws Exception if upload fails or page has been modified (version
         *                   mismatch)
         */
        void uploadWikiAttachment(String projectId, String pageTitle, String token, String filename, String contentType,
                        String currentText, int version) throws Exception;

        // ========== LIFECYCLE ==========

        /**
         * Performs cleanup and releases resources.
         * 
         * <p>
         * Default implementation does nothing. Implementations with connection pools,
         * caches, or open resources should override this method.
         * </p>
         * 
         * <p>
         * Called when:
         * </p>
         * <ul>
         * <li>Application shuts down</li>
         * <li>Client configuration is reloaded</li>
         * <li>Service instance is replaced</li>
         * </ul>
         */
        default void shutdown() {
        }
}
