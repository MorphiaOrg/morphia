package org.bson.types;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 *
 */
//CHECKSTYLE:OFF
public final class ObjectId_CustomFieldSerializer {
    //CHECKSTYLE:ON    
    private ObjectId_CustomFieldSerializer() {
    }

    public static void deserialize(final SerializationStreamReader streamReader, final ObjectId instance) throws SerializationException {
        // already handled in instantiate
    }

    public static ObjectId instantiate(final SerializationStreamReader streamReader) throws SerializationException {
        return new ObjectId(streamReader.readInt(), streamReader.readInt(), streamReader.readInt());
    }

    public static void serialize(final SerializationStreamWriter streamWriter, final ObjectId instance) throws SerializationException {
        streamWriter.writeInt(instance._time);
        streamWriter.writeInt(instance._machine);
        streamWriter.writeInt(instance._inc);
    }
}
