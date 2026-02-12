package dev.morphia.critter.parser

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import java.util.Locale
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode

fun KSDeclaration.packageName() = packageName.asString()

fun KSDeclaration.simpleName() = simpleName.asString()

fun KSTypeReference.packageName(): String {
    val declaration = resolve().declaration
    declaration.qualifiedName
    return declaration.packageName()
}

fun KSTypeReference.simpleName(): String {
    return resolve().declaration.simpleName()
}

fun String.titleCase(): String {
    return first().uppercase(Locale.getDefault()) + substring(1)
}

fun String.methodCase(): String {
    return first().lowercase(Locale.getDefault()) + substring(1)
}

/**
 * Converts a getter method to a property name. Examples:
 * - "getName" -> "name"
 * - "isActive" -> "active"
 * - "getX" -> "x"
 * - "name()" with matching field "name" -> "name"
 *
 * @param entity The class to check for matching fields when method name doesn't follow standard
 *   getter naming
 */
fun MethodNode.getterToPropertyName(entity: Class<*>): String {
    val methodName = this.name

    // Standard getter patterns
    if (methodName.startsWith("get") && methodName.length > 3) {
        return methodName.substring(3).methodCase()
    }
    if (methodName.startsWith("is") && methodName.length > 2) {
        return methodName.substring(2).methodCase()
    }

    // Check if method name matches a field: no parameters and return type matches field type
    val paramCount = Type.getArgumentTypes(this.desc).size
    val returnType = Type.getReturnType(this.desc)
    if (paramCount == 0) {
        val matchingField =
            entity.declaredFields.find { field ->
                field.name == methodName && Type.getType(field.type) == returnType
            }
        if (matchingField != null) {
            return methodName
        }
    }

    return methodName.methodCase()
}

private val snakeCaseRegex = Regex("(?<=.)[A-Z]")

fun String.snakeCase(): String {
    return snakeCaseRegex.replace(this, "_$0").lowercase(Locale.getDefault())
}
