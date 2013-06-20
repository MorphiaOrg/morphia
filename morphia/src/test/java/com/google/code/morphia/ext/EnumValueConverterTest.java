package com.google.code.morphia.ext;


import org.bson.types.ObjectId;
import org.junit.Test;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Converters;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.converters.SimpleValueConverter;
import com.google.code.morphia.converters.TypeConverter;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.mongodb.DBObject;
import junit.framework.Assert;


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
