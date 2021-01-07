package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.BsonTypeMap;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.reader.ReaderState.InitialReaderState;
import org.bson.BsonBinary;
import org.bson.BsonDbPointer;
import org.bson.BsonJavaScript;
import org.bson.BsonJavaScriptWithScope;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonRegularExpression;
import org.bson.BsonSerializationException;
import org.bson.BsonTimestamp;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

/**
 * @morphia.internal
 */
public class DocumentReader implements BsonReader {
    private static final BsonTypeMap TYPE_MAP = new BsonTypeMap();
    private ReaderState readerState;

    /**
     * Construct a new instance.
     *
     * @param document the document to read from
     */
    public DocumentReader(Document document) {
        readerState = new InitialReaderState(this, new DocumentIterator(this, document.entrySet().iterator()));
    }

    @Override
    public BsonType getCurrentBsonType() {
        return stage().getCurrentBsonType();
    }

    @Override
    public String getCurrentName() {
        return stage().name();
    }

    @Override
    public BsonBinary readBinaryData() {
        Object value = stage().value();
        if (value instanceof UUID) {
            return new BsonBinary((UUID) value);
        } else if (value instanceof Binary) {
            return new BsonBinary(((Binary) value).getType(), ((Binary) value).getData());
        } else {
            return (BsonBinary) value;
        }
    }

    @Override
    public byte peekBinarySubType() {
        BsonReaderMark mark = getMark();
        try {
            Object binary = stage().value();
            if (binary instanceof UUID) {
                return (byte) ((UUID) binary).version();
            } else {
                return ((BsonBinary) binary).getType();
            }
        } finally {
            mark.reset();
        }

    }

    @Override
    public int peekBinarySize() {
        return stage().<BsonBinary>value().getData().length;
    }

    @Override
    public BsonBinary readBinaryData(String name) {
        verifyName(name);
        return readBinaryData();
    }

    @Override
    public boolean readBoolean() {
        return stage().value();
    }

    @Override
    public boolean readBoolean(String name) {
        verifyName(name);
        return readBoolean();
    }

    @Override
    public BsonType readBsonType() {
        return stage().getCurrentBsonType();
    }

    @Override
    public long readDateTime() {
        return Conversions.convert(stage().value(), long.class);
    }

    @Override
    public long readDateTime(String name) {
        verifyName(name);
        return readDateTime();
    }

    @Override
    public double readDouble() {
        return stage().value();
    }

    @Override
    public double readDouble(String name) {
        verifyName(name);
        return readDouble();
    }

    @Override
    public void readEndArray() {
        stage().endArray();
    }

    @Override
    public void readEndDocument() {
        stage().endDocument();
    }

    @Override
    public int readInt32() {
        return stage().value();
    }

    @Override
    public int readInt32(String name) {
        verifyName(name);
        return readInt32();
    }

    @Override
    public long readInt64() {
        return stage().value();
    }

    @Override
    public long readInt64(String name) {
        verifyName(name);
        return readInt64();
    }

    @Override
    public Decimal128 readDecimal128() {
        return stage().value();
    }

    @Override
    public Decimal128 readDecimal128(String name) {
        verifyName(name);
        return readDecimal128();
    }

    @Override
    public String readJavaScript() {
        return stage().<BsonJavaScript>value().getCode();
    }

    @Override
    public String readJavaScript(String name) {
        verifyName(name);
        return readJavaScript();
    }

    @Override
    public String readJavaScriptWithScope() {
        return stage().<BsonJavaScriptWithScope>value().getCode();
    }

    @Override
    public String readJavaScriptWithScope(String name) {
        verifyName(name);
        return readJavaScriptWithScope();
    }

    @Override
    public void readMaxKey() {
    }

    @Override
    public void readMaxKey(String name) {
        verifyName(name);
        readMaxKey();
    }

    @Override
    public void readMinKey() {
    }

    @Override
    public void readMinKey(String name) {
        verifyName(name);
        readMinKey();
    }

    @Override
    public String readName() {
        return stage().name();
    }

    @Override
    public void readName(String name) {
        verifyName(name);
    }

    @Override
    public void readNull() {
    }

    @Override
    public void readNull(String name) {
        verifyName(name);
        readNull();
    }

    @Override
    public ObjectId readObjectId() {
        return stage().value();
    }

    @Override
    public ObjectId readObjectId(String name) {
        verifyName(name);
        return readObjectId();
    }

    @Override
    public BsonRegularExpression readRegularExpression() {
        return stage().value();
    }

    @Override
    public BsonRegularExpression readRegularExpression(String name) {
        verifyName(name);
        return readRegularExpression();
    }

    @Override
    public BsonDbPointer readDBPointer() {
        return stage().value();
    }

    @Override
    public BsonDbPointer readDBPointer(String name) {
        verifyName(name);
        return readDBPointer();
    }

    @Override
    public void readStartArray() {
        stage().startArray();
    }

    @Override
    public void readStartDocument() {
        stage().startDocument();
    }

    @Override
    public String readString() {
        return stage().value();
    }

    @Override
    public String readString(String name) {
        verifyName(name);
        return readString();
    }

    @Override
    public String readSymbol() {
        return stage().value();
    }

    @Override
    public String readSymbol(String name) {
        verifyName(name);
        return readSymbol();
    }

    @Override
    public BsonTimestamp readTimestamp() {
        return stage().value();
    }

    @Override
    public BsonTimestamp readTimestamp(String name) {
        verifyName(name);
        return readTimestamp();
    }

    @Override
    public void readUndefined() {
    }

    @Override
    public void readUndefined(String name) {
        verifyName(name);
        readUndefined();
    }

    @Override
    public void skipName() {
    }

    @Override
    public void skipValue() {
        stage().advance();
    }

    @Override
    public BsonReaderMark getMark() {
        return new Mark(this, stage());
    }

    @Override
    public void close() {
    }

    protected void verifyName(String expectedName) {
        String actualName = readName();
        if (!actualName.equals(expectedName)) {
            throw new BsonSerializationException(format("Expected element name to be '%s', not '%s'.",
                expectedName, actualName));
        }
    }

    ReaderState stage() {
        return this.readerState;
    }

    ReaderState nextStage(ReaderState nextReaderState) {
        readerState = nextReaderState;
        return readerState;
    }

    void reset(ReaderState bookmark) {
        readerState = bookmark;
    }

    BsonType getBsonType(Object o) {
        BsonType bsonType = TYPE_MAP.get(o.getClass());
        if (bsonType == null) {
            if (o instanceof List) {
                bsonType = BsonType.ARRAY;
            } else {
                bsonType = BsonType.UNDEFINED;
            }
        }
        return bsonType;
    }

}
