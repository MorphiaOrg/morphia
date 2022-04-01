package dev.morphia.aggregation.stages;

import com.mongodb.lang.Nullable;
import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.Sort;

import java.util.List;

/**
 * Performs operations on a specified span of documents in a collection, known as a window, and returns the results based on the chosen
 * window operator.
 *
 * @mongodb.server.release 5.0
 * @since 2.3
 */
public class SetWindowFields extends Stage {
    private Expression partition;
    private Sort[] sorts;
    private Output[] outputs;

    protected SetWindowFields() {
        super("$setWindowFields");
    }

    public static SetWindowFields setWindowFields() {
        return new SetWindowFields();
    }

    public SetWindowFields output(Output... outputs) {
        this.outputs = outputs;
        return this;
    }

    /**
     * @return the outputs
     * @morphia.internal
     */
    @MorphiaInternal
    public Output[] outputs() {
        return outputs;
    }

    /**
     * @return the partition
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Expression partition() {
        return partition;
    }

    /**
     * Defines the expression to use to partition the data.
     *
     * @param partition the expression
     * @return this
     */
    public SetWindowFields partitionBy(Expression partition) {
        this.partition = partition;
        return this;
    }

    public SetWindowFields sortBy(Sort... sorts) {
        this.sorts = sorts;
        return this;
    }

    /**
     * @return the sort values
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Sort[] sorts() {
        return sorts;
    }

    public static class Output {
        private final String name;
        private Expression operator;
        private Window window;

        private Output(String name) {
            this.name = name;
        }

        public static Output output(String name) {
            return new Output(name);
        }

        /**
         * @return the name
         * @morphia.internal
         */
        @MorphiaInternal
        public String name() {
            return name;
        }

        /**
         * @return the operator
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public Expression operator() {
            return operator;
        }

        public Output operator(Expression operator) {
            this.operator = operator;
            return this;
        }

        public Window window() {
            window = new Window(this);
            return window;
        }

        /**
         * @return the window
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public Window windowDef() {
            return window;
        }
    }

    public static class Window {
        private final Output output;
        private List<Object> documents;
        private List<Object> range;
        private TimeUnit unit;

        private Window(Output output) {
            this.output = output;
        }

        public Output documents(Object lower, Object upper) {
            documents = List.of(lower, upper);
            return output;
        }

        /**
         * @return the documents
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public List<Object> documents() {
            return documents;
        }

        public Output range(Object lower, Object upper, TimeUnit unit) {
            range = List.of(lower, upper);
            this.unit = unit;
            return output;
        }

        public Output range(Object lower, Object upper) {
            range = List.of(lower, upper);
            return output;
        }

        /**
         * @return the range
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public List<Object> range() {
            return range;
        }

        /**
         * @return the unit
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public TimeUnit unit() {
            return unit;
        }
    }
}
