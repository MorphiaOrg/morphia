package dev.morphia.query;

import org.bson.Document;

/**
 * Defines $meta expression object
 *
 * @aggregation.expression $meta
 */
public class Meta {

    private static final String META = "$meta";
    private final MetaDataKeyword metaDataKeyword;
    private final String field;

    /**
     * Specify the meta
     *
     * @param metaDataKeyword - metadata keyword to create
     */
    public Meta(MetaDataKeyword metaDataKeyword) {
        this.metaDataKeyword = metaDataKeyword;
        this.field = metaDataKeyword.getName();
    }

    /**
     * @param metaDataKeyword - metadata keyword to create
     * @param field           - metadata object field name
     */
    public Meta(MetaDataKeyword metaDataKeyword, String field) {
        this.metaDataKeyword = metaDataKeyword;
        this.field = field;
    }

    /**
     * factory method for textScore metaDataKeyword
     *
     * @return instance of 'textScore' Meta
     */
    public static Meta textScore() {
        return new Meta(MetaDataKeyword.textScore);
    }

    /**
     * @param field - the field to project meta data
     * @return instance of 'textScore' Meta
     */
    public static Meta textScore(String field) {
        return new Meta(MetaDataKeyword.textScore, field);
    }

    /**
     * @return metadata object field name
     */
    public String getField() {
        return field;
    }

    Document toDatabase() {
        return new Document(field, new Document(META, metaDataKeyword.getName()));
    }

    /**
     * Representing specified metadata keyword
     *
     * @aggregation.expression $meta
     */
    public enum MetaDataKeyword {
        textScore;

        /**
         * @return MetaDataKeyword name
         */
        public String getName() {
            return textScore.name();
        }
    }
}
