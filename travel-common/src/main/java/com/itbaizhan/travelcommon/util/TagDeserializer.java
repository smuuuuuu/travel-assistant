package com.itbaizhan.travelcommon.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TagDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == null) {
            token = p.nextToken();
        }

        if (token == null || token == JsonToken.VALUE_NULL) {
            return null;
        }

        if (token.isScalarValue()) {
            return p.getValueAsString();
        }

        JsonNode node = p.readValueAsTree();
        return normalize(node);
    }

    private String normalize(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isValueNode()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> list = new ArrayList<>();
            for (JsonNode item : node) {
                String v = normalize(item);
                if (v != null && !v.isBlank()) {
                    list.add(v);
                }
            }
            return String.join(",", list);
        }
        if (node.isObject()) {
            if (node.size() == 1) {
                JsonNode only = node.elements().next();
                return normalize(only);
            }
            return node.toString();
        }
        return node.asText();
    }
}
