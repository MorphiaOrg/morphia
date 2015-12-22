package org.mongodb.morphia.mapping;


import org.bson.types.Binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public final class Serializer {
    private Serializer() {
    }

    /**
     * serializes object to byte[]
     *
     * @param o   the object to serialize
     * @param zip true if the data should be compressed
     * @return the serialized bytes
     * @throws IOException thrown when an error is encountered writing the data
     */
    public static byte[] serialize(final Object o, final boolean zip) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = baos;
        ObjectOutputStream oos = null;
        try {
            if (zip) {
                os = new GZIPOutputStream(os);
            }
            oos = new ObjectOutputStream(os);
            oos.writeObject(o);
            oos.flush();
        } finally {
            if (oos != null) {
                oos.close();
            }
            os.close();
        }

        return baos.toByteArray();
    }

    /**
     * deserializes DBBinary/byte[] to object
     *
     * @param data   the data to read
     * @param zipped true if the data is compressed
     * @return the deserialized object
     * @throws IOException            thrown when an error is encountered reading the data
     * @throws ClassNotFoundException thrown if the Class definition can not be found
     */
    public static Object deserialize(final Object data, final boolean zipped) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream bais;
        if (data instanceof Binary) {
            bais = new ByteArrayInputStream(((Binary) data).getData());
        } else {
            bais = new ByteArrayInputStream((byte[]) data);
        }

        InputStream is = bais;
        try {
            if (zipped) {
                is = new GZIPInputStream(is);
            }

            final ObjectInputStream ois = new ObjectInputStream(is);
            return ois.readObject();
        } finally {
            is.close();
        }
    }

}
