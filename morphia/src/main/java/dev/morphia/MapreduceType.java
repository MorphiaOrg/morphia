package dev.morphia;


import com.mongodb.MapReduceCommand.OutputType;

/**
 * Defines how the output of the map reduce job is handled.
 * @deprecated use {@link OutputType} instead
 */
@Deprecated
public enum MapreduceType {
    REPLACE,
    MERGE,
    REDUCE,
    INLINE;

    /**
     * Finds the type represented by the value given
     *
     * @param value the value to look up
     * @return the type represented by the value given
     */
    public static MapreduceType fromString(final String value) {
        for (int i = 0; i < values().length; i++) {
            final MapreduceType fo = values()[i];
            if (fo.name().equals(value)) {
                return fo;
            }
        }
        return null;
    }

    OutputType toOutputType() {
        switch (this) {
            case REDUCE:
                return OutputType.REDUCE;
            case MERGE:
                return OutputType.MERGE;
            case INLINE:
                return OutputType.INLINE;
            default:
                return OutputType.REPLACE;
        }

    }
}
