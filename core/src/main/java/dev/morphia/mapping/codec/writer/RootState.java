package dev.morphia.mapping.codec.writer;

import dev.morphia.sofia.Sofia;
import org.bson.Document;

class RootState extends WriteState {

    private final Document document;
    private DocumentState documentState;

    RootState(DocumentWriter writer) {
        super(writer);
        document = null;
    }

    RootState(DocumentWriter writer, Document seed) {
        super(writer);
        document = seed;
    }

    //    @Override
    //    void apply(Object value) {
    //    }

    public Document getDocument() {
        return documentState.getDocument();
    }

    @Override
    public String toString() {
        return documentState == null
               ? "<<undefined>>"
               : documentState.toString();
    }

    @Override
    protected String state() {
        return "root";
    }

    @Override
    WriteState document() {
        documentState = new DocumentState(getWriter(), document);
        return documentState;
    }

    @Override
    WriteState previous() {
        throw new IllegalStateException(Sofia.alreadyAtRoot());
    }
}
