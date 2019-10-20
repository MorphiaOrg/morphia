package dev.morphia.query;


import org.bson.Document;

/**
 * Creates a Criteria for a $where clause.
 */
public class WhereCriteria extends AbstractCriteria {

    private final Object js;

    /**
     * Creates a WhereCriteria with the given javascript
     *
     * @param js the javascript
     */
    public WhereCriteria(final String js) {
        this.js = js;
    }

    @Override
    public Document toDocument() {
        return new Document(FilterOperator.WHERE.val(), js);
    }

    @Override
    public String getFieldName() {
        return FilterOperator.WHERE.val();
    }

}
