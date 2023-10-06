package dev.morphia.query;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.Document;

/**
 * Defines $meta expression object
 */
public class Meta {

    private static final String META = "$meta";
    private final String metaDataKeyword;
    private final String field;

    /**
     * Specify the meta values to use
     *
     * @param metaDataKeyword metadata keyword to create
     * @param fieldName       the field to store the value in
     * @since 2.2
     */
    private Meta(String metaDataKeyword, String fieldName) {
        this.metaDataKeyword = metaDataKeyword;
        this.field = fieldName;
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'indexKey' Meta
     * @aggregation.expression $meta
     * @query.filter $meta
     * @since 2.2
     */
    public static Meta indexKey(String field) {
        return new Meta("indexKey", field);
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'searchHighlights' Meta
     * @aggregation.expression $meta
     * @query.filter $meta
     * @since 2.2
     */
    public static Meta searchHighlights(String field) {
        return new Meta("searchHighlights", field);
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'searchScore' Meta
     * @aggregation.expression $meta
     * @query.filter $meta
     * @since 2.2
     */
    public static Meta searchScore(String field) {
        return new Meta("searchScore", field);
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'textScore' Meta
     * @aggregation.expression $meta
     * @query.filter $meta
     * @since 2.2
     */
    public static Meta textScore(String field) {
        return new Meta("textScore", field);
    }

    /**
     * @return metadata object field name
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public String field() {
        return field;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    Document toDatabase() {
        return new Document(field, new Document(META, metaDataKeyword));
    }
}
