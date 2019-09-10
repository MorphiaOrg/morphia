package dev.morphia.mapping.codec.reader;

import org.bson.BsonReaderMark;

public class Mark implements BsonReaderMark {
    private Context context;
    private final Stage stage;

    public Mark(final Context context, final Stage stage) {
        this.context = context;
        this.stage = stage;
    }

    public void reset() {
        context.reset(stage);
    }
}
