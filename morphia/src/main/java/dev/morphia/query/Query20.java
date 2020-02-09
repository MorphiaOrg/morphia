package dev.morphia.query;


import org.bson.Document;

import static dev.morphia.query.MorphiaQuery.legacyOperation;


/**
 * @param <T> The java type to query against
 */
public interface Query20<T> extends Query<T> {
    @Override
    default CriteriaContainer and(final Criteria... criteria) {
        return legacyOperation();
    }

    @Override
    default FieldEnd<? extends CriteriaContainer> criteria(final String field) {
        return legacyOperation();
    }

    @Override
    default FieldEnd<? extends Query<T>> field(final String name) {
        return legacyOperation();
    }

    @Override
    default Query<T> filter(final String condition, final Object value) {
        return legacyOperation();
    }

    @Override
    default CriteriaContainer or(final Criteria... criteria) {
        return legacyOperation();
    }

    @Override
    default Query<T> order(final Meta sort) {
        return legacyOperation();
    }

    @Override
    default Query<T> order(final Sort... sorts) {
        return legacyOperation();
    }

    @Override
    default Query<T> project(final String field, final boolean include) {
        return legacyOperation();
    }

    @Override
    default Query<T> project(final String field, final ArraySlice slice) {
        return legacyOperation();
    }

    @Override
    default Query<T> project(final Meta meta) {
        return legacyOperation();
    }

    @Override
    default Query<T> retrieveKnownFields() {
        return legacyOperation();
    }

    @Override
    default Modify<T> modify(final UpdateOperations<T> operations) {
        return legacyOperation();
    }

    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    default Update<T> update(final UpdateOperations operations) {
        return legacyOperation();
    }

    default String getFieldName() {
        return legacyOperation();
    }

    /**
     * @return the Mongo fields {@link Document}.
     * @morphia.internal
     */
    default Document getFieldsObject() {
        return legacyOperation();
    }

    /**
     * @return the Mongo sort {@link Document}.
     * @morphia.internal
     */
    default Document getSort() {
        return legacyOperation();
    }

    //    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    default Update<T> update(final Document document) {
        return legacyOperation();
    }
}