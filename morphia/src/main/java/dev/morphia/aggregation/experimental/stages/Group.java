package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.AggregationException;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.internal.DocumentExpression;
import dev.morphia.aggregation.experimental.expressions.internal.Fields;
import dev.morphia.sofia.Sofia;

/**
 * Groups input documents by the specified _id expression and for each distinct grouping, outputs a document.
 *
 * @mongodb.driver.manual reference/operator/aggregation/group/ $group
 */
public class Group extends Stage {
    private Fields<Group> fields;
    private GroupId id;

    protected Group() {
        super("$group");
        id = null;
    }

    protected Group(final GroupId id) {
        super("$group");
        this.id = id;
    }

    /**
     * Creates a group stage with no ID definition
     *
     * @return the new stage
     */
    public static Group of() {
        return new Group();
    }

    /**
     * Creates a group stage with an ID definition
     *
     * @param id the group ID
     * @return the new stage
     */
    public static Group of(final GroupId id) {
        return new Group(id);
    }

    /**
     * Creates a named group ID
     *
     * @param name the id name
     * @return the new groupID
     */
    public static GroupId id(final String name) {
        return new GroupId(Expression.field(name));
    }

    /**
     * Creates an unnamed group ID
     *
     * @return the new groupID
     */
    public static GroupId id() {
        return new GroupId();
    }

    /**
     * Adds a field to the group.  This method is equivalent to calling {@code field("name", Expression.field("name"))}
     *
     * @param name the field name
     * @return this
     * @see #field(String, Expression)
     * @see Expression#field(String)
     */
    public Group field(final String name) {
        return field(name, Expression.field(name));
    }

    /**
     * Adds a named field to the group with an expression giving the value.
     *
     * @param name the name of the field
     * @param expression the expression giving the value
     * @return this
     */
    public Group field(final String name, final Expression expression) {
        if (fields == null) {
            fields = Fields.on(this);
        }
        fields.add(name, expression);
        return this;
    }

    /**
     * @return the fields
     * @morphia.internal
     */
    public Fields<Group> getFields() {
        return fields;
    }

    /**
     * @return the ID
     * @morphia.internal
     */
    public GroupId getId() {
        return id;
    }

    /**
     * Defines a group ID
     */
    public static class GroupId {
        private Expression field;
        private DocumentExpression document;

        protected GroupId() {
            document = Expression.of();
        }

        protected GroupId(final Expression value) {
            if (value instanceof DocumentExpression) {
                document = (DocumentExpression) value;
            } else {
                field = value;
            }
        }

        /**
         * Adds a field to the group.  This method is equivalent to calling {@code field("name", Expression.field("name"))}
         *
         * @param name the field name
         * @return this
         * @see #field(String, Expression)
         * @see Expression#field(String)
         */
        public GroupId field(final String name) {
            return field(name, Expression.field(name));
        }

        /**
         * Adds a named field to the group with an expression giving the value.
         *
         * @param name the name of the field
         * @param expression the expression giving the value
         * @return this
         */
        public GroupId field(final String name, final Expression expression) {
            if (field != null) {
                throw new AggregationException(Sofia.mixedModesNotAllowed("_id"));
            }
            document.field(name, expression);

            return this;
        }

        /**
         * @return the document
         * @morphia.internal
         */
        public DocumentExpression getDocument() {
            return document;
        }

        /**
         * @return the field
         * @morphia.internal
         */
        public Expression getField() {
            return field;
        }
    }
}
