package de.twonirwana.infinity.model.specops;


import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

public class SpecopsNestedItemDeserializer extends StdDeserializer<SpecopsNestedItem> {
    public SpecopsNestedItemDeserializer() {
        this(SpecopsNestedItem.class);
    }

    public SpecopsNestedItemDeserializer(Class<?> vc) {
        super(vc);
    }


    @Override
    public SpecopsNestedItem deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JsonNode node = jsonParser.readValueAsTree();
        final int id;

        if (node.has("id")) {
            id = (Integer) (node.get("id")).numberValue();
        } else {
            id = (Integer) (node.numberValue());
        }
        return new SpecopsNestedItem(id);
    }
    // for some reason, nested items are of the form {"id":XX} rather than just XX.. usually.

}
