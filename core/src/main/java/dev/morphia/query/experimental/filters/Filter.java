package dev.morphia.query.experimental.filters;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.query.OperationTarget;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static java.lang.String.format;

/**
 * Base class for query filters
 *
 * @since 2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Filter {
    private final String name;
    private String field;
    private Object value;
    private boolean not;
    private boolean validate;
    private Class<?> entityClass;
    private PathTarget pathTarget;
    private boolean mapped;

    protected Filter(String name) {
        this.name = name;
    }

    protected Filter(String name, String field, Object value) {
        this.name = name;
        this.field = field;
        this.value = value;
    }

    /**
     * @return true if this filter has been notted
     * @morphia.internal
     * @see #not()
     */
    public boolean isNot() {
        return not;
    }

    /**
     * @param mapper  the mapper
     * @param writer  the writer
     * @param context the context
     * @morphia.internal
     */
    public void encode(Mapper mapper, BsonWriter writer, EncoderContext context) {
        document(writer, path(mapper), () -> {
            if (not) {
                document(writer, "$not", () -> {
                    writeNamedValue(name, getValue(mapper), mapper, writer, context);
                });
            } else {
                writeNamedValue(name, getValue(mapper), mapper, writer, context);
            }
        });
    }

    /**
     * Sets the query entity type on the filter
     *
     * @param type the type
     * @return this
     * @morphia.internal
     */
    public Filter entityType(Class<?> type) {
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
    public Filter isValidating(boolean validate) {
        this.validate = validate;
        return this;
    }

    /**
     * Negates this filter by wrapping in "$not: {}"
     *
     * @return this
     * @query.filter $not
     */
    public Filter not() {
        this.not = true;
        return this;
    }

    /**
     * @return the filter field
     * @morphia.internal
     */
    public String getField() {
        return field;
    }

    protected Object getValue(Mapper mapper) {
        if (!mapped) {
            PathTarget target = pathTarget(mapper);
            if (target != null) {
                OperationTarget operationTarget = new OperationTarget(pathTarget, value);
                this.value = operationTarget.getValue();
                PropertyModel mappedField = target.getTarget();
                if (mappedField != null && mappedField.getCodec() instanceof PropertyHandler) {
                    this.value = ((Document) operationTarget.encode(mapper)).get(field);
                }
            }
            mapped = true;
        }
        return value;
    }

    /**
     * @return the filter name
     * @morphia.internal
     */
    public String getName() {
        return name;
    }

    /**
     * @return the filter value
     * @morphia.internal
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return format("%s %s %s", field, name, value);
    }

    protected String path(Mapper mapper) {
        if (field != null && pathTarget == null) {
            pathTarget = new PathTarget(mapper, entityClass, field, validate);
            field = pathTarget.translatedPath();
        }

        return field;
    }

    private PathTarget pathTarget(Mapper mapper) {
        if (pathTarget == null) {
            pathTarget = new PathTarget(mapper, entityClass, field, validate);
        }

        return pathTarget;
    }

    protected void writeNamedValue(String name, Object named, Mapper mapper, BsonWriter writer,
                                   EncoderContext encoderContext) {
        writer.writeName(name);
        if (named != null) {
            Codec codec = mapper.getCodecRegistry().get(named.getClass());
            encoderContext.encodeWithChildContext(codec, writer, named);
        } else {
            writer.writeNull();
        }
    }

    protected void writeUnnamedValue(Object value, Mapper mapper, BsonWriter writer,
                                     EncoderContext encoderContext) {
        if (value != null) {
            Codec codec = mapper.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }
}
