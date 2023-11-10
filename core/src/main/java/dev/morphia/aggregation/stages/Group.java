package dev.morphia.aggregation.stages;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.AggregationException;
import dev.morphia.aggregation.expressions.Expressions;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.Fields;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.sofia.Sofia;

/**
 * Groups input documents by the specified _id expression and for each distinct grouping, outputs a document.
 *
 * @aggregation.stage $group
 */
public class Group extends Stage {
    private final GroupId id;
    private Fields fields;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Group() {
        super("$group");
        id = null;
    }

    /**
     * @param id the group ID`
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Group(GroupId id) {
        super("$group");
        this.id = id;
    }

    /**
     * Creates a group stage with an ID definition
     *
     * @param id the group ID
     * @return the new stage
     * @since 2.2
     */
    public static Group group(GroupId id) {
        return new Group(id);
    }

    /**
     * Creates a group stage with no ID definition
     *
     * @return the new stage
     * @since 2.2
     */
    public static Group group() {
        return new Group();
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
     * Creates a named group ID
     *
     * @param name the id name
     * @return the new groupID
     */
    public static GroupId id(String name) {
        return new GroupId(Expressions.field(name));
    }

    /**
     * Creates a named group ID
     *
     * @param name the id name
     * @return the new groupID
     */
    public static GroupId id(Expression name) {
        return new GroupId(name);
    }

    /**
     * Adds a field to the group. This method is equivalent to calling {@code field("name", Expression.field("name"))}
     *
     * @param name the field name
     * @return this
     * @see #field(String, Expression)
     * @see Expressions#field(String)
     */
    public Group field(String name) {
        return field(name, Expressions.field(name));
    }

    /**
     * Adds a named field to the group with an expression giving the value.
     *
     * @param name       the name of the field
     * @param expression the expression giving the value
     * @return this
     */
    public Group field(String name, Expression expression) {
        if (fields == null) {
            fields = Fields.on(this);
        }
        fields.add(name, expression);
        return this;
    }

    /**
     * @return the fields
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Fields getFields() {
        return fields;
    }

    /**
     * @return the ID
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public GroupId getId() {
        return id;
    }

    /**
     * Defines a group ID
     */
    public static class GroupId {
        private Expression field;
        private DocumentExpression document;

        /**
         * @hidden
         * @morphia.internal
         */
        @MorphiaInternal
        protected GroupId() {
            document = Expressions.document();
        }

        /**
         * @param value the group ID expression
         * @hidden
         * @morphia.internal
         */
        @MorphiaInternal
        protected GroupId(Expression value) {
            if (value instanceof DocumentExpression) {
                document = (DocumentExpression) value;
            } else {
                field = value;
            }
        }

        /**
         * Adds a field to the group. This method is equivalent to calling {@code field("name", Expression.field("name"))}
         *
         * @param name the field name
         * @return this
         * @see #field(String, Expression)
         * @see Expressions#field(String)
         */
        public GroupId field(String name) {
            return field(name, Expressions.field(name));
        }

        /**
         * Adds a named field to the group with an expression giving the value.
         *
         * @param name       the name of the field
         * @param expression the expression giving the value
         * @return this
         */
        public GroupId field(String name, Expression expression) {
            if (field != null) {
                throw new AggregationException(Sofia.mixedModesNotAllowed("_id"));
            }
            document.field(name, expression);

            return this;
        }

        /**
         * @return the document
         * @hidden
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public DocumentExpression getDocument() {
            return document;
        }

        /**
         * @return the field
         * @hidden
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public Expression getField() {
            return field;
        }
    }
}
