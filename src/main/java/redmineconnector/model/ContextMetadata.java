package redmineconnector.model;

import java.util.List;

public class ContextMetadata {
    public List<SimpleEntity> allowedStatuses;
    public List<Integer> availableCustomFieldIds;
    public List<CustomFieldDefinition> definitions; // Added field

    public ContextMetadata(List<SimpleEntity> allowedStatuses, List<Integer> availableCustomFieldIds,
            List<CustomFieldDefinition> definitions) {
        this.allowedStatuses = allowedStatuses;
        this.availableCustomFieldIds = availableCustomFieldIds;
        this.definitions = definitions;
    }
}
