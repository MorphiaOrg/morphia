package dev.morphia.aggregation.experimental.stages;

import java.util.ArrayList;
import java.util.List;

public abstract class Expression {
    protected final String operation;
    protected final String name;
    protected final Object value;

    protected Expression(final String operation) {
        this.operation = operation;
        this.name = null;
        this.value = null;
    }

    protected Expression(final String operation, final String name, final Object value) {
        this.operation = operation;
        this.name = name;
        this.value = value;
    }

    public Expression(final String operation, final String name) {
        this.operation = operation;
        this.name = name;
        this.value = null;
    }

    public static Expression field(final String name) {
        return new Literal(name.startsWith("$") ? name : "$" + name);
    }

    public static Expression literal(final Object value) {
        return new Literal(value);
    }

    public static PushExpression push(final String name) {
        return new PushExpression(name);
    }

    public String getOperation() {
        return operation;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public static class Literal extends Expression {
        public Literal(final Object value) {
            super(null, null, value);
        }
    }

    public static class PushExpression extends Expression {
        private List<Field> fields = new ArrayList<>();
        private String source;

        public PushExpression(final String name) {
            super("$push", name);
        }

        public PushExpression source(final String source, final String... sources) {
            this.source = source;
            return this;
        }

        public PushExpression source(final String source, final String renamed) {
            fields.add(new Field(source, renamed));
            return this;
        }

        public List<Field> getFields() {
            return fields;
        }

        public String getSource() {
            return source;
        }

        public static class Field {
            private String source;
            private String renamed;

            public Field(final String source) {
                this.source = source;
            }

            public Field(final String source, final String renamed) {
                this.source = source;
                this.renamed = renamed;
            }

            public String getSource() {
                return source;
            }

            public String getRenamed() {
                return renamed;
            }
        }
    }
}
