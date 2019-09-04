package dev.morphia.mapping.codec;

import dev.morphia.sofia.Sofia;
import org.bson.AbstractBsonReader;
import org.bson.BSONException;
import org.bson.BsonBinary;
import org.bson.BsonContextType;
import org.bson.BsonDbPointer;
import org.bson.BsonReaderMark;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class DocumentReader extends AbstractBsonReader {
    private Object currentValue;
    private final BsonTypeMap typeMap = new BsonTypeMap();
    private Field nameField;

    public DocumentReader(final Document source) {
        this.currentValue = source;
        setContext(new DocumentReader.Context(null, BsonContextType.TOP_LEVEL, source));
    }

    @Override
    protected BsonBinary doReadBinaryData() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte doPeekBinarySubType() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int doPeekBinarySize() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean doReadBoolean() {
        return (boolean) currentValue;
    }

    @Override
    protected long doReadDateTime() {
        return (long) currentValue;
    }

    @Override
    protected double doReadDouble() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doReadEndArray() {
        setContext(getContext().getParentContext());
    }

    @Override
    protected void doReadEndDocument() {
        setContext(getContext().getParentContext());
        switch (getContext().getContextType()) {
            case ARRAY:
            case DOCUMENT:
                setState(State.TYPE);
                break;
            case TOP_LEVEL:
                setState(State.DONE);
                break;
            default:
                throw new BSONException("Unexpected ContextType.");
        }
    }

    @Override
    protected int doReadInt32() {
        return (int) currentValue;
    }

    @Override
    protected long doReadInt64() {
        return (long) currentValue;
    }

    @Override
    protected Decimal128 doReadDecimal128() {
        return (Decimal128) currentValue;
    }

    @Override
    protected String doReadJavaScript() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String doReadJavaScriptWithScope() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doReadMaxKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doReadMinKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doReadNull() {
    }

    @Override
    protected ObjectId doReadObjectId() {
        return (ObjectId) currentValue;
    }

    @Override
    protected BsonRegularExpression doReadRegularExpression() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected BsonDbPointer doReadDBPointer() {
        return (BsonDbPointer) currentValue;
    }

    @Override
    protected void doReadStartArray() {
        setContext(new DocumentReader.Context(getContext(), BsonContextType.ARRAY, (List<Object>) currentValue));
    }

    @Override
    protected void doReadStartDocument() {
        setContext(new DocumentReader.Context(getContext(), BsonContextType.DOCUMENT, (Document) currentValue));
    }

    @Override
    protected String doReadString() {
        return (String) currentValue;
    }

    @Override
    protected String doReadSymbol() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected BsonTimestamp doReadTimestamp() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doReadUndefined() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doSkipName() {
    }

    @Override
    protected void doSkipValue() {
    }

    @Override
    protected DocumentReader.Context getContext() {
        return (Context) super.getContext();
    }

    @Override
    public BsonType readBsonType() {
        if (getState() == State.INITIAL || getState() == State.SCOPE_DOCUMENT) {
            // there is an implied type of Document for the top level and for scope documents
            setCurrentBsonType(BsonType.DOCUMENT);
            setState(State.VALUE);
            return getCurrentBsonType();
        }

        if (getState() != State.TYPE) {
            throwInvalidState("ReadBSONType", State.TYPE);
        }

        switch (getContext().getContextType()) {
            case ARRAY:
                currentValue = getContext().getNextValue();
                if (currentValue == null) {
                    setState(State.END_OF_ARRAY);
                    return BsonType.END_OF_DOCUMENT;
                }
                setState(State.VALUE);
                break;
            case DOCUMENT:
                Entry<String, Object> currentElement = getContext().getNextElement();
                if (currentElement == null) {
                    setState(State.END_OF_DOCUMENT);
                    return BsonType.END_OF_DOCUMENT;
                }
                setCurrentName(currentElement.getKey());
                currentValue = currentElement.getValue();
                setState(State.NAME);
                break;
            default:
                throw new BSONException("Invalid ContextType.");
        }

        setCurrentBsonType(getBsonType(currentValue));
        return getCurrentBsonType();
    }

    private BsonType getBsonType(final Object o) {
        BsonType bsonType = typeMap.get(o.getClass());
        if (bsonType == null) {
            if (o instanceof List) {
                bsonType = BsonType.ARRAY;
            } else {
                throw new IllegalStateException(Sofia.unknownBsonType(o.getClass()));
            }
        }
        return bsonType;
    }

    @Override
    public void mark() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BsonReaderMark getMark() {
        return new DocumentMark(this);
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    String currentName() {
        try {
            if (nameField == null) {
                nameField = AbstractBsonReader.class.getDeclaredField("currentName");
                nameField.setAccessible(true);
            }
            return (String) nameField.get(this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void setState(final State newState) {
        super.setState(newState);
    }

    @Override
    public void setCurrentBsonType(final BsonType newType) {
        super.setCurrentBsonType(newType);
    }

    @Override
    public void setCurrentName(final String newName) {
        super.setCurrentName(newName);
    }

    protected class Context extends AbstractBsonReader.Context {

        private DocumentMarkableIterator<Entry<String, Object>> documentIterator;
        private DocumentMarkableIterator<Object> listIterator;

        protected Context(final DocumentReader.Context parentContext, final BsonContextType contextType, final List<Object> list) {
            super(parentContext, contextType);
            listIterator = new DocumentMarkableIterator<>(list.iterator());
        }

        protected Context(final DocumentReader.Context parentContext, final BsonContextType contextType, final Document document) {
            super(parentContext, contextType);
            documentIterator = new DocumentMarkableIterator<>(document.entrySet().iterator());
        }

        @Override
        public BsonContextType getContextType() {
            return super.getContextType();
        }

        @Override
        public Context getParentContext() {
            return (Context) super.getParentContext();
        }

        public Entry<String, Object> getNextElement() {
            if (documentIterator.hasNext()) {
                return documentIterator.next();
            } else {
                return null;
            }
        }

        protected void mark() {
            if (documentIterator != null) {
                documentIterator.mark();
            } else {
                listIterator.mark();
            }

            if (getParentContext() != null) {
                getParentContext().mark();
            }
        }

        protected void reset() {
            if (documentIterator != null) {
                documentIterator.reset();
            } else {
                listIterator.reset();
            }

            if (getParentContext() != null) {
                getParentContext().reset();
            }
        }

        public Object getNextValue() {
            if (listIterator.hasNext()) {
                return listIterator.next();
            } else {
                return null;
            }
        }
    }

    private static class DocumentMarkableIterator<T> implements Iterator<T> {

        private Iterator<T> baseIterator;
        private List<T> markIterator = new ArrayList<T>();
        private int curIndex; // index of the cursor
        private boolean marking;

        protected DocumentMarkableIterator(final Iterator<T> baseIterator) {
            this.baseIterator = baseIterator;
            curIndex = 0;
            marking = false;
        }

        /**
         *
         */
        protected void mark() {
            marking = true;
        }

        /**
         *
         */
        protected void reset() {
            curIndex = 0;
            marking = false;
        }


        @Override
        public boolean hasNext() {
            return baseIterator.hasNext() || curIndex < markIterator.size();
        }

        @Override
        public T next() {
            T value;
            //TODO: check closed
            if (curIndex < markIterator.size()) {
                value = markIterator.get(curIndex);
                if (marking) {
                    curIndex++;
                } else {
                    markIterator.remove(0);
                }
            } else {
                value = baseIterator.next();
                if (marking) {
                    markIterator.add(value);
                    curIndex++;
                }
            }


            return value;
        }

        @Override
        public void remove() {
            // iterator is read only
        }
    }

    protected static class DocumentMark extends dev.morphia.mapping.codec.Mark {
        private final Object currentValue;
        private final Context context;
        private DocumentReader reader;


        protected DocumentMark(DocumentReader reader) {
            super(reader);
            
            currentValue = reader.currentValue;
            context = reader.getContext();
            this.reader = reader;
            context.mark();
        }

        public void reset() {
            super.reset();
            reader.currentValue = currentValue;
            reader.setContext(context);
            context.reset();
        }
    }
}
