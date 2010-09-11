/**
 * 
 */
package com.google.code.morphia.mapping.lazy;

import com.google.code.morphia.logging.MorphiaLogger;
import com.google.code.morphia.logging.MorphiaLoggerFactory;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class LazyFeatureDependencies {
	
	private static final MorphiaLogger logger = MorphiaLoggerFactory.get(LazyFeatureDependencies.class);
	private static Boolean fullFilled;
	
	private LazyFeatureDependencies() {
	}
	
	public static boolean assertDependencyFullFilled() {
		boolean fullfilled = testDependencyFullFilled();
		if (!fullfilled)
			logger.warning("Lazy loading impossible due to missing dependencies.");
		return fullfilled;
	}
	
	public static boolean testDependencyFullFilled() {
		if (fullFilled != null)
			return fullFilled;
		try {
			fullFilled = Class.forName("net.sf.cglib.proxy.Enhancer") != null
					&& Class.forName("com.thoughtworks.proxy.toys.hotswap.HotSwapping") != null;
		} catch (ClassNotFoundException e) {
			fullFilled = false;
		}
		return fullFilled;
	}
	
	/**
	 * @return
	 */
	public static LazyProxyFactory createDefaultProxyFactory() {
		if (testDependencyFullFilled())
			try {
				return (LazyProxyFactory) Class.forName("com.google.code.morphia.mapping.lazy.CGLibLazyProxyFactory").newInstance();
			} catch (InstantiationException e) {
				logger.warning("While instanciating com.google.code.morphia.mapping.lazy.CGLibLazyProxyFactory",e);
			} catch (IllegalAccessException e) {
				logger.warning("While instanciating com.google.code.morphia.mapping.lazy.CGLibLazyProxyFactory",e);
			} catch (ClassNotFoundException e) {
				logger.warning("While instanciating com.google.code.morphia.mapping.lazy.CGLibLazyProxyFactory",e);
			}
		return null;
	}
}
