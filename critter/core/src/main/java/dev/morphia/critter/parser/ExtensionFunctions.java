package dev.morphia.critter.parser;

import java.util.Locale;
import java.util.regex.Pattern;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class ExtensionFunctions {

    private static final Pattern SNAKE_CASE_REGEX = Pattern.compile("(?<=.)[A-Z]");

    public static String titleCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        return "%c%s".formatted(Character.toUpperCase(s.charAt(0)), s.substring(1));
    }

    public static String methodCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        return "%c%s".formatted(Character.toLowerCase(s.charAt(0)), s.substring(1));
    }

    public static String snakeCase(String s) {
        return SNAKE_CASE_REGEX.matcher(s).replaceAll(m -> "_" + m.group().toLowerCase(Locale.getDefault()));
    }

    /**
     * Converts a getter method to a property name. Examples:
     * - "getName" -> "name"
     * - "isActive" -> "active"
     * - "getX" -> "x"
     * - "name()" with matching field "name" -> "name"
     *
     * @param method the method node
     * @param entity the class to check for matching fields when method name doesn't follow standard
     *               getter naming
     */
    public static String getterToPropertyName(MethodNode method, Class<?> entity) {
        String methodName = method.name;

        // Standard getter patterns
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return methodCase(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return methodCase(methodName.substring(2));
        }

        // Check if method name matches a field: no parameters and return type matches field type
        Type[] argTypes = Type.getArgumentTypes(method.desc);
        Type returnType = Type.getReturnType(method.desc);
        if (argTypes.length == 0) {
            for (java.lang.reflect.Field field : entity.getDeclaredFields()) {
                if (field.getName().equals(methodName) && Type.getType(field.getType()).equals(returnType)) {
                    return methodName;
                }
            }
        }

        return methodCase(methodName);
    }
}
