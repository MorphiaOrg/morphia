package dev.morphia.query;


import org.bson.Document;


/**
 * Internal class for building up query documents.
 *
 * @morphia.internal
 */
public interface Criteria {
    /**
     * @return the Document form of this type
     */
    Document toDocument();

    /**
     * Used to add this Criteria to a CriteriaContainer
     *
     * @param container the container to add to
     */
    void attach(CriteriaContainer container);

    /**
     * @return the field name for the criteria
     */
    String getFieldName();
}
