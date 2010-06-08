/**
 * 
 */
package com.google.code.morphia.mapping.lazy;

import java.util.logging.Logger;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class LazyFeatureDependencies {
	
	private static final Logger logger = Logger.getLogger(LazyFeatureDependencies.class.getName());
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
}
