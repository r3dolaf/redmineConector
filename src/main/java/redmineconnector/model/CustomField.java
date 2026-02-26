package redmineconnector.model;

import java.io.Serializable;

public class CustomField implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String name;
    public String value;
    // Some custom fields can have multiple values, currently storing as string
    // representation
    // or we can add support for List<String> values if needed.
    // For now, keeping it simple as Redmine often sends "value": "xxx" or "value":
    // ["a", "b"]

    public CustomField(int id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }
}
