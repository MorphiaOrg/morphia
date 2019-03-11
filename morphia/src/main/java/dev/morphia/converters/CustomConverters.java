package dev.morphia.converters;

import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import static java.lang.String.format;

/**
 * Defines a bundle of converters that will delegate to the DefaultConverters for unknown types but provides a chance to override the
 * converter used for different types.
 *
 * @see DefaultConverters
 */
public class CustomConverters extends Converters {
    private final DefaultConverters defaultConverters;

    /**
     * Creates a bundle with a particular Mapper.
     *
     * @param mapper the Mapper to use
     */
    public CustomConverters(final Mapper mapper) {
        super(mapper);
        defaultConverters = new DefaultConverters(mapper);
    }

    @Override
    public boolean isRegistered(final Class<? extends TypeConverter> tcClass) {
        return super.isRegistered(tcClass) || defaultConverters.isRegistered(tcClass);
    }

    @Override
    public void removeConverter(final TypeConverter tc) {
        super.removeConverter(tc);
        defaultConverters.removeConverter(tc);
    }

    @Override
    protected TypeConverter getEncoder(final Class c) {
        TypeConverter encoder = super.getEncoder(c);
        if (encoder == null) {
            encoder = defaultConverters.getEncoder(c);
        }

        if (encoder != null) {
            return encoder;
        }
        throw new ConverterNotFoundException(format("Cannot find encoder for %s", c.getName()));
    }

    @Override
    protected TypeConverter getEncoder(final Object val, final MappedField mf) {
        TypeConverter encoder = super.getEncoder(val, mf);
        if (encoder == null) {
            encoder = defaultConverters.getEncoder(val, mf);
        }

        if (encoder != null) {
            return encoder;
        }

        throw new ConverterNotFoundException(format("Cannot find encoder for %s as need for %s", mf.getType(), mf.getFullName()));
    }
}
