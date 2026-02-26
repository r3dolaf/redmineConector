package redmineconnector.model;

import java.io.Serializable;

public class Attachment implements Serializable {
    private static final long serialVersionUID = 1L;
    public int id;
    public String filename;
    public String contentUrl;
    public String contentType;
    public long filesize;

    public Attachment(int id, String name, String url, String type, long size) {
        this.id = id;
        this.filename = name;
        this.contentUrl = url;
        this.contentType = type;
        this.filesize = size;
    }

    @Override
    public String toString() {
        return "📎 " + filename + " (" + (filesize / 1024) + " KB)";
    }
}
