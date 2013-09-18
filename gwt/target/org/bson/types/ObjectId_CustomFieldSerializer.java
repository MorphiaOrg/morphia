package org.bson.types;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 *
 */
public class ObjectId_CustomFieldSerializer {
    /**
     * @param streamReader
     * @param instance
     * @throws SerializationException
     */
    public static void deserialize(
            SerializationStreamReader streamReader,
            ObjectId instance) throws SerializationException {
        // already handled in instantiate
    }

    /**
     * @param streamReader
     * @return
     * @throws SerializationException
     */
    public static ObjectId instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new ObjectId(streamReader.readInt(),
                streamReader.readInt(), streamReader.readInt());
    }

    /**
     * @param streamWriter
     * @param instance
     * @throws SerializationException
     */
    public static void serialize(SerializationStreamWriter streamWriter,
                                 ObjectId instance)
            throws SerializationException {
        streamWriter.writeInt(instance._time);
        streamWriter.writeInt(instance._machine);
        streamWriter.writeInt(instance._inc);
    }
}
