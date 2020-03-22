package dev.morphia.mapping.codec;

import dev.morphia.sofia.Sofia;
import org.bson.BsonBinary;
import org.bson.BsonDbPointer;
import org.bson.BsonMaxKey;
import org.bson.BsonMinKey;
import org.bson.BsonReader;
import org.bson.BsonRegularExpression;
import org.bson.BsonSymbol;
import org.bson.BsonTimestamp;
import org.bson.BsonUndefined;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Utility to write out to a Document
 *
 * @morphia.internal
 */
@SuppressWarnings("unchecked")
public class DocumentWriter implements BsonWriter {
    private WriteState state;
    private int arraysLevel;
    private int docsLevel;

    /**
     * Creates a new Writer
     */
    public DocumentWriter() {
        state = new RootState(this);
    }

    /**
     * Creates a new Writer with a seeded Document
     *
     * @param seed the seed Document
     */
    public DocumentWriter(final Document seed) {
        state = new RootState(this, seed);
    }

    /**
     * Encodes a value in to this Writer
     *
     * @param codecRegistry  the registry to use
     * @param value          the value to encode
     * @param encoderContext the context
     * @return this
     */
    public DocumentWriter encode(final CodecRegistry codecRegistry, final Object value, final EncoderContext encoderContext) {
        ((Codec) codecRegistry.get(value.getClass()))
            .encode(this, value, encoderContext);

        return this;
    }

    @Override
    public void flush() {
    }

    /**
     * @return a number
     * @morphia.internal
     */
    public int getArraysLevel() {
        return arraysLevel;
    }

    /**
     * @return a number
     * @morphia.internal
     */
    public int getDocsLevel() {
        return docsLevel;
    }

    /**
     * @return the root, or output, of this writer.  usually a Document.
     */
    public Document getDocument() {
        if (arraysLevel != 0 || docsLevel != 0) {
            throw new IllegalStateException(Sofia.unbalancedOpens(arraysLevel, docsLevel, state));
        }
        return ((DocumentState) state).getDocument();
    }

    @Override
    public String toString() {
        return state.toString();
    }

    @Override
    public void writeBinaryData(final BsonBinary binary) {
        state.value(binary);
    }

    @Override
    public void writeBinaryData(final String name, final BsonBinary binary) {
        state.name(name).value(binary);
    }

    @Override
    public void writeBoolean(final boolean value) {
        state.value(value);
    }

    @Override
    public void writeBoolean(final String name, final boolean value) {
        state.name(name).value(value);
    }

    @Override
    public void writeDateTime(final long value) {
        state.value(LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC));
    }

    @Override
    public void writeDateTime(final String name, final long value) {
        state.name(name).value(LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC));
    }

    @Override
    public void writeDBPointer(final BsonDbPointer value) {
        state.value(value);
    }

    @Override
    public void writeDBPointer(final String name, final BsonDbPointer value) {
        state.name(name).value(value);
    }

    @Override
    public void writeDouble(final double value) {
        state.value(value);
    }

    @Override
    public void writeDouble(final String name, final double value) {
        state.name(name).value(value);
    }

    @Override
    public void writeEndArray() {
        arraysLevel--;
        state.end();
    }

    @Override
    public void writeEndDocument() {
        docsLevel--;
        state.end();
    }

    @Override
    public void writeInt32(final int value) {
        state.value(value);
    }

    @Override
    public void writeInt32(final String name, final int value) {
        state.name(name).value(value);
    }

    @Override
    public void writeInt64(final long value) {
        state.value(value);
    }

    @Override
    public void writeInt64(final String name, final long value) {
        state.name(name).value(value);
    }

    @Override
    public void writeDecimal128(final Decimal128 value) {
        state.value(value);
    }

    @Override
    public void writeDecimal128(final String name, final Decimal128 value) {
        state.name(name).value(value);
    }

    @Override
    public void writeJavaScript(final String code) {
        state.value(code);
    }

    @Override
    public void writeMaxKey(final String name) {
        writeName(name);
        writeMaxKey();
    }

    @Override
    public void writeJavaScript(final String name, final String code) {
        state.name(name).value(code);
    }

    @Override
    public void writeMinKey(final String name) {
        writeName(name);
        writeMinKey();
    }

    @Override
    public void writeJavaScriptWithScope(final String code) {
        state.value(code);
    }

    @Override
    public void writeJavaScriptWithScope(final String name, final String code) {
        state.name(name).value(code);
    }

    @Override
    public void writeMaxKey() {
        state.value(new BsonMaxKey());
    }

    @Override
    public void writeMinKey() {
        state.value(new BsonMinKey());
    }

    @Override
    public void writeName(final String name) {
        state.name(name);
    }

    @Override
    public void writeNull() {
        state.value(null);
    }

    @Override
    public void writeNull(final String name) {
        writeName(name);
        state.value(null);
    }

    @Override
    public void writeObjectId(final ObjectId objectId) {
        state.value(objectId);
    }

    @Override
    public void writeStartArray(final String name) {
        writeName(name);
        writeStartArray();
    }

    @Override
    public void writeObjectId(final String name, final ObjectId objectId) {
        state.name(name).value(objectId);
    }

    @Override
    public void writeRegularExpression(final BsonRegularExpression regularExpression) {
        state.value(regularExpression);
    }

    @Override
    public void writeRegularExpression(final String name, final BsonRegularExpression regularExpression) {
        state.name(name).value(regularExpression);
    }

    @Override
    public void writeStartArray() {
        arraysLevel++;
        state.array();
    }

    @Override
    public void writeStartDocument() {
        docsLevel++;
        state.document();
    }

    @Override
    public void writeSymbol(final String name, final String value) {
        writeName(name);
        writeSymbol(value);
    }

    @Override
    public void writeStartDocument(final String name) {
        state.name(name).document();
        docsLevel++;
    }

    @Override
    public void writeString(final String value) {
        state.value(value);
    }

    @Override
    public void writeString(final String name, final String value) {
        state.name(name).value(value);
    }

    @Override
    public void writeUndefined(final String name) {
        writeName(name);
        writeUndefined();
    }

    @Override
    public void pipe(final BsonReader reader) {
        throw new UnsupportedOperationException("org.bson.io.TestingDocumentWriter.pipe has not yet been implemented.");
    }

    @Override
    public void writeSymbol(final String value) {
        state.value(new BsonSymbol(value));
    }

    @Override
    public void writeTimestamp(final BsonTimestamp value) {
        state.value(value);
    }

    @Override
    public void writeTimestamp(final String name, final BsonTimestamp value) {
        writeName(name);
        state.value(value);
    }

    @Override
    public void writeUndefined() {
        state.value(new BsonUndefined());
    }

    WriteState state(final WriteState state) {
        final WriteState previous = this.state;
        this.state = state;
        return previous;
    }

}
