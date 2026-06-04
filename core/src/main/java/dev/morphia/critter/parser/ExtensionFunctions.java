package dev.morphia.critter.parser;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility methods for string and type transformations used during Critter code generation.
 */
public class ExtensionFunctions {

    /** @hidden */
    private ExtensionFunctions() {
    }

    private static final Pattern SNAKE_CASE_REGEX = Pattern.compile("(?<=.)[A-Z]");

    /**
     * Converts a string to title case by capitalizing the first character.
     */
    public static String titleCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        return "%c%s".formatted(Character.toUpperCase(s.charAt(0)), s.substring(1));
    }

    /**
     * Converts a string to method (camel) case by lower-casing the first character.
     */
    public static String methodCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        return "%c%s".formatted(Character.toLowerCase(s.charAt(0)), s.substring(1));
    }

    /**
     * Converts a camelCase string to snake_case.
     */
    public static String snakeCase(String s) {
        return SNAKE_CASE_REGEX.matcher(s).replaceAll(m -> "_" + m.group().toLowerCase(Locale.getDefault()));
    }

    /**
     * Converts a getter method to a property name. Examples:
     * - "getName" -> "name"
     * - "isActive" -> "active"
     * - "getX" -> "x"
     * - "name()" with matching field "name" -> "name"
     */
    public static String getterToPropertyName(MethodInfo method, Class<?> entity) {
        String methodName = method.name();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return methodCase(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return methodCase(methodName.substring(2));
        }

        // Parse desc to find argument types and return type
        java.lang.constant.MethodTypeDesc mtd = java.lang.constant.MethodTypeDesc.ofDescriptor(method.desc());
        if (mtd.parameterCount() == 0) {
            String returnDesc = mtd.returnType().descriptorString();
            for (java.lang.reflect.Field field : entity.getDeclaredFields()) {
                String fieldDesc = io.github.dmlloyd.classfile.TypeKind
                        .from(java.lang.constant.ClassDesc.ofDescriptor(returnDesc)).upperBound().descriptorString();
                if (field.getName().equals(methodName)) {
                    return methodName;
                }
            }
        }

        return methodCase(methodName);
    }
}
