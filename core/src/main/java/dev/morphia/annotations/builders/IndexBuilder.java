package dev.morphia.annotations.builders;

import dev.morphia.annotations.Field;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;

import java.util.List;

/**
 * @morphia.internal
 * @since 2.0
 */
public class IndexBuilder extends AnnotationBuilder<Index> implements Index {
    public IndexBuilder() {
    }

    IndexBuilder(Index original) {
        super(original);
    }

    @Override
    public Class<Index> annotationType() {
        return Index.class;
    }

    @Override
    public Field[] fields() {
        return get("fields");
    }

    @Override
    public IndexOptions options() {
        return get("options");
    }

    IndexBuilder fields(List<Field> fields) {
        put("fields", fields.toArray(new Field[0]));
        return this;
    }

    public IndexBuilder fields(Field... fields) {
        put("fields", fields);
        return this;
    }

    /**
     * Options to apply to the index.  Use of this field will ignore any of the deprecated options defined on {@link Index} directly.
     */
    public IndexBuilder options(IndexOptions options) {
        put("options", options);
        return this;
    }

    /**
     * Create the index in the background
     */
    IndexBuilder background(boolean background) {
        put("background", background);
        return this;
    }

    /**
     * The name of the index to create; default is to let the mongodb create a name (in the form of key1_1/-1_key2_1/-1...)
     */
    IndexBuilder name(String name) {
        put("name", name);
        return this;
    }

    /**
     * Create the index with the sparse option
     */
    IndexBuilder sparse(boolean sparse) {
        put("sparse", sparse);
        return this;
    }

    /**
     * Creates the index as a unique value index; inserting duplicates values in this field will cause errors
     */
    IndexBuilder unique(boolean unique) {
        put("unique", unique);
        return this;
    }

    /**
     * List of fields (prepended with "-" for desc; defaults to asc).
     */
    IndexBuilder value(String value) {
        put("value", value);
        return this;
    }
}
