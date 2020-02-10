package dev.morphia.query;


import org.bson.Document;

import static dev.morphia.query.MorphiaQuery.legacyOperation;


/**
 * Internal class for building up query documents.
 *
 * @morphia.internal
 */
@Deprecated(since = "2.0", forRemoval = true)
public interface Criteria {
    /**
     * Used to add this Criteria to a CriteriaContainer
     *
     * @param container the container to add to
     */
    default void attach(CriteriaContainer container) {
        legacyOperation();
    }

    /**
     * @return the field name for the criteria
     */
    default String getFieldName() {
        return legacyOperation();
    }

    /**
     * @return the Document form of this type
     */
    default Document toDocument() {
        return legacyOperation();
    }
}
