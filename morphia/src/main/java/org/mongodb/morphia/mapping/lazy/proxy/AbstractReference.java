package org.mongodb.morphia.mapping.lazy.proxy;


import com.thoughtworks.proxy.kit.ObjectReference;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.lazy.DatastoreProvider;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings({"rawtypes"})
public abstract class AbstractReference implements Serializable, ObjectReference, ProxiedReference {

    private static final long serialVersionUID = 1L;
    //CHECKSTYLE:OFF
    protected final DatastoreProvider p;
    protected final boolean ignoreMissing;
    protected Object object;
    protected final Class referenceObjClass;
    //CHECKSTYLE:ON
    private boolean isFetched;

    protected AbstractReference(final DatastoreProvider p, final Class referenceObjClass, final boolean ignoreMissing) {
        this.p = p;
        this.referenceObjClass = referenceObjClass;
        this.ignoreMissing = ignoreMissing;
    }

    public final synchronized Object get() {
        if (isFetched) {
            return object;
        }

        object = fetch();
        isFetched = true;
        return object;
    }

    protected abstract Object fetch();

    public final void set(final Object arg0) {
        throw new UnsupportedOperationException();
    }

    //CHECKSTYLE:OFF
    public final boolean __isFetched() {
    //CHECKSTYLE:ON
        return isFetched;
    }

    @SuppressWarnings("unchecked")
    protected final Object fetch(final Key<?> id) {
        return p.get().getByKey(referenceObjClass, id);
    }


    private void writeObject(final ObjectOutputStream out) throws IOException {
        // excessive hoop-jumping in order not to have to recreate the
        // instance.
        // as soon as weÂ´d have an ObjectFactory, that would be unnecessary
        beforeWriteObject();
        isFetched = false;
        out.defaultWriteObject();
    }

    protected void beforeWriteObject() {
    }

    //CHECKSTYLE:OFF
    public final Class __getReferenceObjClass() {
    //CHECKSTYLE:ON
        return referenceObjClass;
    }

    //CHECKSTYLE:OFF
    public Object __unwrap() {
    //CHECKSTYLE:ON
        return get();
    }
}
