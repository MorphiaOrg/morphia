package dev.morphia.query.experimental.filters;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
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
    @Nullable
    private Object value;
    private boolean not;
    private boolean validate;
    private Class<?> entityClass;
    private PathTarget pathTarget;
    private boolean mapped;

    protected Filter(String name) {
        this.name = name;
    }

    protected Filter(String name, @Nullable String field, @Nullable Object value) {
        this.name = name;
        this.field = field != null ? field : "";
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
     * @param datastore the datastore
     * @param writer    the writer
     * @param context   the context
     * @morphia.internal
     */
    public void encode(Datastore datastore, BsonWriter writer, EncoderContext context) {
        document(writer, path(datastore.getMapper()), () -> {
            if (not) {
                document(writer, "$not", () -> {
                    writeNamedValue(name, getValue(datastore), datastore, writer, context);
                });
            } else {
                writeNamedValue(name, getValue(datastore), datastore, writer, context);
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
     * @return the filter field
     * @morphia.internal
     */
    @Nullable
    public String getField() {
        return field;
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
     * @return the filter name
     * @morphia.internal
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * @return the filter value
     * @morphia.internal
     */
    @Nullable
    public Object getValue() {
        return value;
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
        pathTarget = null;
        mapped = false;
        return this;
    }

    @Nullable
    protected Object getValue(Datastore datastore) {
        if (!mapped) {
            PathTarget target = pathTarget(datastore.getMapper());
            OperationTarget operationTarget = new OperationTarget(pathTarget, value);
            this.value = operationTarget.getValue();
            PropertyModel property = target.getTarget();
            if (property != null && property.specializeCodec(datastore) instanceof PropertyHandler) {
                this.value = ((Document) operationTarget.encode(datastore)).get(field);
            }
            mapped = true;
        }
        return value;
    }

    @Override
    public String toString() {
        return format("%s %s %s", field, name, value);
    }

    protected String path(Mapper mapper) {
        return pathTarget(mapper).translatedPath();
    }

    private PathTarget pathTarget(Mapper mapper) {
        if (pathTarget == null) {
            pathTarget = new PathTarget(mapper, entityClass, field, validate);
        }

        return pathTarget;
    }

    protected void writeNamedValue(@Nullable String name, @Nullable Object named, Datastore datastore, BsonWriter writer,
                                   EncoderContext encoderContext) {
        writer.writeName(name);
        if (named != null) {
            Codec codec = datastore.getCodecRegistry().get(named.getClass());
            encoderContext.encodeWithChildContext(codec, writer, named);
        } else {
            writer.writeNull();
        }
    }

    protected void writeUnnamedValue(@Nullable Object value, Datastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        if (value != null) {
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }
}
