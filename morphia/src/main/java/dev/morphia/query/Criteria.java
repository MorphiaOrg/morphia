package dev.morphia.query;


import com.mongodb.DBObject;


/**
 * Internal class for building up query documents.
 *
 * @morphia.internal
 */
public interface Criteria {
    /**
     * @return the DBObject form of this type
     */
    DBObject toDBObject();

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
