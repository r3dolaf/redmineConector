package redmineconnector.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomFieldDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String name;
    public String type; // e.g., string, list, date, bool
    public boolean isRequired;
    public boolean isFilter;
    public List<String> possibleValues = new ArrayList<>();
    public List<Integer> trackerIds = new ArrayList<>(); // IDs of trackers this field applies to. Empty = Global/All?
    public List<Integer> projectIds = new ArrayList<>(); // IDs of projects this field applies to. Empty = All (if "For
                                                         // all projects" checked)

    public CustomFieldDefinition(int id, String name, String type, boolean isRequired) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isRequired = isRequired;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
