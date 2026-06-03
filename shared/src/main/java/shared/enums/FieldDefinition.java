package shared.enums;

import java.io.Serializable;

public class FieldDefinition implements Serializable {
    public enum FieldType {
        TEXT,
        STATUS_CHOICE_BOX,
        CHECKBOX
    }

    private final String label;
    private final String id;
    private final FieldType type;

    public FieldDefinition(String label, String id, FieldType type) {
        this.label = label;
        this.id = id;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public String getId() {
        return id;
    }

    public FieldType getType() {
        return type;
    }
}
