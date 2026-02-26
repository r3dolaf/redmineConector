package redmineconnector.model;

import java.io.Serializable;

public class SimpleEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    public int id;
    public String name;
    public boolean isClosed = false;

    public SimpleEntity() {
    }

    public SimpleEntity(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public SimpleEntity(int id, String name, boolean isClosed) {
        this.id = id;
        this.name = name;
        this.isClosed = isClosed;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SimpleEntity && ((SimpleEntity) o).id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
