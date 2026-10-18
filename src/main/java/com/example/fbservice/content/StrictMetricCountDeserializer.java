package com.example.fbservice.content;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/**
 * Analytics are unverified user data, but must preserve exactly entered integer values.
 * Jackson defaults can silently coerce 1.9 to 1 or "100" to 100.
 */
public final class StrictMetricCountDeserializer extends StdDeserializer<Long> {
    private static final long MAX=1_000_000_000L;

    public StrictMetricCountDeserializer() {
        super(Long.class);
    }

    @Override
    public Long deserialize(JsonParser parser,DeserializationContext context) throws IOException {
        if(parser.currentToken()!=JsonToken.VALUE_NUMBER_INT)
            throw JsonMappingException.from(parser,"Metric counts must be JSON integers");
        long value=parser.getLongValue();
        if(value<0||value>MAX)
            throw JsonMappingException.from(parser,"Metric count is outside 0..1000000000");
        return value;
    }

    @Override
    public Long getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        throw JsonMappingException.from(ctxt.getParser(),"Metric counts cannot be null");
    }
}
