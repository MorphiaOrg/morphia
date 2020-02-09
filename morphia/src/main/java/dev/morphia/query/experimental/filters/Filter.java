package dev.morphia.query.experimental.filters;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.StringJoiner;

import static java.lang.String.format;


/**
 * Base class for query filters
 *
 * @since 2.0
 */
public class Filter {
    private String filterName;
    private String field;
    private Object value;
    private boolean not;
    private boolean validate;
    private Class<?> entityClass;

    protected Filter(final String name) {
        this.filterName = name;
    }

    protected Filter(final String name, final String field, final Object value) {
        this.filterName = name;
        this.field = field;
        this.value = value;
    }

    /**
     * @param mapper  the mapper
     * @param writer  the writer
     * @param context the context
     * @morphia.internal
     */
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext context) {
        writer.writeStartDocument(field(mapper));
        if (not) {
            writer.writeStartDocument("$not");
        }
        writer.writeName(filterName);
        writeUnnamedValue(value, mapper, writer, context);
        if (not) {
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }

    /**
     * Sets the query entity type on the filter
     *
     * @param type the type
     * @return this
     * @morphia.internal
     */
    public Filter entityType(final Class<?> type) {
        this.entityClass = type;
        return this;
    }

    /**
     * Sets whether to validate field names or not
     *
     * @param validate true to validate
     * @return this
     * @morphia.internal
     */
    public Filter isValidating(final boolean validate) {
        this.validate = validate;
        return this;
    }

    /**
     * Negates this filter by wrapping in "$not: {}"
     *
     * @param not possible negation
     * @return this
     */
    public Filter not(final boolean not) {
        this.not = not;
        return this;
    }

    @Override
    public String toString() {
        return format("%s %s %s", field, filterName, value);
    }

    protected String field(final Mapper mapper) {
        String s = field;
        if (validate) {
            s = new PathTarget(mapper, entityClass, s).translatedPath();
        }

        return s;
    }

    protected String getFilterName() {
        return filterName;
    }

    protected Object getValue() {
        return value;
    }

    protected void writeNamedValue(final String name, final Object value, final Mapper mapper, final BsonWriter writer,
                                   final EncoderContext encoderContext) {
        writer.writeName(name);
        if (value != null) {
            Codec codec = mapper.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }

    protected void writeUnnamedValue(final Object value, final Mapper mapper, final BsonWriter writer,
                                     final EncoderContext encoderContext) {
        if (value != null) {
            Codec codec = mapper.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }
}
