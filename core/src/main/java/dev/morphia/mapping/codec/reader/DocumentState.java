package dev.morphia.mapping.codec.reader;

import org.bson.BsonType;
import org.bson.Document;

import java.util.List;

import static java.util.stream.Collectors.toList;

class DocumentState extends ReaderState {

    public static final String NAME = "DOCUMENT";
    private final Document document;
    private DocumentEndState endState;

    DocumentState(DocumentReader reader, Document document) {
        super(reader);
        this.document = document;
    }

    @Override
    String getStateName() {
        return NAME;
    }

    @Override
    BsonType getCurrentBsonType() {
        return BsonType.DOCUMENT;
    }

    @Override
    void startDocument() {
        if (endState == null) {
            List<ReaderState> states = document.entrySet().stream()
                                               .flatMap(e -> {
                                                   List<ReaderState> nameStates =
                                                       List.of(new NameState(reader, e.getKey()), valueState(e.getValue()));
                                                   return nameStates.stream();
                                               })
                                               .collect(toList());
            ReaderState docState = null;
            for (ReaderState state : states) {
                if (docState != null) {
                    docState.next(state);
                } else {
                    next(state);
                }
                docState = state;
            }

            endState = new DocumentEndState(reader);
            if (docState != null) {
                docState.next(endState);
            } else {
                next(endState);
            }
        }
        advance();
    }

    @Override
    void skipValue() {
        reader.state(endState != null ? endState.nextState : nextState);
    }
}
