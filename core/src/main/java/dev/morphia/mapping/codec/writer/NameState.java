package dev.morphia.mapping.codec.writer;

import com.mongodb.lang.Nullable;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.of;

class NameState extends WriteState {
    private final String name;
    private final Document document;
    private WriteState value;

    NameState(DocumentWriter writer, String name, Document document) {
        super(writer);
        this.name = name;
        if (!document.containsKey(name)) {
            this.document = document;
            document.put(name, this);
        } else {
            this.document = andTogether(document, name, this);
        }

    }

    @SuppressWarnings("unchecked")
    private Document andTogether(Document doc, String key, @Nullable Object additional) {
        if (additional != null) {
            Document newSubdoc = new Document(key, additional);
            var extant = doc.remove(key);
            List<Document> and = (List<Document>) doc.get("$and");
            if (and != null) {
                and.add(newSubdoc);
            } else {
                and = new ArrayList<>();
                and.addAll(of(new Document(key, extant), newSubdoc));
                doc.put("$and", and);
                return newSubdoc;
            }
        }
        return doc;
    }

    @Override
    public String toString() {
        return value == null
               ? "<<pending>>"
               : value.toString();
    }

    @Override
    protected String state() {
        return "name";
    }

    @Override
    WriteState array() {
        value = new ArrayState(getWriter());
        document.put(name, ((ArrayState) value).getList());
        return value;
    }

    @Override
    WriteState document() {
        if (document.get(name) instanceof Document) {
            value = new DocumentState(getWriter(), (Document) document.get(name));
        } else {
            value = new DocumentState(getWriter());
            document.put(name, ((DocumentState) value).getDocument());
        }
        return value;
    }

    @Override
    DocumentState previous() {
        return super.previous();
    }

    @Override
    void value(Object value) {
        document.put(name, value);
        end();
    }
}
