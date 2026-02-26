# ⚙️ Redmine Connector - Configuration Guide

## Table of Contents
1. [Configuration File Structure](#configuration-file-structure)
2. [Client Configuration](#client-configuration)
3. [Connection Settings](#connection-settings)
4. [Behavior Settings](#behavior-settings)
5. [Twin Synchronization](#twin-synchronization)
6. [Notification Settings](#notification-settings)
7. [Advanced Settings](#advanced-settings)
8. [Troubleshooting](#troubleshooting)

---

## 1. Configuration File Structure

### 1.1 Main Configuration File

**Location**: `redmine_config.properties` (same directory as JAR)

**Format**: Java Properties file (key=value pairs)

**Encoding**: UTF-8

**Example Structure**:
```properties
# Global Settings
app.language=es
clients.list=prod,dev,client

# Client 1: Production
prod.clientName=Production Server
prod.url=https://redmine.company.com/
prod.apiKey=abc123def456...
prod.projectId=my-project
# ... more settings

# Client 2: Development
dev.clientName=Dev Environment
dev.url=https://dev-redmine.company.com/
# ... more settings
```

---

## 2. Client Configuration

### 2.1 Client List

**Property**: `clients.list`

**Format**: Comma-separated list of client IDs

**Example**:
```properties
clients.list=prod,dev,staging,client-a,client-b
```

**Rules**:
- Client IDs must be unique
- Use alphanumeric characters and hyphens only
- No spaces allowed
- Order determines tab order in application

### 2.2 Client Name

**Property**: `{clientId}.clientName`

**Description**: Display name shown in tabs and menus

**Example**:
```properties
prod.clientName=Production Server
dev.clientName=Development Environment
```

**Rules**:
- Can contain spaces and special characters
- Recommended: Keep under 20 characters for UI clarity
- Used in tab titles and menu items

---

## 3. Connection Settings

### 3.1 Server URL

**Property**: `{clientId}.url`

**Description**: Base URL of Redmine server

**Format**: `https://hostname[:port]/[path/]`

**Examples**:
```properties
# Standard HTTPS
prod.url=https://redmine.company.com/

# With port
dev.url=https://redmine.company.com:8443/

# With path
staging.url=https://company.com/redmine/
```

**Important**:
- Must end with `/`
- Use HTTPS for security (HTTP allowed but not recommended)
- Include port if non-standard (not 80/443)
- Include path if Redmine is not at root

### 3.2 Authentication

#### Option A: API Key (Recommended)

**Property**: `{clientId}.apiKey`

**Description**: Personal API access key from Redmine

**How to obtain**:
1. Log in to Redmine web interface
2. Go to "My account"
3. Find "API access key" in right sidebar
4. Click "Show" to reveal key
5. Copy and paste into configuration

**Example**:
```properties
prod.apiKey=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0
```

**Security**:
- Keep this key secret
- Do not share or commit to version control
- Regenerate if compromised

#### Option B: Username/Password

**Properties**: 
- `{clientId}.username`
- `{clientId}.password`

**Example**:
```properties
dev.username=john.doe
dev.password=MySecurePassword123
```

**Note**: API key is preferred for security reasons

### 3.3 Project ID

**Property**: `{clientId}.projectId`

**Description**: Identifier of the Redmine project

**How to find**:
1. Open project in Redmine web interface
2. Look at URL: `https://redmine.com/projects/my-project-id`
3. The part after `/projects/` is the project ID

**Example**:
```properties
prod.projectId=customer-portal
dev.projectId=internal-tools
```

**Rules**:
- Must match exactly (case-sensitive)
- Usually lowercase with hyphens
- No spaces allowed

---

## 4. Behavior Settings

### 4.1 Task Limit

**Property**: `{clientId}.taskLimit`

**Description**: Maximum number of tasks to fetch per refresh

**Default**: `0` (uses server default, usually 100)

**Examples**:
```properties
# Fetch up to 500 tasks
prod.taskLimit=500

# Fetch up to 1000 tasks (may be slow)
dev.taskLimit=1000

# Use server default
staging.taskLimit=0
```

**Recommendations**:
- **Small projects (<100 tasks)**: 0 or 100
- **Medium projects (100-500 tasks)**: 500
- **Large projects (>500 tasks)**: 1000 (may impact performance)

**Performance Impact**:
- Higher limits = slower refresh
- Higher limits = more memory usage
- Consider using filters instead of increasing limit

### 4.2 Refresh Interval

**Property**: `{clientId}.refreshInterval`

**Description**: Minutes between automatic data refreshes

**Default**: `5`

**Examples**:
```properties
# Refresh every 5 minutes
prod.refreshInterval=5

# Refresh every 15 minutes (less network traffic)
dev.refreshInterval=15

# Disable auto-refresh
staging.refreshInterval=0
```

**Recommendations**:
- **Active projects**: 5-10 minutes
- **Low-activity projects**: 15-30 minutes
- **Read-only/archived**: 0 (manual refresh only)

### 4.3 Download Path

**Property**: `{clientId}.downloadPath`

**Description**: Directory where attachments and task exports are saved

**Format**: Absolute path to directory

**Examples**:
```properties
# Windows
prod.downloadPath=C:/Users/JohnDoe/Downloads/Redmine

# Linux/Mac
dev.downloadPath=/home/johndoe/redmine-downloads
```

**Rules**:
- Directory must exist (or be creatable)
- User must have write permissions
- Use forward slashes `/` even on Windows
- No trailing slash

### 4.4 Show Closed by Default

**Property**: `{clientId}.showClosedByDefault`

**Description**: Whether to show closed tasks on initial load

**Values**: `true` or `false`

**Default**: `false`

**Examples**:
```properties
# Show only open tasks initially
prod.showClosedByDefault=false

# Show all tasks (including closed)
dev.showClosedByDefault=true
```

**Recommendation**: `false` for active projects, `true` for archived projects

### 4.5 Include Epics

**Property**: `{clientId}.includeEpics`

**Description**: Whether to include top-level parent tasks (epics)

**Values**: `true` or `false`

**Default**: `true`

**Example**:
```properties
prod.includeEpics=true
```

---

## 5. Twin Synchronization

### 5.1 Reference Pattern

**Property**: `{clientId}.refPattern`

**Description**: Pattern used to identify and create twin task references

**Format**: Text with `{id}` placeholder

**Examples**:
```properties
# Standard format
prod.refPattern=[Ref #{id}]

# Alternative formats
dev.refPattern=(Twin: #{id})
client.refPattern=Related to task {id}
```

**How it works**:
1. When cloning task #1234 from Server A to Server B
2. Application adds reference to description: `[Ref #1234]`
3. When searching for twins, looks for this pattern in subjects/descriptions

**Best Practices**:
- Use distinctive pattern unlikely to appear naturally
- Include `{id}` placeholder exactly once
- Keep pattern short and readable
- Coordinate pattern with peer servers

### 5.2 Folder Pattern

**Property**: `{clientId}.folderPattern`

**Description**: Pattern for naming folders when downloading tasks

**Variables**:
- `{id}`: Task ID
- `{subject}`: Task subject
- `{tracker}`: Task tracker/type
- `{priority}`: Task priority

**Examples**:
```properties
# ID and subject
prod.folderPattern={id}_{subject}
# Result: "1234_Fix login bug"

# ID, tracker, and subject
dev.folderPattern={id}_{tracker}_{subject}
# Result: "1234_Bug_Fix login bug"

# Just ID
simple.folderPattern={id}
# Result: "1234"
```

**Recommendations**:
- Always include `{id}` for uniqueness
- `{subject}` makes folders more readable
- Avoid special characters in pattern (they'll be sanitized)

---

## 6. Notification Settings

### 6.1 Notification Types

Currently, notifications are automatically triggered for:
- New tasks assigned to you (created in last hour)
- Tasks you created

**Future enhancements** (not yet implemented):
- Status changes
- New comments
- Due date reminders
- Overdue alerts

### 6.2 Notification Persistence

**Files**:
- `.redmine_notifications.dat`: Notification history (max 100)
- `.redmine_notified_tasks.dat`: Tracking to prevent duplicates (max 1000)

**Location**: Same directory as JAR file

**Cleanup**: Automatic (old entries removed when limits exceeded)

---

## 7. Advanced Settings

### 7.1 Attachment Format

**Property**: `{clientId}.attachmentFormat`

**Description**: Markup format for wiki and descriptions

**Values**: `textile` or `markdown`

**Default**: `textile`

**Example**:
```properties
prod.attachmentFormat=textile
dev.attachmentFormat=markdown
```

**When to change**:
- Match your Redmine server configuration
- Check in Redmine: Administration → Settings → Text formatting

### 7.2 Column Configuration

**Properties**:
- `{clientId}.columnWidths`: Saved column widths
- `{clientId}.columnVisibility`: Which columns are visible

**Format**: Auto-generated by application

**Example**:
```properties
prod.columnWidths=0:50,1:300,2:100,3:80,...
prod.columnVisibility=0,1,2,3,4,5,6,7,8,9,10
```

**Note**: These are managed by the application. Manual editing not recommended.

### 7.3 Status Colors

**Properties**: `{clientId}.statusColor.{statusName}`

**Description**: Custom colors for each status

**Format**: Hex color code

**Examples**:
```properties
prod.statusColor.New=#FFA500
prod.statusColor.In Progress=#0000FF
prod.statusColor.Resolved=#00FF00
prod.statusColor.Closed=#808080
```

**Configuration UI**: Client menu → Status Colors

### 7.4 Language

**Property**: `app.language`

**Description**: Application language

**Values**: `es` (Spanish) or `en` (English)

**Default**: System locale

**Example**:
```properties
app.language=es
```

**Note**: Requires application restart to take effect

---

## 8. Troubleshooting

### 8.1 Configuration Not Loading

**Symptoms**:
- Application starts with no clients
- Default settings used

**Causes**:
- `redmine_config.properties` not in same directory as JAR
- File encoding not UTF-8
- Syntax errors in properties file

**Solutions**:
1. Verify file location: `ls -la redmine_config.properties`
2. Check file encoding: Should be UTF-8
3. Validate syntax: No spaces around `=`, no duplicate keys
4. Check logs for error messages

### 8.2 Connection Failures

**Symptoms**:
- "Connection failed" errors
- Immediate offline mode

**Causes**:
- Incorrect URL
- Invalid API key
- Network/firewall issues
- SSL certificate problems

**Solutions**:
1. Test URL in browser: Should show Redmine login page
2. Verify API key: Copy-paste carefully, no extra spaces
3. Check network: Ping server, check firewall
4. SSL issues: Try HTTP temporarily (not for production)

### 8.3 Missing Tasks

**Symptoms**:
- Expected tasks not showing
- Task count lower than expected

**Causes**:
- Active filters
- Task limit too low
- Insufficient permissions in Redmine

**Solutions**:
1. Clear all filters: Click "Limpiar Filtros" button
2. Increase task limit: Set higher value in configuration
3. Check Redmine permissions: Ensure user can view tasks
4. Verify project ID: Must match exactly

### 8.4 Slow Performance

**Symptoms**:
- Long refresh times
- UI freezing
- High memory usage

**Causes**:
- Too many tasks
- Too many clients
- Low refresh interval
- Large cache

**Solutions**:
1. Reduce task limit: Lower `taskLimit` value
2. Increase refresh interval: Higher `refreshInterval` value
3. Close unused clients: Remove from `clients.list`
4. Clear cache: Restart application

### 8.5 Notification Issues

**Symptoms**:
- No desktop notifications
- Notifications not appearing

**Causes**:
- System tray not supported
- OS permissions denied
- Notification center closed

**Solutions**:
1. Check system tray support: Should see tray icon
2. Grant permissions: OS settings → Notifications
3. Open notification center: Click 🔔 button

---

## Appendix A: Complete Configuration Example

```properties
# ============================================
# Redmine Connector Configuration
# ============================================

# Global Settings
app.language=es
clients.list=production,development,client-external

# ============================================
# Production Server
# ============================================
production.clientName=Production Server
production.url=https://redmine.company.com/
production.apiKey=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0
production.projectId=main-project
production.taskLimit=500
production.refreshInterval=5
production.downloadPath=C:/Redmine/Production
production.refPattern=[Ref #{id}]
production.folderPattern={id}_{subject}
production.showClosedByDefault=false
production.includeEpics=true
production.attachmentFormat=textile

# Status Colors
production.statusColor.New=#FFA500
production.statusColor.In Progress=#0000FF
production.statusColor.Resolved=#00FF00
production.statusColor.Feedback=#FFFF00
production.statusColor.Closed=#808080
production.statusColor.Rejected=#FF0000

# ============================================
# Development Server
# ============================================
development.clientName=Dev Environment
development.url=https://dev-redmine.company.com/
development.apiKey=z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4j3i2h1g0
development.projectId=dev-project
development.taskLimit=200
development.refreshInterval=10
development.downloadPath=C:/Redmine/Development
development.refPattern=[Ref #{id}]
development.folderPattern={id}_{tracker}_{subject}
development.showClosedByDefault=false
development.includeEpics=true
development.attachmentFormat=markdown

# ============================================
# External Client Server
# ============================================
client-external.clientName=Client Portal
client-external.url=https://client.external.com/redmine/
client-external.apiKey=p0o9i8u7y6t5r4e3w2q1a0s9d8f7g6h5j4k3l2m1
client-external.projectId=client-project
client-external.taskLimit=300
client-external.refreshInterval=15
client-external.downloadPath=C:/Redmine/Client
client-external.refPattern=(Twin: #{id})
client-external.folderPattern={id}_{subject}
client-external.showClosedByDefault=true
client-external.includeEpics=false
client-external.attachmentFormat=textile
```

---

## Appendix B: Configuration Validation Checklist

Before starting the application, verify:

- [ ] `redmine_config.properties` exists in same directory as JAR
- [ ] File encoding is UTF-8
- [ ] `clients.list` property is defined
- [ ] Each client has all required properties:
  - [ ] `clientName`
  - [ ] `url` (ends with `/`)
  - [ ] `apiKey` or (`username` + `password`)
  - [ ] `projectId`
- [ ] URLs are accessible (test in browser)
- [ ] API keys are valid (no extra spaces)
- [ ] Download paths exist and are writable
- [ ] No duplicate client IDs
- [ ] No syntax errors (spaces around `=`, etc.)

---

**© 2025 Redmine Connector - Configuration Guide**
