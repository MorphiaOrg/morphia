package dev.morphia.converters;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import dev.morphia.converters.experimental.ReferenceConverter;
import dev.morphia.geo.GeometryConverter;
import dev.morphia.geo.GeometryShapeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

/**
 * Default encoders
 *
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class DefaultConverters extends Converters {
    public static final boolean JAVA_8;
    private final IdentityConverter identityConverter;
    private final SerializedObjectConverter serializedConverter;

    static {
        boolean found;
        try {
            Class.forName("java.time.LocalDateTime");
            found = true;
        } catch (ClassNotFoundException e) {
            found = false;
        }
        JAVA_8 = found;
    }

    /**
     * Creates a bundle with a particular Mapper.
     *
     * @param mapper the Mapper to use
     */
    public DefaultConverters(Mapper mapper) {
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
        addConverter(new BigDecimalConverter());
        addConverter(new CurrencyConverter());
        addConverter(new ReferenceConverter(mapper));

        // Converters for Geo entities
        addConverter(new GeometryShapeConverter.PointConverter());
        addConverter(new GeometryShapeConverter.LineStringConverter());
        addConverter(new GeometryShapeConverter.MultiPointConverter());
        addConverter(new GeometryShapeConverter.MultiLineStringConverter());
        addConverter(new GeometryShapeConverter.PolygonConverter());
        addConverter(new GeometryShapeConverter.MultiPolygonConverter());
        addConverter(new GeometryConverter());

        if (JAVA_8) {
            addConverter(new LocalTimeConverter());
            addConverter(new LocalDateTimeConverter(mapper));
            addConverter(new LocalDateConverter(mapper));
            addConverter(new InstantConverter());
        }

        //generic converter that will just pass things through.
        identityConverter = new IdentityConverter();
        serializedConverter = new SerializedObjectConverter();
    }

    @Override
    protected TypeConverter getEncoder(Class c) {
        TypeConverter encoder = super.getEncoder(c);

        if (encoder == null && identityConverter.canHandle(c)) {
            encoder = identityConverter;
        }
        return encoder;
    }

    @Override
    protected TypeConverter getEncoder(Object val, MappedField mf) {
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
