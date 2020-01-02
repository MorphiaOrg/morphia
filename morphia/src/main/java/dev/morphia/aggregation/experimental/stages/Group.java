package dev.morphia.aggregation.experimental.stages;

import dev.morphia.aggregation.experimental.expressions.Expression;

import java.util.ArrayList;
import java.util.List;

public class Group extends Stage {
    private GroupId id;
    protected final List<PipelineField> fields = new ArrayList<>();

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
        fields.add(new PipelineField(name, expression));
        return this;
    }

    public List<PipelineField> getFields() {
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
        private final List<PipelineField> fields = new ArrayList<>();

        public GroupId() {
        }

        private GroupId(final Expression value) {
            fields.add(new PipelineField("_id", value));
        }

        public GroupId fields(final String name, final Expression expression) {
            fields.add(new PipelineField(name, expression));
            return this;
        }

        public List<PipelineField> getFields() {
            return fields;
        }
    }
}
