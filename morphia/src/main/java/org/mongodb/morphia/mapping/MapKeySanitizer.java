package org.mongodb.morphia.mapping;

/**
 * @author Ross M. Lodge
 */
public interface MapKeySanitizer {

	public String sanitizeMapKey(String unsanitized);

	public Object unsanitizeMapKey(Object sanitized);

}
