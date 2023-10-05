package dev.morphia.query.filters;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.Document;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class JsonSchemaFilter extends Filter {
    private final Document schema;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public JsonSchemaFilter(Document schema) {
        super("$jsonSchema", null, schema);
        this.schema = schema;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Document schema() {
        return schema;
    }
}
