/**
 * 
 */
package com.google.code.morphia.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import com.google.code.morphia.utils.Assert;

/**
 * @author us@thomas-daily.de
 */
public class VerboseJSR303ConstraintViolationException extends ConstraintViolationException {
	public VerboseJSR303ConstraintViolationException(final Set<ConstraintViolation<?>> vio) {
		super(createVerboseMessage(vio), vio);
		Assert.parameterNotNull(vio, "vio");
	}
	
	private static String createVerboseMessage(final Set<ConstraintViolation<?>> vio) {
		final StringBuffer sb = new StringBuffer(128);
		sb.append("The following constraints have been violated:\n");
		for (final ConstraintViolation<?> c : vio) {
			sb.append(c.getRootBeanClass().getSimpleName());
			sb.append(".");
			sb.append(c.getPropertyPath());
			sb.append(": ");
			sb.append(c.getMessage());
			sb.append(" ('");
			sb.append(c.getInvalidValue());
			sb.append("')\n");
		}
		return sb.toString();
	}
}
