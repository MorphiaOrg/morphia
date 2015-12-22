package org.mongodb.morphia.converters;

import com.mongodb.DBRef;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.MappedField;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class KeyConverter extends TypeConverter {

    /**
     * Creates the Converter.
     */
    public KeyConverter() {
        super(Key.class);
    }

    @Override
    public Object decode(final Class targetClass, final Object o, final MappedField optionalExtraInfo) {
        if (o == null) {
            return null;
        }
        if (!(o instanceof DBRef)) {
            throw new ConverterException(String.format("cannot convert %s to Key because it isn't a DBRef", o.toString()));
        }

        return getMapper().refToKey((DBRef) o);
    }

    @Override
    public Object encode(final Object t, final MappedField optionalExtraInfo) {
        if (t == null) {
            return null;
        }
        return getMapper().keyToDBRef((Key) t);
    }

}
