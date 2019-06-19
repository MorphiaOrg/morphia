package dev.morphia.ext;


import dev.morphia.TestBase;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Id;
import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;


/**
 * Example converter which stores the enum value instead of string (name)
 *
 * @author scotthernandez
 */
public class EnumValueConverterTest extends TestBase {

    @Test
    public void testEnum() {
        final EnumEntity ee = new EnumEntity();
        getDs().save(ee);
        final Document dbObj = getDs().getCollection(EnumEntity.class).find().first();
        Assert.assertEquals(1, dbObj.get("val"));
    }

    private enum AEnum {
        One,
        Two

    }

    private static class AEnumConverter extends TypeConverter implements SimpleValueConverter {

        AEnumConverter() {
            super(AEnum.class);
        }

        @Override
        public Object decode(final Class targetClass, final Object fromDocument, final MappedField optionalExtraInfo) {
            if (fromDocument == null) {
                return null;
            }
            return AEnum.values()[(Integer) fromDocument];
        }

        @Override
        public Object encode(final Object value, final MappedField optionalExtraInfo) {
            if (value == null) {
                return null;
            }

            return ((Enum) value).ordinal();
        }
    }

    @Converters(AEnumConverter.class)
    private static class EnumEntity {
        @Id
        private ObjectId id = new ObjectId();
        private AEnum val = AEnum.Two;

    }
}
