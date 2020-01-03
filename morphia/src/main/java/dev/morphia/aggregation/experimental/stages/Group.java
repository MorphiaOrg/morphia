package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.Fields;
import dev.morphia.aggregation.experimental.expressions.PipelineField;

public class Group extends Stage {
    private GroupId id;
    protected Fields<Group> fields;

    protected Group() {
        super("$group");
        id = null;
    }

    protected Group(final GroupId id) {
        super("$group");
        this.id = id;
    }

    public GroupId getId() {
        return id;
    }

    public Group fields(final String name, final Expression expression) {
        if(fields == null) {
            fields = Expression.fields(this);
        }
        fields.add(name, expression);
        return this;
    }

    public Fields<Group> getFields() {
        return fields;
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

    public static class GroupId {
        private Fields<GroupId> fields;

        protected GroupId() {
        }

        protected GroupId(final Expression value) {
            field("_id", value);
        }

        public GroupId field(final String name, final Expression expression) {
            if(fields == null) {
                fields = Expression.fields(this);
            }
            return fields.add(name, expression);
        }

        public Fields<GroupId> getFields() {
            return fields;
        }
    }
}
