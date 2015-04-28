package org.mongodb.morphia.converters;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.geo.GeometryConverter;
import org.mongodb.morphia.geo.GeometryShapeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;


/**
 * Default encoders
 *
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class DefaultConverters extends Converters {
    private final IdentityConverter identityConverter;
    private final SerializedObjectConverter serializedConverter;

    public DefaultConverters(final Mapper mapper) {
        super(mapper);
        addConverter(new IdentityConverter(DBObject.class, BasicDBObject.class));
        addConverter(new EnumSetConverter());
        addConverter(new EnumConverter());
        addConverter(new StringConverter());
        addConverter(new CharacterConverter());
        addConverter(new ByteConverter());
        addConverter(new BooleanConverter());
        addConverter(new DoubleConverter());
        addConverter(new FloatConverter());
        addConverter(new LongConverter());
        addConverter(new LocaleConverter());
        addConverter(new ShortConverter());
        addConverter(new IntegerConverter());
        addConverter(new CharArrayConverter());
        addConverter(new DateConverter());
        addConverter(new URIConverter());
        addConverter(new KeyConverter());
        addConverter(new MapOfValuesConverter());
        addConverter(new IterableConverter());
        addConverter(new ClassConverter());
        addConverter(new ObjectIdConverter());
        addConverter(new TimestampConverter());

        // Converters for Geo entities
        addConverter(new GeometryShapeConverter.PointConverter());
        addConverter(new GeometryShapeConverter.LineStringConverter());
        addConverter(new GeometryShapeConverter.MultiPointConverter());
        addConverter(new GeometryShapeConverter.MultiLineStringConverter());
        addConverter(new GeometryShapeConverter.PolygonConverter());
        addConverter(new GeometryShapeConverter.MultiPolygonConverter());
        addConverter(new GeometryConverter());

        //generic converter that will just pass things through.
        identityConverter = new IdentityConverter();
        serializedConverter = new SerializedObjectConverter();
    }

    @Override
    protected TypeConverter getEncoder(final Class c) {
        TypeConverter encoder = super.getEncoder(c);

        if (encoder == null && identityConverter.canHandle(c)) {
            encoder = identityConverter;
        }
        return encoder;
    }

    @Override
    protected TypeConverter getEncoder(final Object val, final MappedField mf) {
        if (serializedConverter.canHandle(mf)) {
            return serializedConverter;
        }

        TypeConverter encoder = super.getEncoder(val, mf);
        if (encoder == null && (identityConverter.canHandle(mf)
                                || (val != null && identityConverter.isSupported(val.getClass(), mf)))) {
            encoder = identityConverter;
        }
        return encoder;
    }
}