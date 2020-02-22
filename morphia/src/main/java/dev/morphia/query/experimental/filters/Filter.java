package dev.morphia.query.experimental.filters;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.OperationTarget;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static java.lang.String.format;

/**
 * Base class for query filters
 *
 * @since 2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Filter {
    private String filterName;
    private String field;
    private Object value;
    private boolean not;
    private boolean validate;
    private Class<?> entityClass;
    private PathTarget pathTarget;
    private boolean mapped;

    protected Filter(final String name) {
        this.filterName = name;
    }

    protected Filter(final String name, final String field, final Object value) {
        this.filterName = name;
        this.field = field;
        this.value = value;
    }

    protected boolean isNot() {
        return not;
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
        writeUnnamedValue(getValue(mapper), mapper, writer, context);
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
     * @return this
     */
    public Filter not() {
        this.not = true;
        return this;
    }

    @Override
    public String toString() {
        return format("%s %s %s", field, filterName, value);
    }

    protected String field(final Mapper mapper) {
        if (field != null && pathTarget == null) {
            pathTarget = new PathTarget(mapper, entityClass, field, validate);
            field = pathTarget.translatedPath();
        }

        return field;
    }

    protected String getFilterName() {
        return filterName;
    }

    protected Object getValue() {
        return value;
    }

    protected Object getValue(final Mapper mapper) {
        if (!mapped) {
            String field = field(mapper);
            if (pathTarget != null) {
                this.value = ((Document) new OperationTarget(pathTarget, value)
                                             .encode(mapper))
                                 .get(field);
            }
            mapped = true;
        }
        return value;
    }

    protected void writeNamedValue(final String name, final Object named, final Mapper mapper, final BsonWriter writer,
                                   final EncoderContext encoderContext) {
        writer.writeName(name);
        if (named != null) {
            Codec codec = mapper.getCodecRegistry().get(named.getClass());
            encoderContext.encodeWithChildContext(codec, writer, named);
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
