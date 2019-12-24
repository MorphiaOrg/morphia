package dev.morphia.aggregation.experimental.stages;

import org.bson.BsonWriter;

import java.util.ArrayList;
import java.util.List;

public class Sort extends Stage {
    private List<SortType> sorts = new ArrayList<>();

    protected Sort() {
        super("$sort");
    }

    public static Sort on() {
        return new Sort();
    }

    public Sort ascending(final String field) {
        sorts.add(new SortType(field, Direction.ASCENDING));
        return this;
    }

    public Sort descending(final String field) {
        sorts.add(new SortType(field, Direction.DESCENDING));
        return this;
    }

    public List<SortType> getSorts() {
        return sorts;
    }

    public Sort meta(final String field) {
        sorts.add(new SortType(field, Direction.META));
        return this;
    }

    public enum Direction {
        ASCENDING {
            @Override
            public void write(final BsonWriter writer) {
                writer.writeInt32(1);
            }
        },
        DESCENDING {
            @Override
            public void write(final BsonWriter writer) {
                writer.writeInt32(-1);
            }
        },
        META {
            @Override
            public void write(final BsonWriter writer) {
                writer.writeStartDocument();
                writer.writeString("$meta", "textScore");
                writer.writeEndDocument();
            }
        };

        public abstract void write(final BsonWriter writer);
    }

    public class SortType {
        private String field;
        private Direction direction;

        public SortType(final String field, final Direction direction) {
            this.field = field;
            this.direction = direction;
        }

        public Direction getDirection() {
            return direction;
        }

        public String getField() {
            return field;
        }
    }
}
