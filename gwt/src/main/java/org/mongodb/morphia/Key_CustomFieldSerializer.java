package org.mongodb.morphia;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.io.Serializable;

@SuppressWarnings({"rawtypes", "unchecked"})
//CHECKSTYLE:OFF
public final class Key_CustomFieldSerializer {
    //CHECKSTYLE:ON
    private Key_CustomFieldSerializer() {
    }

    public static void deserialize(final SerializationStreamReader streamReader, final Key instance)
        throws SerializationException {
        // already handled in instantiate
    }

    public static Key instantiate(final SerializationStreamReader streamReader)
        throws SerializationException {
        Key newKey = new Key();
        newKey.id = (Serializable) streamReader.readObject();
        newKey.kind = streamReader.readString();
        // newKey.kindClass = (Class) streamReader.readObject();
        return newKey;
    }

    public static void serialize(final SerializationStreamWriter streamWriter, final Key instance)
        throws SerializationException {
        streamWriter.writeObject(instance.getId());
        streamWriter.writeString(instance.getKind());
        // streamWriter.writeObject(instance.getKindClass());
    }
}