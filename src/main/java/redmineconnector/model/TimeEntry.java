package redmineconnector.model;

import java.io.Serializable;

public class TimeEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    public int id;
    public int issueId;
    public String issueSubject;
    public String user;
    public double hours;
    public String spentOn;
    public String comment;

    public TimeEntry(int id, int issueId, String issueSubject, String user, double hours, String spentOn,
            String comment) {
        this.id = id;
        this.issueId = issueId;
        this.issueSubject = issueSubject;
        this.user = user;
        this.hours = hours;
        this.spentOn = spentOn;
        this.comment = comment;
    }
}
