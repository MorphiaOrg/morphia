package com.google.code.morphia;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Key_CustomFieldSerializer {
	public static void deserialize(SerializationStreamReader streamReader, Key instance) throws SerializationException {
		// already handled in instantiate
	}
	
	public static Key instantiate(SerializationStreamReader streamReader) throws SerializationException {
		Key newKey = new Key();
		newKey.id = (Serializable) streamReader.readObject();
		newKey.kind = streamReader.readString();		
//		newKey.kindClass = (Class) streamReader.readObject();
		return newKey;
	}
	
	public static void serialize(SerializationStreamWriter streamWriter, Key instance) throws SerializationException {
		streamWriter.writeObject(instance.getId());
		streamWriter.writeString(instance.getKind());
//		streamWriter.writeObject(instance.getKindClass());
	}
}
