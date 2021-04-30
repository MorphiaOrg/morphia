package dev.morphia.query;

import org.bson.Document;

/**
 * Defines $meta expression object
 *
 * @aggregation.expression $meta
 * @query.filter $meta
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
    public Meta(String metaDataKeyword, String fieldName) {
        this.metaDataKeyword = metaDataKeyword;
        this.field = fieldName;
    }

    /**
     * Specify the meta
     *
     * @param metaDataKeyword - metadata keyword to create
     */
    @Deprecated(forRemoval = true)
    public Meta(MetaDataKeyword metaDataKeyword) {
        this(metaDataKeyword.name(), metaDataKeyword.name());
    }

    /**
     * @param metaDataKeyword - metadata keyword to create
     * @param field           - metadata object field name
     */
    @Deprecated(forRemoval = true)
    public Meta(MetaDataKeyword metaDataKeyword, String field) {
        this(metaDataKeyword.name(), field);
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'indexKey' Meta
     */
    public static Meta indexKey(String field) {
        return new Meta("indexKey", field);
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'searchHighlights' Meta
     */
    public static Meta searchHighlights(String field) {
        return new Meta("searchHighlights", field);
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'searchScore' Meta
     */
    public static Meta searchScore(String field) {
        return new Meta("searchScore", field);
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'textScore' Meta
     */
    public static Meta textScore(String field) {
        return new Meta("textScore", field);
    }

    /**
     * factory method for textScore metaDataKeyword
     *
     * @return instance of 'textScore' Meta
     */
    @Deprecated(forRemoval = true)
    public static Meta textScore() {
        return new Meta(MetaDataKeyword.textScore);
    }

    /**
     * @return metadata object field name
     */
    public String getField() {
        return field;
    }

    Document toDatabase() {
        return new Document(field, new Document(META, metaDataKeyword));
    }

    /**
     * Representing specified metadata keyword
     *
     * @aggregation.expression $meta
     */
    @Deprecated(forRemoval = true)
    public enum MetaDataKeyword {
        textScore

    }
}
