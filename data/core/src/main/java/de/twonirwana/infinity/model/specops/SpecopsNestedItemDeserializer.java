package de.twonirwana.infinity.model.specops;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class SpecopsNestedItemDeserializer extends StdDeserializer<SpecopsNestedItem> {
    public SpecopsNestedItemDeserializer() {
        this(null);
    }

    public SpecopsNestedItemDeserializer(Class<?> vc) {
        super(vc);
    }


    @Override
    public SpecopsNestedItem deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int id = 0;
        try {

            if (node.has("id")) {
                id = (Integer) (node.get("id")).numberValue();
            } else {
                id = (Integer) (node.numberValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new SpecopsNestedItem(id);
    }
    // for some reason, nested items are of the form {"id":XX} rather than just XX.. usually.

}
