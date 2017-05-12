package org.mongodb.morphia.mapping;

/**
 * The default is a no-op
 *
 * @author Ross M. Lodge
 */
public class DefaultMapKeySanitizer implements MapKeySanitizer {
	@Override
	public String sanitizeMapKey(final String unsanitized) {
		return unsanitized;
	}

	@Override
	public Object unsanitizeMapKey(final Object sanitized) {
		return sanitized;
	}

}
