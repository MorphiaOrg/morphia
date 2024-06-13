package dev.morphia.query.filters;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import dev.morphia.mapping.codec.pojo.PropertyModel;
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
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Filter {
    private final String name;
    @Nullable
    private String field;
    @Nullable
    private Object value;
    private boolean not;
    private boolean validate;
    private Class<?> entityClass;
    @Nullable
    private PathTarget pathTarget;
    private boolean mapped;

    /**
     * @param name the name of the filter
     */
    protected Filter(String name) {
        this.name = name;
    }

    /**
     * @param name  the name of the filter
     * @param field the field
     * @param value the value
     */
    protected Filter(String name, @Nullable String field, @Nullable Object value) {
        this.name = name;
        this.field = field != null ? field : "";
        this.value = value;
    }

    /**
     * @return true if this filter has been notted
     * @hidden
     * @morphia.internal
     * @see #not()
     */
    @MorphiaInternal
    public boolean isNot() {
        return not;
    }

    /**
     * Sets the query entity type on the filter
     *
     * @param type the type
     * @return this
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Filter entityType(Class<?> type) {
        this.entityClass = type;
        return this;
    }

    /**
     * @return the filter field
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
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
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public String getName() {
        return name;
    }

    /**
     * @return the filter value
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public Object getValue() {
        return value;
    }

    /**
     * @param value the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected void setValue(Object value) {
        this.value = value;
    }

    /**
     * Sets whether to validate field names or not
     *
     * @param validate true to validate
     * @return this
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Filter isValidating(boolean validate) {
        this.validate = validate;
        pathTarget = null;
        mapped = false;
        return this;
    }

    /**
     * @param datastore the datastore
     * @hidden
     * @morphia.internal
     * @return the value
     */
    @MorphiaInternal
    @Nullable
    public Object getValue(MorphiaDatastore datastore) {
        if (!mapped) {
            PathTarget target = pathTarget(datastore.getMapper());
            OperationTarget operationTarget = new OperationTarget(pathTarget, value);
            this.value = operationTarget.getValue();
            PropertyModel property = target.target();
            if (property != null && property.specializeCodec(datastore) instanceof PropertyHandler) {
                this.value = ((Document) operationTarget.encode(datastore)).get(field);
            }
            mapped = true;
        }
        return value;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String toString() {
        return format("%s %s %s", field, name, value);
    }

    /**
     * @param mapper the mapper
     * @hidden
     * @morphia.internal
     * @return the path to use
     */
    @MorphiaInternal
    public String path(Mapper mapper) {
        return pathTarget(mapper).translatedPath();
    }

    private PathTarget pathTarget(Mapper mapper) {
        if (pathTarget == null) {
            pathTarget = new PathTarget(mapper, entityClass, field, validate);
        }

        return pathTarget;
    }

    /**
     * @param value          the value to write
     * @param datastore      the datastore
     * @param writer         the writer
     * @param encoderContext the encoder context
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected void writeValue(@Nullable Object value, MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        if (value != null) {
            Codec codec = datastore.getCodecRegistry().get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        } else {
            writer.writeNull();
        }
    }
}
