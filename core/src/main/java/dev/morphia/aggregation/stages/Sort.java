package dev.morphia.aggregation.stages;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.BsonWriter;

import static dev.morphia.mapping.codec.CodecHelper.document;

/**
 * Sorts all input documents and returns them to the pipeline in sorted order.
 *
 * @aggregation.stage $sort
 */
public class Sort extends Stage {
    private final List<SortType> sorts = new ArrayList<>();

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected Sort() {
        super("$sort");
    }

    /**
     * Creates a sort stage.
     *
     * @return the new stage
     * @since 2.2
     * @aggregation.stage $sort
     */
    public static Sort sort() {
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
     * Adds a descending sort definition on the field.
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
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
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
        /**
         * ascending
         */
        ASCENDING {
            @Override
            public void encode(BsonWriter writer) {
                writer.writeInt32(1);
            }
        },
        /**
         * descending
         */
        DESCENDING {
            @Override
            public void encode(BsonWriter writer) {
                writer.writeInt32(-1);
            }
        },
        /**
         * sort by meta/textScore
         */
        META {
            @Override
            public void encode(BsonWriter writer) {
                document(writer, () -> writer.writeString("$meta", "textScore"));
            }
        };

        /**
         * @param writer the writer to use
         * @hidden
         * @morphia.internal
         */
        @MorphiaInternal
        public abstract void encode(BsonWriter writer);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public static class SortType {
        private final String field;
        private final Direction direction;

        private SortType(String field, Direction direction) {
            this.field = field;
            this.direction = direction;
        }

        /**
         * @return the direction
         * @hidden
         * @morphia.internal
         */
        @MorphiaInternal
        public Direction direction() {
            return direction;
        }

        /**
         * @return the field
         * @hidden
         * @morphia.internal
         */
        @MorphiaInternal
        public String field() {
            return field;
        }
    }
}
