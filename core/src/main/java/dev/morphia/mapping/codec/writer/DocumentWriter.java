package dev.morphia.mapping.codec.writer;

import java.time.Instant;
import java.time.LocalDateTime;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;

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

/**
 * Utility to write out to a Document
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("unchecked")
public class DocumentWriter implements BsonWriter {
    private final RootState root;
    private final MorphiaConfig config;
    private WriteState state;

    /**
     * Creates a new Writer
     *
     * @param config
     */
    public DocumentWriter(MorphiaConfig config) {
        this.config = config;
        root = new RootState(this);
        state = root;
    }

    /**
     * Creates a new Writer with a seeded Document
     *
     * @param config
     * @param seed   the seed Document
     */
    public DocumentWriter(MorphiaConfig config, @Nullable Document seed) {
        root = new RootState(this, seed);
        this.config = config;
        state = root;
    }

    /**
     * Encodes a value in to this Writer
     *
     * @param codecRegistry  the registry to use
     * @param value          the value to encode
     * @param encoderContext the context
     * @return this
     */
    public DocumentWriter encode(CodecRegistry codecRegistry, Object value, EncoderContext encoderContext) {
        ((Codec) codecRegistry.get(value.getClass()))
                .encode(this, value, encoderContext);

        return this;
    }

    /**
     * Encodes a value.
     *
     * @param value         the value to encode
     * @param mapper
     * @param codecRegistry
     * @return the encoded Document
     */
    public static Document encode(Object value, Mapper mapper, CodecRegistry codecRegistry) {
        return encode(value, mapper, codecRegistry, EncoderContext.builder().build());
    }

    /**
     * Encodes this writer's structuring using the given codec
     *
     * @param value          the value to encode
     * @param mapper
     * @param codecRegistry
     * @param encoderContext the encoder context
     * @return the encoded Document
     */
    public static Document encode(Object value, Mapper mapper, CodecRegistry codecRegistry, EncoderContext encoderContext) {
        DocumentWriter writer = new DocumentWriter(mapper.getConfig());
        Codec codec = codecRegistry.get(value.getClass());
        codec.encode(writer, value, encoderContext);
        return writer.getDocument();
    }

    @Override
    public void flush() {
    }

    /**
     * @return the root, or output, of this writer. usually a Document.
     */
    public Document getDocument() {
        return root.getDocument();
    }

    /**
     * @return the state
     */
    public WriteState state() {
        return state;
    }

    @Override
    public void writeBinaryData(BsonBinary binary) {
        state.value(binary);
    }

    @Override
    public void writeBinaryData(String name, BsonBinary binary) {
        state.name(name).value(binary);
    }

    @Override
    public void writeBoolean(boolean value) {
        state.value(value);
    }

    @Override
    public void writeBoolean(String name, boolean value) {
        state.name(name).value(value);
    }

    @Override
    public void writeDateTime(long value) {
        state.value(LocalDateTime.ofInstant(Instant.ofEpochMilli(value), config.dateStorage().getZone()));
    }

    @Override
    public void writeDateTime(String name, long value) {
        state.name(name);
        writeDateTime(value);
    }

    @Override
    public void writeDBPointer(BsonDbPointer value) {
        state.value(value);
    }

    @Override
    public void writeDBPointer(String name, BsonDbPointer value) {
        state.name(name).value(value);
    }

    @Override
    public void writeDouble(double value) {
        state.value(value);
    }

    @Override
    public void writeDouble(String name, double value) {
        state.name(name).value(value);
    }

    @Override
    public void writeEndArray() {
        state.end();
    }

    @Override
    public void writeEndDocument() {
        state.end();
    }

    @Override
    public void writeInt32(int value) {
        state.value(value);
    }

    @Override
    public void writeInt32(String name, int value) {
        state.name(name).value(value);
    }

    @Override
    public void writeInt64(long value) {
        state.value(value);
    }

    @Override
    public void writeInt64(String name, long value) {
        state.name(name).value(value);
    }

    @Override
    public void writeDecimal128(Decimal128 value) {
        state.value(value);
    }

    @Override
    public void writeDecimal128(String name, Decimal128 value) {
        state.name(name).value(value);
    }

    @Override
    public void writeJavaScript(String code) {
        state.value(code);
    }

    @Override
    public void writeMaxKey(String name) {
        writeName(name);
        writeMaxKey();
    }

    @Override
    public void writeJavaScript(String name, String code) {
        state.name(name).value(code);
    }

    @Override
    public void writeMinKey(String name) {
        writeName(name);
        writeMinKey();
    }

    @Override
    public void writeJavaScriptWithScope(String code) {
        state.value(code);
    }

    @Override
    public void writeJavaScriptWithScope(String name, String code) {
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
    public void writeName(String name) {
        state.name(name);
    }

    @Override
    public void writeNull() {
        state.value(null);
    }

    @Override
    public void writeNull(String name) {
        writeName(name);
        state.value(null);
    }

    @Override
    public void writeObjectId(ObjectId objectId) {
        state.value(objectId);
    }

    @Override
    public void writeStartArray(String name) {
        writeName(name);
        writeStartArray();
    }

    @Override
    public void writeObjectId(String name, ObjectId objectId) {
        state.name(name).value(objectId);
    }

    @Override
    public void writeRegularExpression(BsonRegularExpression regularExpression) {
        state.value(regularExpression);
    }

    @Override
    public void writeRegularExpression(String name, BsonRegularExpression regularExpression) {
        state.name(name).value(regularExpression);
    }

    @Override
    public void writeStartArray() {
        state.array();
    }

    @Override
    public void writeStartDocument() {
        state.document();
    }

    @Override
    public void writeSymbol(String name, String value) {
        writeName(name);
        writeSymbol(value);
    }

    @Override
    public void writeStartDocument(String name) {
        state.name(name).document();
    }

    @Override
    public void writeString(String value) {
        state.value(value);
    }

    @Override
    public void writeString(String name, String value) {
        state.name(name).value(value);
    }

    @Override
    public void writeUndefined(String name) {
        writeName(name);
        writeUndefined();
    }

    @Override
    public void pipe(BsonReader reader) {
        throw new UnsupportedOperationException("org.bson.io.TestingDocumentWriter.pipe has not yet been implemented.");
    }

    @Override
    public void writeSymbol(String value) {
        state.value(new BsonSymbol(value));
    }

    @Override
    public void writeTimestamp(BsonTimestamp value) {
        state.value(value);
    }

    @Override
    public void writeTimestamp(String name, BsonTimestamp value) {
        writeName(name);
        state.value(value);
    }

    @Override
    public void writeUndefined() {
        state.value(new BsonUndefined());
    }

    @Override
    public String toString() {
        return root.toString();
    }

    void state(WriteState state) {
        this.state = state;
    }

}
