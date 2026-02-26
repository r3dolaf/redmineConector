package redmineconnector.model;

import java.io.Serializable;

public class UploadToken implements Serializable {
    private static final long serialVersionUID = 1L;
    public String token;
    public String filename;
    public String contentType;

    public UploadToken(String t, String f, String c) {
        this.token = t;
        this.filename = f;
        this.contentType = c;
    }

    @Override
    public String toString() {
        return "📤 " + filename + " (Pendiente)";
    }
}
