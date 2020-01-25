package dev.morphia.aggregation.experimental.stages;

import org.bson.BsonWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorts all input documents and returns them to the pipeline in sorted order.
 *
 * @mongodb.driver.manual reference/operator/aggregation/sort $sort
 */
public class Sort extends Stage {
    private List<SortType> sorts = new ArrayList<>();

    protected Sort() {
        super("$sort");
    }

    /**
     * Creates a sort stage.
     *
     * @return the new stage
     */
    public static Sort on() {
        return new Sort();
    }

    /**
     * Adds an ascending sort definition on the field.
     *
     * @param field      the sort field
     * @param additional any additional fields to sort on
     * @return this
     */
    public Sort ascending(final String field, final String... additional) {
        sorts.add(new SortType(field, Direction.ASCENDING));
        for (final String another : additional) {
            sorts.add(new SortType(another, Direction.ASCENDING));
        }
        return this;
    }

    /**
     * Adds an descending sort definition on the field.
     *
     * @param field the sort field
     * @param additional any additional fields to sort on
     * @return this
     */
    public Sort descending(final String field, final String... additional) {
        sorts.add(new SortType(field, Direction.DESCENDING));
        for (final String another : additional) {
            sorts.add(new SortType(another, Direction.DESCENDING));
        }
        return this;
    }

    /**
     * @return the sorts
     * @morphia.internal
     */
    public List<SortType> getSorts() {
        return sorts;
    }

    /**
     * Adds a sort by the computed textScore metadata in descending order.
     *
     * @param field the sort field
     * @return this
     */
    public Sort meta(final String field) {
        sorts.add(new SortType(field, Direction.META));
        return this;
    }

    /**
     * The sort types
     */
    public enum Direction {
        ASCENDING {
            @Override
            public void encode(final BsonWriter writer) {
                writer.writeInt32(1);
            }
        },
        DESCENDING {
            @Override
            public void encode(final BsonWriter writer) {
                writer.writeInt32(-1);
            }
        },
        META {
            @Override
            public void encode(final BsonWriter writer) {
                writer.writeStartDocument();
                writer.writeString("$meta", "textScore");
                writer.writeEndDocument();
            }
        };

        /**
         * @param writer the writer to use
         * @morphia.internal
         */
        public abstract void encode(BsonWriter writer);
    }

    /**
     * @morphia.internal
     */
    public class SortType {
        private String field;
        private Direction direction;

        protected SortType(final String field, final Direction direction) {
            this.field = field;
            this.direction = direction;
        }

        /**
         * @return the direction
         * @morphia.internal
         */
        public Direction getDirection() {
            return direction;
        }

        /**
         * @return the field
         * @morphia.internal
         */
        public String getField() {
            return field;
        }
    }
}
