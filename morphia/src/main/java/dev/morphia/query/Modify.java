package dev.morphia.query;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import dev.morphia.FindAndModifyOptions;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.MappedClass;

public class Modify<R> extends UpdatesImpl<Modify<R>> {
    private final QueryImpl<R> query;
    private final DBCollection collection;
    private final DBObject queryObject;

    Modify(final QueryImpl<R> query) {
        super(query.ds, query.mapper, query.clazz);
        this.query = query;
        this.collection = query.dbColl;
        this.queryObject = query.getQueryObject();
    }

    public R execute() {
        return execute(new FindAndModifyOptions());
    }

    public R execute(final FindAndModifyOptions options) {
        versionUpdate();
        DBObject res = collection.findAndModify(queryObject, options.copy()
                                                                    .sort(query.getSortObject())
                                                                    .projection(query.getFieldsObject())
                                                                    .update(getOps())
                                                                    .getOptions());

        return mapper.fromDBObject(datastore, query.getEntityClass(), res, mapper.createEntityCache());

    }

    private void versionUpdate() {
        final MappedClass mc = mapper.getMappedClass(clazz);

        if (!mc.getFieldsAnnotatedWith(Version.class).isEmpty()) {
            inc(mc.getMappedVersionField().getNameToStore());
        }

    }
}
