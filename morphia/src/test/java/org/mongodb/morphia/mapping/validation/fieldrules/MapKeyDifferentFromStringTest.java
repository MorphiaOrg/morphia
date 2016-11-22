package org.mongodb.morphia.mapping.validation.fieldrules;


import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.mapping.validation.ConstraintViolation;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.mapping.validation.DefaultClassConstraintFactory;
import org.mongodb.morphia.testutil.TestEntity;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MapKeyDifferentFromStringTest extends TestBase {

    @Test
    public void testCheck() {
        getMorphia().map(MapWithWrongKeyType1.class);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidKeyType() {
        getMorphia().map(MapWithWrongKeyType3.class);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidReferenceType() {
        getMorphia().map(MapWithWrongKeyType2.class);
    }

    /**
     * This is really a test of the ability to set a constraint factory and change the behavior of the validator
     * as needed.
     */
    @Test
    public void testConvertibleType() {
        Morphia morphia = getMorphia();
        morphia.getMapper().getConverters().addConverter(new WrappedUriConverter());
        morphia.getMapper().getConverters().hasSimpleValueConverter(WrappedUri.class);
        morphia.getMapper().getOptions().setConstraintFactory(new DefaultClassConstraintFactory() {
            @Override
            public List<ClassConstraint> getConstraints(final ObjectFactory creator) {
                List<ClassConstraint> constraints = super.getConstraints(creator);
                for (ClassConstraint constraint : constraints) {
                    if (constraint instanceof MapKeyDifferentFromString) {
                        assertTrue(constraints.remove(constraint));
                        break;
                    }
                }
                constraints.add(new FieldConstraint() {
                    private static final String SUPPORTED = "(Map<String/Enum/Long/ObjectId/..., ?>)";

                    @Override
                    protected void check(final Mapper mapper, final MappedClass mc, final MappedField mf, final Set<ConstraintViolation> ve) {
                        if (mf.isMap() && (!mf.hasAnnotation(Serialized.class))) {
                            final Class<?> aClass = ReflectionUtils.getParameterizedClass(mf.getField(), 0);
                            // WARN if not parameterized : null or Object...
                            if (aClass == null || Object.class.equals(aClass)) {
                                ve.add(new ConstraintViolation(
                                    ConstraintViolation.Level.WARNING, mc, mf, this.getClass(),
                                    "Maps cannot be keyed by Object (Map<Object,?>); Use a parametrized type that is supported "
                                        + SUPPORTED));
                            } else if (!aClass.equals(String.class) && !aClass.equals(ObjectId.class) && !ReflectionUtils.isPrimitiveLike(
                                aClass) && !mapper.getConverters().hasSimpleValueConverter(aClass) && !mapper.getConverters().hasSimpleValueConverter(mf)) {
                                ve.add(new ConstraintViolation(
                                    ConstraintViolation.Level.FATAL, mc, mf,  this.getClass(),
                                    "Maps must be keyed by a simple type " + SUPPORTED + "; " + aClass
                                        + " is not supported as a map key type."));
                            }
                        }
                    }
                });
                return constraints;
            }
        });
        morphia.map(MapWithWrongKeyType4.class);
    }

    public static class MapWithWrongKeyType1 extends TestEntity {
        @Serialized
        private Map<Integer, Integer> shouldBeOk = new HashMap<Integer, Integer>();

    }

    public static class MapWithWrongKeyType2 extends TestEntity {
        @Reference
        private Map<Integer, Integer> shouldBeOk = new HashMap<Integer, Integer>();

    }

    public static class MapWithWrongKeyType3 extends TestEntity {
        @Embedded
        private Map<BigDecimal, Integer> shouldBeOk = new HashMap<BigDecimal, Integer>();

    }

    public static class MapWithWrongKeyType4 extends TestEntity {
        @Embedded
        private Map<WrappedUri, Integer> shouldBeOk = new HashMap<WrappedUri, Integer>();

    }

    public static class WrappedUri {
        private final URI uri;

        public WrappedUri(final URI uri) {
            assert uri != null;
            this.uri = uri;
        }

        @Override
        public String toString() {
            return uri.toString();
        }
    }

    public static class WrappedUriConverter extends TypeConverter implements SimpleValueConverter {

        public WrappedUriConverter() {
            super(WrappedUri.class);
        }

        @Override
        public Object decode(
            final Class<?> targetClass, final Object fromDBObject, final MappedField optionalExtraInfo
        ) {
            return fromDBObject != null ? new WrappedUri(URI.create(fromDBObject.toString())) : null;
        }

        @Override
        public Object encode(final Object value, final MappedField optionalExtraInfo) {
            return value != null ? value.toString() : null;
        }
    }

}
