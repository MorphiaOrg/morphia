package dev.morphia.query;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.query.updates.UpdateOperators.inc;

/**
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class Operations {
    private final MorphiaDatastore datastore;

    private final boolean validate;

    private final EntityModel model;
    private final List<UpdateOperator> updates = new ArrayList<>();

    /**
     * @param model    the entity model
     * @param updates  the updates
     * @param validate validate or not
     */
    public Operations(MorphiaDatastore datastore, @Nullable EntityModel model, List<UpdateOperator> updates, boolean validate) {
        this.datastore = datastore;
        this.validate = validate;
        this.model = model;
        updates.forEach(this::add);
    }

    public void add(UpdateOperator update) {
        update.validate(validate);
        update.model(model);
        update.datastore(datastore);
        updates.add(update);
    }

    private void versionUpdate(@Nullable EntityModel entityModel) {
        if (entityModel != null) {
            PropertyModel versionField = entityModel.getVersionProperty();
            if (versionField != null) {
                String version = versionField.getMappedName();
                boolean already = updates.stream()
                        .filter(f -> f.operator().equals("$inc"))
                        .anyMatch(f -> f.field().equals(version));
                if (!already) {
                    add(inc(version));
                }
            }
        }
    }

    public Document toDocument(MorphiaDatastore datastore) {
        var document = new Document();
        versionUpdate(model);
        for (UpdateOperator update : updates) {
            Document encoded = encode(datastore, update);
            encoded.forEach((key, value) -> {
                if (!document.containsKey(key)) {
                    document.putAll(encoded);
                } else {
                    Document o = (Document) document.get(key);
                    o.putAll((Document) encoded.get(key));
                }
            });
        }

        return document;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Document encode(MorphiaDatastore datastore, UpdateOperator update) {
        var codecRegistry = datastore.getCodecRegistry();
        var writer = new DocumentWriter(datastore.getMapper().getConfig());
        ((Codec) codecRegistry.get(update.getClass()))
                .encode(writer, update, EncoderContext.builder().build());
        return writer.getDocument();
    }

}
