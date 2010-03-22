package com.google.code.morphia;


/**
 * 
 * @author Scott Hernnadez
 *
 */
public class DatastoreService {
	private static Morphia mor;
	private static DatastoreSimple ds;
	
	static {
		mor = new Morphia();
		ds = mor.createDatastore();
	}
	
	public static DatastoreSimple getDatastore() {
		return ds;
	}
	
	@SuppressWarnings("unchecked")
	public static void mapClass(Class c) {
		mor.map(c);
	}

	@SuppressWarnings("unchecked")
	public static void mapClasses(Class[] classes) {
		for (Class c: classes)
			mapClass(c);
	}

	public static void mapPackage(String pkg) {
		mor.mapPackage(pkg, true);
	}
}