package com.google.code.morphia.mapping.validation;

import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ConstraintViolation {
	public enum Level {
		MINOR, INFO, WARNING, SEVERE, FATAL;
	}

	private final MappedClass clazz;
	private MappedField field = null;
	private final String message;
	private final Level level;

	public ConstraintViolation(Level level, MappedClass clazz, MappedField field, String message) {
		this(level, clazz, message);
		this.field = field;
	}
	
	public ConstraintViolation(Level level, MappedClass clazz, String message) {
		this.level = level;
		this.clazz = clazz;
		this.message = message;
	}
	
	public String render() {
		return clazz.getClazz().getName() + ((field != null) ? "." + field.getClassFieldName() : "") + ": " + message;
	}

	public Level getLevel() {
		return level;
	}

	public String getPrefix() {
		String fn = (field != null) ? field.getClassFieldName() : "";
		return clazz.getClazz().getName() + "." + fn;
	}

}