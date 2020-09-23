package dev.morphia.aggregation.experimental.stages;

import org.bson.BsonWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorts all input documents and returns them to the pipeline in sorted order.
 *
 * @aggregation.expression $sort
 */
public class Sort extends Stage {
    private final List<SortType> sorts = new ArrayList<>();

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
    public Sort ascending(String field, String... additional) {
        sorts.add(new SortType(field, Direction.ASCENDING));
        for (String another : additional) {
            sorts.add(new SortType(another, Direction.ASCENDING));
        }
        return this;
    }

    /**
     * Adds an descending sort definition on the field.
     *
     * @param field      the sort field
     * @param additional any additional fields to sort on
     * @return this
     */
    public Sort descending(String field, String... additional) {
        sorts.add(new SortType(field, Direction.DESCENDING));
        for (String another : additional) {
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
    public Sort meta(String field) {
        sorts.add(new SortType(field, Direction.META));
        return this;
    }

    /**
     * The sort types
     */
    public enum Direction {
        ASCENDING {
            @Override
            public void encode(BsonWriter writer) {
                writer.writeInt32(1);
            }
        },
        DESCENDING {
            @Override
            public void encode(BsonWriter writer) {
                writer.writeInt32(-1);
            }
        },
        META {
            @Override
            public void encode(BsonWriter writer) {
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
        private final String field;
        private final Direction direction;

        protected SortType(String field, Direction direction) {
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
