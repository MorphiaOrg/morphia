package dev.morphia.mapping.codec.writer;

import org.bson.Document;

class RootState extends WriteState {

    private Document seed;
    private DocumentState documentState;

    RootState(DocumentWriter writer) {
        super(writer, null);
    }

    RootState(DocumentWriter writer, Document seed) {
        super(writer, null);
        this.seed = seed;
    }

    public Document getDocument() {
        Document document = new Document();
        if (seed != null) {
            document.putAll(seed);
        }
        document.putAll(documentState.value());

        return document;
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
        documentState = new DocumentState(getWriter(), this);
        return documentState;
    }

    @Override
    void done() {
    }
}
