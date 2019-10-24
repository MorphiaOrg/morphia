package dev.morphia.query;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.CodeWScope;

/**
 * Creates a Criteria for a $where clause.
 * @deprecated no replacement is planned
 */
@Deprecated
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

    /**
     * Creates a WhereCriteria with the given javascript
     *
     * @param js the javascript
     */
    public WhereCriteria(final CodeWScope js) {
        this.js = js;
    }

    @Override
    public DBObject toDBObject() {
        return new BasicDBObject(FilterOperator.WHERE.val(), js);
    }

    @Override
    public String getFieldName() {
        return FilterOperator.WHERE.val();
    }

}
