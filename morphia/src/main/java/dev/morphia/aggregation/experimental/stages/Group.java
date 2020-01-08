package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.Expression.DocumentExpression;
import dev.morphia.aggregation.experimental.expressions.Fields;
import dev.morphia.sofia.Sofia;

public class Group extends Stage {
    protected Fields<Group> fields;
    private GroupId id;

    protected Group() {
        super("$group");
        id = null;
    }

    protected Group(final GroupId id) {
        super("$group");
        this.id = id;
    }

    public static Group of() {
        return new Group();
    }

    public static Group of(final GroupId id) {
        return new Group(id);
    }

    public static GroupId id(final String name) {
        return new GroupId(Expression.field(name));
    }

    public static GroupId id() {
        return new GroupId();
    }

    public Group field(final String name, final Expression expression) {
        if (fields == null) {
            fields = Fields.on(this);
        }
        fields.add(name, expression);
        return this;
    }

    public Fields<Group> getFields() {
        return fields;
    }

    public GroupId getId() {
        return id;
    }

    public static class GroupId {
        private Expression field;
        private DocumentExpression document;

        protected GroupId() {
            document = Expression.of();
        }

        protected GroupId(final Expression value) {
            if(value instanceof DocumentExpression) {
                document = (DocumentExpression) value;
            } else {
                field = value;
            }
        }

        public GroupId field(final String name, final Expression expression) {
            if (field != null) {
                throw new IllegalStateException(Sofia.mixedModesNotAllowed("_id"));
            }
            document.field(name, expression);

            return this;
        }

        public DocumentExpression getDocument() {
            return document;
        }

        public Expression getField() {
            return field;
        }
    }
}
