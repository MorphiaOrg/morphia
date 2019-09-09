package dev.morphia.mapping.codec.reader;

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

import static java.lang.String.format;

/**
 * @morphia.internal
 */
public class FlattenedDocumentReader implements BsonReader {
    private final Context context;

    /**
     * Construct a new instance.
     *
     * @param document the document to read from
     */
    public FlattenedDocumentReader(final Document document) {
        context = new Context(this, document);
    }

    @Override
    public void close() {
    }

    Context getContext() {
        return context;
    }

    void setContext(final Context context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BsonBinary readBinaryData() {
        return (BsonBinary) getContext().getCurrentValue();
    }

    @Override
    public byte peekBinarySubType() {
        BsonBinary currentValue = (BsonBinary) getContext().getCurrentValue();
        return currentValue.getType();
    }

    @Override
    public int peekBinarySize() {
        BsonBinary currentValue = (BsonBinary) getContext().getCurrentValue();
        return currentValue.getData().length;
    }

    @Override
    public boolean readBoolean() {
        return (boolean) getContext().getCurrentValue();
    }

    @Override
    public long readDateTime() {
        return (long) getContext().getCurrentValue();
    }

    @Override
    public double readDouble() {
        return (double) getContext().getCurrentValue();
    }

    @Override
    public void readEndArray() {
        getContext().endArray();
    }

    @Override
    public void readEndDocument() {
        getContext().endDocument();
    }

    @Override
    public int readInt32() {
        return (int) getContext().getCurrentValue();
    }

    @Override
    public long readInt64() {
        return (long) getContext().getCurrentValue();
    }

    @Override
    public Decimal128 readDecimal128() {
        return (Decimal128) getContext().getCurrentValue();
    }

    @Override
    public String readJavaScript() {
        BsonJavaScript currentValue = (BsonJavaScript) getContext().getCurrentValue();
        return currentValue.getCode();
    }

    @Override
    public String readJavaScriptWithScope() {
        BsonJavaScriptWithScope currentValue = (BsonJavaScriptWithScope) getContext().getCurrentValue();
        return currentValue.getCode();
    }

    @Override
    public void readMaxKey() {
    }

    @Override
    public void readMinKey() {
    }

    @Override
    public void readNull() {
    }

    @Override
    public ObjectId readObjectId() {
        return (ObjectId) getContext().getCurrentValue();
    }

    @Override
    public BsonRegularExpression readRegularExpression() {
        return (BsonRegularExpression) getContext().getCurrentValue();
    }

    @Override
    public BsonDbPointer readDBPointer() {
        return (BsonDbPointer) getContext().getCurrentValue();
    }

    @Override
    public void readStartArray() {
        getContext().startArray();
    }

    @Override
    public void readStartDocument() {
        getContext().startDocument();
    }

    @Override
    public String readString() {
        return (String) getContext().getCurrentValue();
    }

    @Override
    public String readSymbol() {
        return (String) getContext().getCurrentValue();
    }

    @Override
    public BsonTimestamp readTimestamp() {
        return (BsonTimestamp) getContext().getCurrentValue();
    }

    @Override
    public void readUndefined() {
    }

    @Override
    public void skipName() {
    }

    @Override
    public void skipValue() {
    }

    @Override
    public BsonType getCurrentBsonType() {
        return getContext().getCurrentBsonType();
    }

    @Override
    public String getCurrentName() {
        return getContext().getCurrentName();
    }

    @Override
    public BsonType readBsonType() {
        return getContext().stage().advance().getCurrentBsonType();
   }

    @Deprecated
    @Override
    public void mark() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BsonReaderMark getMark() {
        return getContext().mark();
    }

    @Deprecated
    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BsonBinary readBinaryData(final String name) {
        verifyName(name);
        return readBinaryData();
    }

    @Override
    public boolean readBoolean(final String name) {
        verifyName(name);
        return readBoolean();
    }

    @Override
    public long readDateTime(final String name) {
        verifyName(name);
        return readDateTime();
    }

    @Override
    public double readDouble(final String name) {
        verifyName(name);
        return readDouble();
    }

    @Override
    public int readInt32(final String name) {
        verifyName(name);
        return readInt32();
    }

    @Override
    public long readInt64(final String name) {
        verifyName(name);
        return readInt64();
    }

    @Override
    public Decimal128 readDecimal128(final String name) {
        verifyName(name);
        return readDecimal128();
    }

    @Override
    public String readJavaScript(final String name) {
        verifyName(name);
        return readJavaScript();
    }

    @Override
    public String readJavaScriptWithScope(final String name) {
        verifyName(name);
        return readJavaScriptWithScope();
    }

    @Override
    public void readMaxKey(final String name) {
        verifyName(name);
        readMaxKey();
    }

    @Override
    public void readMinKey(final String name) {
        verifyName(name);
        readMinKey();
    }

    @Override
    public String readName() {
        return getContext().stage().name();
    }

    @Override
    public void readName(final String name) {
        verifyName(name);
    }

    @Override
    public void readNull(final String name) {
        verifyName(name);
        readNull();
    }

    @Override
    public ObjectId readObjectId(final String name) {
        verifyName(name);
        return readObjectId();
    }

    @Override
    public BsonRegularExpression readRegularExpression(final String name) {
        verifyName(name);
        return readRegularExpression();
    }

    @Override
    public BsonDbPointer readDBPointer(final String name) {
        verifyName(name);
        return readDBPointer();
    }


    @Override
    public String readString(final String name) {
        verifyName(name);
        return readString();
    }

    @Override
    public String readSymbol(final String name) {
        verifyName(name);
        return readSymbol();
    }

    @Override
    public BsonTimestamp readTimestamp(final String name) {
        verifyName(name);
        return readTimestamp();
    }

    @Override
    public void readUndefined(final String name) {
        verifyName(name);
        readUndefined();
    }

    protected void verifyName(final String expectedName) {
        readBsonType();
        String actualName = readName();
        if (!actualName.equals(expectedName)) {
            throw new BsonSerializationException(format("Expected element name to be '%s', not '%s'.",
                expectedName, actualName));
        }
    }

}
