/**
 * 
 */
package com.google.code.morphia.mapping;

import com.google.code.morphia.converters.DefaultConverters;
import com.mongodb.DBObject;

class ValueMapper
{

	private final DefaultConverters converters;
	
	public ValueMapper(DefaultConverters converters) {
		this.converters = converters;
	}

	void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity)
    {
        try
        {
			converters.fromDBObject(dbObject, mf, entity);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

	void toDBObject(final Object entity, final MappedField mf, final DBObject dbObject, MapperOptions opts)
    {
        try
        {
			converters.toDBObject(entity, mf, dbObject, opts);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
