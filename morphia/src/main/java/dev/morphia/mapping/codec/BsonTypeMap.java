package dev.morphia.mapping.codec;

import org.bson.BsonDbPointer;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.BsonType;
import org.bson.BsonUndefined;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.Code;
import org.bson.types.CodeWithScope;
import org.bson.types.Decimal128;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.bson.types.ObjectId;
import org.bson.types.Symbol;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Java type to BSON types
 */
public class BsonTypeMap {
    private final Map<Class<?>, BsonType> map = new HashMap<>();

    /**
     * Creates the map
     */
    public BsonTypeMap() {
        map.put(List.class, BsonType.ARRAY);
        map.put(Binary.class, BsonType.BINARY);
        map.put(Boolean.class, BsonType.BOOLEAN);
        map.put(Date.class, BsonType.DATE_TIME);
        map.put(BsonDbPointer.class, BsonType.DB_POINTER);
        map.put(Document.class, BsonType.DOCUMENT);
        map.put(Double.class, BsonType.DOUBLE);
        map.put(Integer.class, BsonType.INT32);
        map.put(Long.class, BsonType.INT64);
        map.put(Decimal128.class, BsonType.DECIMAL128);
        map.put(MaxKey.class, BsonType.MAX_KEY);
        map.put(MinKey.class, BsonType.MIN_KEY);
        map.put(Code.class, BsonType.JAVASCRIPT);
        map.put(CodeWithScope.class, BsonType.JAVASCRIPT_WITH_SCOPE);
        map.put(ObjectId.class, BsonType.OBJECT_ID);
        map.put(BsonRegularExpression.class, BsonType.REGULAR_EXPRESSION);
        map.put(String.class, BsonType.STRING);
        map.put(Symbol.class, BsonType.SYMBOL);
        map.put(BsonTimestamp.class, BsonType.TIMESTAMP);
        map.put(BsonUndefined.class, BsonType.UNDEFINED);
    }

    /**
     * Gets the Class that is mapped to the given BSON type.
     *
     * @param type the BSON type
     * @return the Class that is mapped to the BSON type
     */
    public BsonType get(final Class<?> type) {
        return map.get(type);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BsonTypeMap that = (BsonTypeMap) o;

        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
