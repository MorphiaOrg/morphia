package dev.morphia.mapping.lazy;


import com.thoughtworks.proxy.ProxyFactory;
import com.thoughtworks.proxy.kit.ObjectReference;
import com.thoughtworks.proxy.toys.delegate.DelegationMode;
import com.thoughtworks.proxy.toys.hotswap.HotSwappingInvoker;

import dev.morphia.mapping.lazy.proxy.EntityObjectReference;
import dev.morphia.annotations.IdGetter;

import java.lang.reflect.Method;


class NonFinalizingHotSwappingInvoker<T> extends HotSwappingInvoker<T> {

    private static final long serialVersionUID = 1L;

    NonFinalizingHotSwappingInvoker(final Class<?>[] types, final ProxyFactory proxyFactory,
                                    final ObjectReference<Object> delegateReference,
                                    final DelegationMode delegationMode) {
        super(types, proxyFactory, delegateReference, delegationMode);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if ("finalize".equals(method.getName()) && args != null && args.length == 0) {
            return null;
        }

        /*
         * If the method being invoked is annotated with @IdGetter and the delegate reference is an EntityObjectReference,
         * return the id of the EntityObjectReference's key. This allows us to return the referenced entity's id without
         * fetching the entity from the datastore.
         */
        if (method.getAnnotation(IdGetter.class) != null) {
            ObjectReference<Object> delegateReference = getDelegateReference();
            if (delegateReference instanceof EntityObjectReference) {
                EntityObjectReference entityObjectReference = (EntityObjectReference) delegateReference;
                return entityObjectReference.__getKey().getId();
            }
        }

        return super.invoke(proxy, method, args);
    }

}
