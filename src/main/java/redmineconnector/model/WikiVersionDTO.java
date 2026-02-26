package redmineconnector.model;

import java.io.Serializable;

public class WikiVersionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    public int version;
    public String updatedOn;
    public String author;
    public String comments;

    public WikiVersionDTO(int version, String updatedOn, String author, String comments) {
        this.version = version;
        this.updatedOn = updatedOn;
        this.author = author;
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "v" + version + " - " + updatedOn + " por " + author;
    }
}
