package dev.morphia.aggregation.stages;

import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.query.Sort;

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

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected SetWindowFields() {
        super("$setWindowFields");
    }

    /**
     * Performs operations on a specified span of documents in a collection, known as a window, and returns the results based on the chosen
     * window operator.
     *
     * @return the new stage
     * @mongodb.server.release 5.0
     * @aggregation.expression $setWindowFields
     * @since 2.3
     */
    public static SetWindowFields setWindowFields() {
        return new SetWindowFields();
    }

    /**
     * Specifies the field(s) to append to the documents in the output returned by the $setWindowFields stage.
     *
     * @param outputs the output fields
     * @return this
     */
    public SetWindowFields output(Output... outputs) {
        this.outputs = outputs;
        return this;
    }

    /**
     * @return the outputs
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Output[] outputs() {
        return outputs;
    }

    /**
     * @return the partition
     * @hidden
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

    /**
     * Specifies the field(s) to sort the documents by in the partition.
     *
     * @param sorts the sort criteria
     * @return this
     */
    public SetWindowFields sortBy(Sort... sorts) {
        this.sorts = sorts;
        return this;
    }

    /**
     * @return the sort values
     * @hidden
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

        /**
         * Creates a named output
         *
         * @param name the name
         * @return the new Output
         */
        public static Output output(String name) {
            return new Output(name);
        }

        /**
         * @return the name
         * @hidden
         * @morphia.internal
         */
        @MorphiaInternal
        public String name() {
            return name;
        }

        /**
         * @return the operator
         * @hidden
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public Expression operator() {
            return operator;
        }

        /**
         * The operator to use in the output.
         *
         * @param operator the output
         * @return the Output
         */
        public Output operator(Expression operator) {
            this.operator = operator;
            return this;
        }

        /**
         * Creates a new window.
         *
         * @return the new window
         */
        public Window window() {
            window = new Window(this);
            return window;
        }

        /**
         * @return the window
         * @hidden
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

        /**
         * A window where the lower and upper boundaries are specified relative to the position of the current document read from the
         * collection.
         *
         * @param lower the lower bound
         * @param upper the upper bound
         * @return the Output
         */
        public Output documents(Object lower, Object upper) {
            documents = List.of(lower, upper);
            return output;
        }

        /**
         * @return the documents
         * @hidden
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public List<Object> documents() {
            return documents;
        }

        /**
         * A window where the lower and upper boundaries are defined using a range of values based on the sortBy field in the current
         * document.
         *
         * @param lower the lower bound
         * @param upper the upper bound
         * @param unit  the unit to use
         * @return the Output
         */
        public Output range(Object lower, Object upper, TimeUnit unit) {
            range = List.of(lower, upper);
            this.unit = unit;
            return output;
        }

        /**
         * A window where the lower and upper boundaries are defined using a range of values based on the sortBy field in the current
         * document.
         *
         * @param lower the lower bound
         * @param upper the upper bound
         * @return the Output
         */
        public Output range(Object lower, Object upper) {
            range = List.of(lower, upper);
            return output;
        }

        /**
         * @return the range
         * @hidden
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public List<Object> range() {
            return range;
        }

        /**
         * @return the unit
         * @hidden
         * @morphia.internal
         */
        @Nullable
        @MorphiaInternal
        public TimeUnit unit() {
            return unit;
        }
    }
}
