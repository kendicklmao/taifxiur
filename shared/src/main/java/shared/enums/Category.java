package shared.enums;

import java.util.List;

public enum Category {
    ELECTRONICS(List.of(
            new FieldDefinition("Brand", "brandField", FieldDefinition.FieldType.TEXT),
            new FieldDefinition("Status", "statusField", FieldDefinition.FieldType.STATUS_CHOICE_BOX)
    )),
    FASHIONS(List.of(
            new FieldDefinition("Brand", "brandField", FieldDefinition.FieldType.TEXT),
            new FieldDefinition("Status", "statusField", FieldDefinition.FieldType.STATUS_CHOICE_BOX)
    )),
    VEHICLES(List.of(
            new FieldDefinition("Brand", "brandField", FieldDefinition.FieldType.TEXT),
            new FieldDefinition("Model Year", "modelField", FieldDefinition.FieldType.TEXT),
            new FieldDefinition("KM Traveled", "kmField", FieldDefinition.FieldType.TEXT)
    )),
    ARTS(List.of(
            new FieldDefinition("Artist", "artistField", FieldDefinition.FieldType.TEXT),
            new FieldDefinition("Year", "yearField", FieldDefinition.FieldType.TEXT),
            new FieldDefinition("Original", "originalBox", FieldDefinition.FieldType.CHECKBOX)
    )),
    COLLECTIBLES(List.of(
            new FieldDefinition("Year", "yearField", FieldDefinition.FieldType.TEXT)
    ));

    private final List<FieldDefinition> fields;

    Category(List<FieldDefinition> fields) {
        this.fields = fields;
    }

    public List<FieldDefinition> getFields() {
        return fields;
    }
}