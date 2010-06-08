/**
 * 
 */
package com.google.code.morphia.mapping.lazy.proxy;

import java.util.List;

import com.google.code.morphia.Key;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public interface ProxiedEntityReferenceList extends ProxiedReference {

	void __add(Key<?> key);

	List<Key<?>> __getKeysAsList();

}
