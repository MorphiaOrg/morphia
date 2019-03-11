package dev.morphia.converters;

import com.mongodb.DBRef;
import dev.morphia.Key;
import dev.morphia.mapping.MappedField;

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


        DBRef ref = (DBRef) o;

        MappedField actualType = getActualType(optionalExtraInfo);


        final Class<?> keyType = actualType != null
                                 ? actualType.getConcreteType()
                                 : getMapper().getClassFromCollection(ref.getCollectionName());

        final Key<?> key = new Key<Object>(keyType, ref.getCollectionName(), ref.getId());

        return key;
    }

    private MappedField getActualType(final MappedField field) {
        if (field == null) {
            return null;
        }
        MappedField mappedField = field.getTypeParameters().get(0);
        if (mappedField.getTypeParameters().size() != 0) {
            mappedField = getActualType(mappedField);
        }
        return mappedField;
    }

    @Override
    public Object encode(final Object t, final MappedField optionalExtraInfo) {
        if (t == null) {
            return null;
        }
        return getMapper().keyToDBRef((Key) t);
    }

}
