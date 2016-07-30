package org.mongodb.morphia.aggregation;

import com.mongodb.DBObject;
import org.mongodb.morphia.query.Meta;

/**
 * Meta expression ($meta) representation for aggregation pipeline.
 * Contains reference to the meta object under the hood
 * @see org.mongodb.morphia.query.Meta
 * Inner builder class
 * @see MetaAggregationBuilder provides the abbility to build different type of objects for aggregation piplile
 * @mongodb.driver.manual reference/operator/aggregation/meta/ $meta
 */
public class MetaAggregation implements GroupElement, ProjectionElement, SortElement {

    /**
     * internal reference to Meta object
     */
    private final Meta meta;

    /**
     * Static factory method and object builder are preferred
     * @param meta - reference to the Meta object
     */
    MetaAggregation(final Meta meta) {
        this.meta = meta;
    }

    /**
     * static factory method for {$meta : testScore}
     * @return MetaAggregationBuilder object
     */
    public static MetaAggregationBuilder textScore() {
        return new MetaAggregationBuilder(Meta.MetaDataKeyword.textScore);
    }

    @Override
    public DBObject toDBObject() {
        return meta.toDatabase();
    }

    /**
     * Inner static builder for MetaAggregation object
     */
    public static final class MetaAggregationBuilder {
        private final Meta.MetaDataKeyword metaDataKeyword;

        private MetaAggregationBuilder(final Meta.MetaDataKeyword metaDataKeyword) {
            this.metaDataKeyword = metaDataKeyword;
        }

        /**
         *
         * @param metaFieldName - field name
         * @return {metaFieldName: {$meta: metaDataKeyword}}
         */
        public SortElement sort(final String metaFieldName) {
            return new MetaAggregation(new Meta(metaDataKeyword, metaFieldName));
        }

        /**
         *
         * @param metaFieldName - field name
         * @return {metaFieldName: {$meta: metaDataKeyword}}
         */
        public ProjectionElement project(final String metaFieldName) {
            return new MetaAggregation(new Meta(metaDataKeyword, metaFieldName));
        }

        /**
         *
         * @param metaFieldName - field name
         * @return {metaFieldName: {$meta: metaDataKeyword}}
         */
        public GroupElement grouping(final String metaFieldName) {
            return new MetaAggregation(new Meta(metaDataKeyword, metaFieldName));
        }

        /**
         *
         * @return {$meta: metaDataKeyword}
         */
        public GroupElement group() {
            return new MetaAggregation(new Meta(metaDataKeyword, "name")) {
                @Override
                public DBObject toDBObject() {
                    return (DBObject) super.toDBObject().get("name");
                }
            };
        }

    }
}

