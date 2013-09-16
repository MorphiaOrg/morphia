package org.mongodb.morphia.ext;


import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;
import com.mongodb.DBObject;
import org.junit.Assert;


/**
 * Example converter which stores the enum value instead of string (name)
 *
 * @author scotthernandez
 */
public class EnumValueConverterTest extends TestBase {

  @SuppressWarnings({"rawtypes"})
  private static class AEnumConverter extends TypeConverter implements SimpleValueConverter {

    public AEnumConverter() {
      super(AEnum.class);
    }

    @Override
    public Object decode(final Class targetClass, final Object fromDBObject, final MappedField optionalExtraInfo) throws MappingException {
      if (fromDBObject == null) {
        return null;
      }
      return AEnum.values()[(Integer) fromDBObject];
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
      if (value == null) {
        return null;
      }

      return ((Enum) value).ordinal();
    }
  }

  private enum AEnum {
    One,
    Two

  }

  @Converters(AEnumConverter.class)
  private static class EnumEntity {
    @Id ObjectId id = new ObjectId();
    AEnum val = AEnum.Two;

  }

  @Test
  public void testEnum() {
    final EnumEntity ee = new EnumEntity();
    ds.save(ee);
    final DBObject dbObj = ds.getCollection(EnumEntity.class).findOne();
    Assert.assertEquals(1, dbObj.get("val"));
  }
}
