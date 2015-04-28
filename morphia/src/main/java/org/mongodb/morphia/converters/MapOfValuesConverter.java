package org.mongodb.morphia.converters;

import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.utils.IterHelper;
import org.mongodb.morphia.utils.IterHelper.MapIterCallback;
import org.mongodb.morphia.utils.ReflectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MapOfValuesConverter extends TypeConverter {
    @Override
    protected boolean isSupported(final Class<?> c, final MappedField optionalExtraInfo) {
        if (optionalExtraInfo != null) {
            return optionalExtraInfo.isMap();
        } else {
            return ReflectionUtils.implementsInterface(c, Map.class);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object decode(final Class targetClass, final Object fromDBObject, final MappedField mf) {
        if (fromDBObject == null) {
            return null;
        }


        final Map values = getMapper().getOptions().getObjectFactory().createMap(mf);
        new IterHelper<Object, Object>().loopMap(fromDBObject, new MapIterCallback<Object, Object>() {
            @Override
            public void eval(final Object key, final Object val) {
                final Object objKey = getMapper().getConverters().decode(mf.getMapKeyClass(), key, mf);
                values.put(objKey, val != null ? getMapper().getConverters().decode(mf.getSubClass(), val, mf) : null);
            }
        });

        return values;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object encode(final Object value, final MappedField mf) {
        if (value == null) {
            return null;
        }

        final Map<Object, Object> map = (Map<Object, Object>) value;
        if (!map.isEmpty() || getMapper().getOptions().isStoreEmpties()) {
            final Map mapForDb = new HashMap();
            for (final Map.Entry<Object, Object> entry : map.entrySet()) {
                final String strKey = getMapper().getConverters().encode(entry.getKey()).toString();
                mapForDb.put(strKey, getMapper().getConverters().encode(entry.getValue()));
            }
            return mapForDb;
        }
        return null;
    }
}