package redmineconnector.model;

import java.io.Serializable;

public class Journal implements Serializable {
    private static final long serialVersionUID = 1L;
    public String user;
    public String notes;
    public String createdOn;

    public Journal(String u, String n, String d) {
        this.user = u;
        this.notes = n;
        this.createdOn = d;
    }
}
