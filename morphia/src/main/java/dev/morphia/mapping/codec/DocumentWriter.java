package dev.morphia.mapping.codec;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Utility to write out to a Document
 */
@SuppressWarnings("unchecked")
public class DocumentWriter implements BsonWriter {
    private Stack<Object> state = new Stack<>();
    private Object root;

    /**
     * Creates a new Writer
     */
    public DocumentWriter() {
        push(new RootSlab());
    }

    private void push(final Object o) {
        state.push(o);
    }

    /**
     * Creates a new Writer with a seeded Document
     *
     * @param seed the seed Document
     */
    public DocumentWriter(final Document seed) {
        push(new RootSlab(seed));
    }

    /**
     * @param <T> the root type
     * @return the root, or output, of this writer.  usually a Document.
     */
    public <T> T getRoot() {
        return (T) root;
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
        ((Codec) codecRegistry
                     .get(value.getClass()))
            .encode(this, value, encoderContext);

        return this;
    }

    @Override
    public void flush() {
    }

    @Override
    public void writeBinaryData(final BsonBinary binary) {
        value(binary);
    }

    @Override
    public void writeBinaryData(final String name, final BsonBinary binary) {
        document().put(name, binary);
    }

    @Override
    public void writeBoolean(final boolean value) {
        value(value);
    }

    @Override
    public void writeBoolean(final String name, final boolean value) {
        document().put(name, value);
    }

    @Override
    public void writeDateTime(final long value) {
        value(LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC));
    }

    @Override
    public void writeDateTime(final String name, final long value) {
        document().put(name, LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC));
    }

    @Override
    public void writeDBPointer(final BsonDbPointer value) {
        value(value);
    }

    @Override
    public void writeDBPointer(final String name, final BsonDbPointer value) {
        document().put(name, value);
    }

    @Override
    public void writeDouble(final double value) {
        value(value);
    }

    @Override
    public void writeDouble(final String name, final double value) {
        document().put(name, value);
    }

    @Override
    public void writeEndArray() {
        pop();
    }

    @Override
    public void writeEndDocument() {
        pop();
    }

    @Override
    public void writeInt32(final int value) {
        value(value);
    }

    @Override
    public void writeInt32(final String name, final int value) {
        document().put(name, value);
    }

    @Override
    public void writeInt64(final long value) {
        value(value);
    }

    @Override
    public void writeInt64(final String name, final long value) {
        document().put(name, value);
    }

    @Override
    public void writeDecimal128(final Decimal128 value) {
        value(value);
    }

    @Override
    public void writeDecimal128(final String name, final Decimal128 value) {
        document().put(name, value);
    }

    @Override
    public void writeJavaScript(final String code) {
        value(code);
    }

    @Override
    public void writeJavaScript(final String name, final String code) {
        document().put(name, code);
    }

    @Override
    public void writeJavaScriptWithScope(final String code) {
        value(code);
    }

    @Override
    public void writeJavaScriptWithScope(final String name, final String code) {
        document().put(name, code);
    }

    @Override
    public void writeMaxKey() {
        value(new BsonMaxKey());
    }

    @Override
    public void writeMaxKey(final String name) {
        writeName(name);
        writeMaxKey();
    }

    @Override
    public void writeMinKey() {
        value(new BsonMinKey());
    }

    @Override
    public void writeMinKey(final String name) {
        writeName(name);
        writeMinKey();
    }

    @Override
    public void writeName(final String name) {
        Document document = peek();
        final ValueSlab value = new ValueSlab(name);
        document.put(name, value);
        push(value);
    }

    @Override
    public void writeNull() {
        value(null);
    }

    @Override
    public void writeNull(final String name) {
        writeName(name);
        value(null);
    }

    @Override
    public void writeObjectId(final ObjectId objectId) {
        value(objectId);
    }

    @Override
    public void writeObjectId(final String name, final ObjectId objectId) {
        document().put(name, objectId);
    }

    @Override
    public void writeRegularExpression(final BsonRegularExpression regularExpression) {
        value(regularExpression);
    }

    @Override
    public void writeRegularExpression(final String name, final BsonRegularExpression regularExpression) {
        document().put(name, regularExpression);
    }

    @Override
    public void writeStartArray() {
        final List<Object> list = new ArrayList<>();
        value(list);
        push(new ListSlab(list));
    }

    @Override
    public void writeStartArray(final String name) {
        writeName(name);
        writeStartArray();
    }

    @Override
    public void writeStartDocument() {
        final Document document = new Document();
        value(document);
        push(document);
    }

    @Override
    public void writeStartDocument(final String name) {
        final Document document = new Document();
        value(document);
        push(document);
        writeName(name);
    }

    @Override
    public void writeString(final String value) {
        value(value);
    }

    @Override
    public void writeString(final String name, final String value) {
        document().put(name, value);
    }

    @Override
    public void writeSymbol(final String value) {
        value(new BsonSymbol(value));
    }

    @Override
    public void writeSymbol(final String name, final String value) {
        writeName(name);
        writeSymbol(value);
    }

    @Override
    public void writeTimestamp(final BsonTimestamp value) {
        value(value);
    }

    @Override
    public void writeTimestamp(final String name, final BsonTimestamp value) {
        writeName(name);
        value(value);
    }

    @Override
    public void writeUndefined() {
        value(new BsonUndefined());
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

    private Document document() {
        return (Document) peek();
    }

    private <T> T peek() {
        return (T) state.peek();
    }

    private void value(final Object value) {
        ((ValueSlab) pop()).apply(value);
    }

    private <T> T pop() {
        final Object pop = state.pop();
        try {
            return (T) pop;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Tried to end the wrong state.  The current state is: " + pop.getClass(), e);
        }
    }

    @Override
    public String toString() {
        return root == null ? "<empty>" : root.toString();
    }

    private class ValueSlab {
        private String name;

        private ValueSlab(final String name) {
            this.name = name;
        }

        public void apply(final Object value) {
            document().put(name, value);
        }

        @Override
        public String toString() {
            return "Calculating...";
        }
    }

    private final class RootSlab extends ValueSlab {
        private Document seed;

        private RootSlab() {
            super(null);
        }

        private RootSlab(final Document seed) {
            super(null);
            this.seed = seed;
        }

        @Override
        public void apply(final Object value) {
            if (seed != null) {
                if (value instanceof Document) {
                    ((Document) value).putAll(seed);
                    seed = null;
                }
            }
            root = value;
        }
    }

    private final class ListSlab extends ValueSlab {
        private List<Object> list;

        private ListSlab(final List<Object> list) {
            super(null);
            this.list = list;
        }

        @Override
        public void apply(final Object value) {
            list.add(value);
            push(this);
        }

        @Override
        public String toString() {
            return list.toString();
        }
    }
}
