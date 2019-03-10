package dev.morphia.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Defines $meta expression object
 * @mongodb.driver.manual reference/operator/aggregation/meta/ $meta
 */
public class Meta {

    private static final String META = "$meta";

    /**
     * Representing specified metadata keyword
     * @mongodb.driver.manual reference/operator/aggregation/meta/#exp._S_meta $meta
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

    private MetaDataKeyword metaDataKeyword;
    private String field;

    /**
     * Specify the meta
     * @param metaDataKeyword - metadata keyword to create
     */
    public Meta(final MetaDataKeyword metaDataKeyword) {
        this.metaDataKeyword = metaDataKeyword;
        this.field = metaDataKeyword.getName();
    }

    /**
     * @param metaDataKeyword  - metadata keyword to create
     * @param field - metadata object field name
     */
    public Meta(final MetaDataKeyword metaDataKeyword, final String field) {
        this.metaDataKeyword = metaDataKeyword;
        this.field = field;
    }

    /**
     * @return metadata object field name
     */
    public String getField() {
        return field;
    }

    /**
     * factory method for textScore metaDataKeyword
     * @return instance of 'textScore' Meta
     */
    public static Meta textScore() {
        return new Meta(MetaDataKeyword.textScore);
    }

    /**
     *
     * @param field - the field to project meta data
     * @return  instance of 'textScore' Meta
     */
    public static Meta textScore(final String field) {
        return new Meta(MetaDataKeyword.textScore, field);
    }

    DBObject toDatabase() {
        BasicDBObject metaObject = new BasicDBObject(META, metaDataKeyword.getName());
        return new BasicDBObject(field, metaObject);
    }
}
