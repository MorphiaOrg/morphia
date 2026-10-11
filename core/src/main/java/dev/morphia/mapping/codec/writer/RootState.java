package dev.morphia.mapping.codec.writer;

import com.mongodb.lang.Nullable;

import org.bson.Document;

class RootState extends WriteState {

    @Nullable
    private Document seed;
    private DocumentState documentState;

    RootState(DocumentWriter writer) {
        super(writer, null);
    }

    RootState(DocumentWriter writer, @Nullable Document seed) {
        super(writer, null);
        this.seed = seed;
    }

    public Document getDocument() {
        Document document = seed != null ? new Document(seed) : new Document();
        documentState.value().forEach((key, value) -> DocumentState.merge(document, key, value));

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
