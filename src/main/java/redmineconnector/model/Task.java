package redmineconnector.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Task implements Serializable {
    private static final long serialVersionUID = 2L; // Updated for new field
    public int id;
    public boolean isFullDetails = false;
    public String subject = "";
    public String description = "";
    public String status = "";
    public String priority = "";
    public String tracker = "";
    public String assignedTo = "";
    public String category = "";
    public String author = "";
    public String authorEmail = "";
    public String targetVersion = "";
    public String comment = "";
    public int priorityId, trackerId, assignedToId, statusId, categoryId, targetVersionId, authorId, projectId;
    public int parentId = 0;
    public String parentName = "";
    public int doneRatio;
    public Date createdOn;
    public String webUrl;
    public double spentHours;
    public List<Attachment> attachments = new ArrayList<>();
    public List<Journal> journals = new ArrayList<>();
    public List<Changeset> changesets = new ArrayList<>();
    public List<UploadToken> pendingUploads = new ArrayList<>();
    public List<CustomField> customFields = new ArrayList<>();

    public Task() {
        createdOn = new Date();
    }

    public Task(Task other) {
        this.id = 0;
        this.subject = other.subject;
        this.projectId = other.projectId;
        this.description = other.description;
        this.status = "Nueva";
        this.statusId = 0;
        this.priority = other.priority;
        this.priorityId = other.priorityId;
        this.tracker = other.tracker;
        this.trackerId = other.trackerId;
        this.assignedTo = other.assignedTo;
        this.assignedToId = other.assignedToId;
        this.category = other.category;
        this.categoryId = other.categoryId;
        this.targetVersion = other.targetVersion;
        this.targetVersionId = other.targetVersionId;
        this.author = other.author;
        this.authorEmail = other.authorEmail;
        this.parentId = other.parentId;
        this.parentName = other.parentName;
        this.createdOn = new Date();
        this.spentHours = 0.0;
        this.doneRatio = 0;
        this.doneRatio = 0;
        this.attachments = new ArrayList<>(other.attachments);
        this.customFields = new ArrayList<>();
        if (other.customFields != null) {
            for (CustomField cf : other.customFields) {
                this.customFields.add(new CustomField(cf.id, cf.name, cf.value));
            }
        }
    }

    @Override
    public String toString() {
        return subject;
    }
}
