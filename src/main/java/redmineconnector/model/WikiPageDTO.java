package redmineconnector.model;

import java.io.Serializable;

public class WikiPageDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    public String title;
    public String text;
    public String version;
    public String updatedOn;
    public String author;

    public java.util.List<Attachment> attachments = new java.util.ArrayList<>();

    public WikiPageDTO(String title, String text, String version, String updatedOn, String author) {
        this.title = title;
        this.text = text;
        this.version = version;
        this.updatedOn = updatedOn;
        this.author = author;
    }

    public WikiPageDTO(String title) {
        this(title, "", "", "", "");
    }

    @Override
    public String toString() {
        return title;
    }
}
