package dev.morphia.converters;


import dev.morphia.mapping.MappedField;

import java.util.UUID;


/**
 * provided by http://code.google.com/p/morphia/issues/detail?id=320
 *
 * @author stummb
 * @author scotthernandez
 */
public class UUIDConverter extends TypeConverter implements SimpleValueConverter {

    /**
     * Creates the Converter.
     */
    public UUIDConverter() {
        super(UUID.class);
    }

    @Override
    public Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
        if (fromDBObject == null || fromDBObject instanceof UUID) {
            return fromDBObject;
        } /*else if (fromDBObject instanceof Binary) {
            mapper.
            Binary binary = (Binary) fromDBObject;
            return UuidHelper.decodeBinaryToUuid(binary.getData(), binary.getType(), getMapper().getOptions().getUU);;
        }*/
        return UUID.fromString((String) fromDBObject);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        return value == null ? null : value.toString();
    }
}
