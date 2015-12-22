package org.mongodb.morphia.mapping.lazy;


import com.thoughtworks.proxy.ProxyFactory;
import com.thoughtworks.proxy.kit.ObjectReference;
import com.thoughtworks.proxy.toys.delegate.DelegationMode;
import com.thoughtworks.proxy.toys.hotswap.HotSwappingInvoker;

import java.lang.reflect.Method;


class NonFinalizingHotSwappingInvoker<T> extends HotSwappingInvoker<T> {

    private static final long serialVersionUID = 1L;

    public NonFinalizingHotSwappingInvoker(final Class<?>[] types, final ProxyFactory proxyFactory,
                                           final ObjectReference<Object> delegateReference,
                                           final DelegationMode delegationMode) {
        super(types, proxyFactory, delegateReference, delegationMode);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if ("finalize".equals(method.getName()) && args != null && args.length == 0) {
            return null;
        }

        return super.invoke(proxy, method, args);
    }

}
