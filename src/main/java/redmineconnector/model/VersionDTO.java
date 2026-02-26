package redmineconnector.model;

import java.io.Serializable;

public class VersionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    public int id;
    public String name;
    public String status;
    public String startDate;
    public String dueDate;

    public VersionDTO(int id, String name, String status, String startDate, String dueDate) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.startDate = startDate;
        this.dueDate = dueDate;
    }

    @Override
    public String toString() {
        return name + " (" + status + ")";
    }
}
