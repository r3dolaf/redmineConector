package redmineconnector.model;

import java.io.Serializable;

public class Changeset implements Serializable {
    private static final long serialVersionUID = 1L;
    public String revision;
    public String user;
    public String comments;
    public String committedOn;

    public Changeset(String r, String u, String c, String d) {
        this.revision = r;
        this.user = u;
        this.comments = c;
        this.committedOn = d;
    }
}
