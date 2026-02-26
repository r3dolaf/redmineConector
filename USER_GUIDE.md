# 📖 Ultimate User Guide - Redmine Connector Pro (v2.5)

This guide documents **every** single feature available in the application, from the basics to advanced power-user tools.

---

## 🆕 Recent Updates (v2.5)

### Latest Features
- **Default "Nuevo" Status**: New and cloned tasks automatically get "Nuevo" (New) status by default
- **Bidirectional Twin Closure**: When closing twin tasks, the system now syncs back to the original instance
- **Tracker-Based Status Filtering**: Multi-close and twin closure dialogs show only valid statuses for your task type
- **Comprehensive Help Dialog**: Press F1 for 3-tab help system with shortcuts, features, and productivity tips
- **Enhanced Notes Ordering**: Quick View notes can be sorted newest-first or oldest-first

---

## 📚 Documentation

For technical documentation, architecture, and developer guides, see the **[docs-md/](docs-md/)** directory:
- **[Architecture & Analysis](docs-md/architecture/)** - System architecture, API usage, project analysis
- **[Build Guides](docs-md/build/)** - Compilation and build instructions
- **[User & Developer Guides](docs-md/guides/)** - Configuration, development, and user manuals

---

## 📑 Detailed Table of Contents
1. [Environment & Tab Management](#environment--tab-management)
2. [Task List Panel](#task-list-panel)
3. [Data Filtering (Basic & Advanced)](#data-filtering-basic--advanced)
4. [Task Record: Details & Operations](#task-record-details--operations)
5. [Smart Twin Synchronization](#smart-twin-synchronization)
6. [Time Tracking & Reporting](#time-tracking--reporting)
8. [Kanban Board](#kanban-board)
9. [Global Search (Command Palette)](#global-search-command-palette)
10. [Offline Mode (Work Disconnected)](#offline-mode-work-disconnected)
11. [Visual Customization & Themes](#visual-customization--themes)
12. [Utilities & Exporting](#utilities--exporting)
13. [Keyboard Shortcuts (Cheat Sheet)](#keyboard-shortcuts-cheat-sheet)
14. [Technical Configuration Guide](#technical-configuration-guide)

---

## 1. Environment & Tab Management
- **Multi-Instance**: Open each Redmine connection in its own tab. Configure as many as needed in **File → Client Manager**.
- **Detachable Windows (Detach)**: Using multiple monitors? Click the `↗` icon in the tab header to turn it into an independent window. **Important**: The application remembers which windows were detached and will restore them automatically on startup.
- **Live Renaming**: Change the display name of any client directly in its specific configuration menu.

---

- **Premium Design**: The task list features a subtle **row hover effect** for better visual tracking. Priority icons are anti-aliased and redesigned for a high-end look.
- **Quick View Panel**: Located below the main table, this panel updates instantly when a task is selected. You can read the **Description**, check **Latest Notes**, and inspect **Attachments** (including image previews) without opening any dialogs.
- **Quick Time Control**: From the Quick View panel, you can **Log Hours** or **Close Task** directly without opening the full task record.
- **Attachment Preview**: Select an image in the "Attachments" tab to see a scaled preview directly in the Quick View.
- **Expand/Minimize Control**: Use the `↕` button in the Quick View header to toggle between expanded and compact modes.
- **Task Pinning (Favorites)**: Right-click any task and select "⭐ Pin / Unpin" to mark it as a favorite. Pinned tasks always stay at the top of your list with a gold star icon.
- **Sorting**: Click any column header to sort. Supports multi-column sorting via `Shift + Click`.
- **Column Management**: Right-click the table header to hide or show specific columns.

---

## 3. Data Filtering (Basic & Advanced)
The top filter panel is extremely powerful:
- **ID & Subject**: Instant real-time search.
- **Exclusion Filter**: In the "Exclude" field, enter keywords (comma-separated) to hide unwanted tasks (e.g., `test, meeting`).
- **Date Filtering**: Use calendars to see work performed within a specific range.
- **Quick & Smart Filters**: Directly above the task table, you have a quick access bar for:
    - **My Tasks Only**: Instantly filters by the current user.
    - **Overdue / Today**: Detects tasks with missed deadlines or due today.
    - **By Version / Assignee / Category**: Dropdown menus for filtering by common metadata.
- **Smart Filter Sync**: Selecting a "Tracker" will automatically filter the "Status" list to show only applicable statuses.
- **Quick Clear**: The `❌` button resets all filters to the full view.

---

## 4. Task Record: Details & Operations
Inside any task, you will find:
- **General Details**: Rich view with full description.
- **History/Journals**: Full logs of comments and status changes.
- **Time Logging**: Direct hour registration.
- **Attachments**: List of files available for one-click downloading.
- **Context Menu**:
    - **Copy Subject + Desc**: Formats task data for pasting into chats or emails.
    - **Open in Browser**: Direct jump to the Redmine web interface.
    - **Create Subtask**: Creates a child task linked automatically.
- **Full Export**: When using "Download to Desktop", the generated `Detalles.txt` file now includes the **complete chronological history** of notes and changes.
- **Robust Attachments**: File downloads are now more reliable thanks to a dual-verification API key system.

---

## 5. Smart Twin Synchronization
Optimized for working between two servers (e.g., Client & Provider):
- **Smart Match / Clone**: Copy a task from one server to another. The description will include an automatic back-link.
- **Auto-Detection**: The system scans subjects for IDs to detect existing "twins" on the peer server.
- **Synchronized Closure**: Close both tasks with one click. The system queries both servers for valid workflow transitions.
- **Bulk Closure**: Select multiple tasks for local or synchronized closure based on twin detection.
    - **New**: The system now allows you to explicitly **clear/empty** the version field during bulk closure by selecting the `[Clear / None]` option.

---

## 6. Time Tracking & Reporting
- **Visual Dashboard**: Rich view featuring **comparative bar charts** for status and priority breakdowns. Get an intuitive pulse on project progress instantly. Includes informative tooltips and a **Copy Summary** button for quick reporting.
- **Hour Reports**: Generate detailed tables by user and month.
- **Exporting**: Copy any report table and paste directly into Excel while maintaining format.

---

## 7. Wiki, Versions & Documentation
- **Wiki Browser**: Navigation tree for all project documentation.
- **Wiki History**: View past versions and restore deleted content.
- **Version Manager (Milestones)**:
    - Track percentage progress per version.
    - **Deployment Email**: Generates a ready-to-send email with the version's changelog (closed tasks and summaries).
    - **Automated Refresh**: When creating or modifying a version, selectors across the entire application update instantly without requiring a manual reload.

---

## 8. Kanban Board
Get a visual pulse on your project workflow:
- **Status Lanes**: Tasks are automatically grouped into columns based on their current status.
- **Client Integration**: Access the board from each client's specific menu for a bird's-eye view of progress.

---

## 9. Global Search (Command Palette)
Press `Ctrl + P` to trigger the unified command palette:
- **Cross-Instance Search**: Search for tasks across all configured Redmine servers simultaneously by ID or keywords.
- **Lightning Navigation**: Use arrow keys and Enter to jump instantly to the correct client tab and task details.

---

## 10. Offline Mode (Work Disconnected)
The application is built with an **Offline-First** philosophy:
- **Automatic Detection**: If the connection to Redmine fails during a refresh, the app automatically enters **OFFLINE MODE**.
- **Visual Indicator**: You will see a red "OFFLINE MODE (READ ONLY)" label in the header.
- **Data Availability**: You will still have access to the last successfully loaded task list and metadata, thanks to the persistent caching system.
- **Write Restriction**: To prevent conflicts, all modification operations (creating tasks, logging time, uploading attachments) are automatically disabled until connection is restored.
- **Recovery**: Click the "Retry Connection" button to attempt to exit offline mode. If the refresh succeeds, the app returns to full normal operation.

---

## 11. Visual Customization & Themes
- **Predefined Themes**: Light and Dark modes.
- **Theme Editor**: In **View → Theme → Custom**, change background, header, and button colors.
- **Status Color Config**: Assign specific colors to each status (e.g., "In Progress" in blue, "New" in orange) per client.

---

## 9. Utilities & Exporting
- **CSV Export**: Save current task list to a CSV file.
- **Keywords Analysis**: Quickly see the most frequent words in task subjects.
- **Global Log**: Bottom panel logging connection success or detailed errors.
    - **Source Filtering**: You can now filter logs to see only those from a specific client (e.g., "Show only logs from Server A") or system-only messages.
    - **High Availability**: The caching system is now thread-safe, ensuring stability in intensive multi-client environments.

---

## 10. Keyboard Shortcuts (Cheat Sheet)

| Shortcut | Action |
| :--- | :--- |
| **Ctrl+P** | Global Search (Command Palette) |
| **F5 / Ctrl+R** | Full Data Refresh |
| **Ctrl+N** | Quick New Task |
| **Ctrl+F** | Focus Search Bar |
| **Enter** | Open Task Details |
| **Ctrl+E** | Edit Task |
| **Ctrl + [1..9]** | Jump Between Client Tabs |
| **Ctrl+Shift+C** | Copy Task ID Only |
| **F1** | Open Shortcut Help |
| **Esc** | Close Any Popup/Dialog |

---

## 11. Technical Configuration Guide

When accessing a client's settings (**Gear Icon ⚙️**), you will see the following fields. Here we explain their exact purpose:

### 🌐 Basic Connection
- **URL**: Your Redmine server address (e.g., `https://redmine.mycompany.com`). Make sure it ends with a `/`.
- **API Key**: Your personal access token. Find it in your Redmine profile (My account) on the right sidebar ("API access key").
- **Project ID**: The alphanumeric identifier of your project (the one shown in the web browser's URL).

### ⚙️ Behavior
- **Task Limit**: Maximum number of tasks the app will download per refresh. If set to `0`, it uses the server's default limit (usually 100).
- **Refresh Interval**: How many minutes between automatic server checks for updates.
- **Download Path**: Directory on your computer where attachments and documents will be saved when using the "Download to Desktop" function.

### 🔗 Smart Twin Synchronization
- **Reference Pattern**: This field is **CRITICAL**. Here you define how cross-references are written.
  - Example: If you set `[Ref #{id}]`, the system will search for this text in the subjects of tasks on the peer server. When cloning a task, the system will write the reference in this format.
- **Folder Pattern**: Defines how folders are named when downloading a task. You can use variables:
  - `{id}`: Task ID.
  - `{subject}`: Task Subject.
  - `{priority}`: Priority.
  - Example: `{id}_{subject}` will create folders like `12345_Fix login bug`.

### 🔔 Notifications & Others
- **Show Closed by Default**: If checked, the tab will show all tasks. If not, only open tasks are displayed initially.
- **Include Epics**: Determines if top-level parent tasks should be listed.
- **Attachment Format**: Choose between `Textile` or `Markdown` based on your Redmine configuration to ensure consistent Wiki and description editing.

### ⚠️ Validations
- **Required Subject**: Creating tasks without a subject is now prevented to avoid orphaned records on the server.

---

*(También disponible en [Castellano](MANUAL_ES.md))*

**© 2025 Redmine Connector Pro - The definitive tool for Redmine experts.**
 Broadway
