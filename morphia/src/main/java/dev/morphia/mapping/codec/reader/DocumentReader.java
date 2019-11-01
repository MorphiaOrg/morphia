package dev.morphia.mapping.codec.reader;

import dev.morphia.mapping.codec.BsonTypeMap;
import dev.morphia.mapping.codec.reader.Stage.InitialStage;
import dev.morphia.sofia.Sofia;
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
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.List;

import static java.lang.String.format;

/**
 * @morphia.internal
 */
public class DocumentReader implements BsonReader {
    private static final BsonTypeMap TYPE_MAP = new BsonTypeMap();
    private final Stage initial;
    private Stage stage;

    /**
     * Construct a new instance.
     *
     * @param document the document to read from
     */
    public DocumentReader(final Document document) {
        stage = new InitialStage(this, new DocumentIterator(this, document.entrySet().iterator()));
        initial = stage;
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
        return (BsonBinary) stage().value();
    }

    @Override
    public byte peekBinarySubType() {
        return stage().<BsonBinary>value().getType();
    }

    @Override
    public int peekBinarySize() {
        return stage().<BsonBinary>value().getData().length;
    }

    @Override
    public BsonBinary readBinaryData(final String name) {
        verifyName(name);
        return readBinaryData();
    }

    @Override
    public boolean readBoolean() {
        return (boolean) stage().value();
    }

    @Override
    public boolean readBoolean(final String name) {
        verifyName(name);
        return readBoolean();
    }

    @Override
    public BsonType readBsonType() {
        return stage().getCurrentBsonType();
    }

    @Override
    public long readDateTime() {
        return (long) stage().value();
    }

    @Override
    public long readDateTime(final String name) {
        verifyName(name);
        return readDateTime();
    }

    @Override
    public double readDouble() {
        return (double) stage().value();
    }

    @Override
    public double readDouble(final String name) {
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
        return (int) stage().value();
    }

    @Override
    public int readInt32(final String name) {
        verifyName(name);
        return readInt32();
    }

    @Override
    public long readInt64() {
        return (long) stage().value();
    }

    @Override
    public long readInt64(final String name) {
        verifyName(name);
        return readInt64();
    }

    @Override
    public Decimal128 readDecimal128() {
        return (Decimal128) stage().value();
    }

    @Override
    public Decimal128 readDecimal128(final String name) {
        verifyName(name);
        return readDecimal128();
    }

    @Override
    public String readJavaScript() {
        return stage().<BsonJavaScript>value().getCode();
    }

    @Override
    public String readJavaScript(final String name) {
        verifyName(name);
        return readJavaScript();
    }

    @Override
    public String readJavaScriptWithScope() {
        return stage().<BsonJavaScriptWithScope>value().getCode();
    }

    @Override
    public String readJavaScriptWithScope(final String name) {
        verifyName(name);
        return readJavaScriptWithScope();
    }

    @Override
    public void readMaxKey() {
    }

    @Override
    public void readMaxKey(final String name) {
        verifyName(name);
        readMaxKey();
    }

    @Override
    public void readMinKey() {
    }

    @Override
    public void readMinKey(final String name) {
        verifyName(name);
        readMinKey();
    }

    @Override
    public String readName() {
        return stage().name();
    }

    @Override
    public void readName(final String name) {
        verifyName(name);
    }

    @Override
    public void readNull() {
    }

    @Override
    public void readNull(final String name) {
        verifyName(name);
        readNull();
    }

    @Override
    public ObjectId readObjectId() {
        return (ObjectId) stage().value();
    }

    @Override
    public ObjectId readObjectId(final String name) {
        verifyName(name);
        return readObjectId();
    }

    @Override
    public BsonRegularExpression readRegularExpression() {
        return (BsonRegularExpression) stage().value();
    }

    @Override
    public BsonRegularExpression readRegularExpression(final String name) {
        verifyName(name);
        return readRegularExpression();
    }

    @Override
    public BsonDbPointer readDBPointer() {
        return (BsonDbPointer) stage().value();
    }

    @Override
    public BsonDbPointer readDBPointer(final String name) {
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
        return (String) stage().value();
    }

    @Override
    public String readString(final String name) {
        verifyName(name);
        return readString();
    }

    @Override
    public String readSymbol() {
        return (String) stage().value();
    }

    @Override
    public String readSymbol(final String name) {
        verifyName(name);
        return readSymbol();
    }

    @Override
    public BsonTimestamp readTimestamp() {
        return (BsonTimestamp) stage().value();
    }

    @Override
    public BsonTimestamp readTimestamp(final String name) {
        verifyName(name);
        return readTimestamp();
    }

    @Override
    public void readUndefined() {
    }

    @Override
    public void readUndefined(final String name) {
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

    @Deprecated
    @Override
    public void mark() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BsonReaderMark getMark() {
        return new Mark(this, stage());
    }

    @Deprecated
    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }

    protected void verifyName(final String expectedName) {
        readBsonType();
        String actualName = readName();
        if (!actualName.equals(expectedName)) {
            throw new BsonSerializationException(format("Expected element name to be '%s', not '%s'.",
                expectedName, actualName));
        }
    }

    Stage stage() {
        return this.stage;
    }

    Stage nextStage(final Stage nextStage) {
        stage = nextStage;
        return stage;
    }

    void reset(final Stage bookmark) {
        stage = bookmark;
    }

    BsonType getBsonType(final Object o) {
        BsonType bsonType = TYPE_MAP.get(o.getClass());
        if (bsonType == null) {
            if (o instanceof List) {
                bsonType = BsonType.ARRAY;
            } else {
                throw new IllegalStateException(Sofia.unknownBsonType(o.getClass()));
            }
        }
        return bsonType;
    }

}
