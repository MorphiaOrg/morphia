package dev.morphia.audits.api

import javax.lang.model.element.TypeElement
import javax.lang.model.type.*

/** Canonical names from attributed compiler symbols; no hand-written Java name resolution. */
internal object CompilerTypes {
    fun name(type: TypeMirror): String {
        require(type.kind != TypeKind.ERROR) { "Unresolved compiler type: $type" }
        return when (type) {
            is DeclaredType -> {
                val element = type.asElement() as TypeElement
                val enclosing = type.enclosingType as? DeclaredType
                val owner =
                    if (enclosing != null && enclosing.typeArguments.isNotEmpty())
                        name(enclosing) + "." + element.simpleName
                    else element.qualifiedName.toString()
                owner +
                    if (type.typeArguments.isEmpty()) ""
                    else type.typeArguments.joinToString(",", "<", ">", transform = ::name)
            }
            is ArrayType -> name(type.componentType) + "[]"
            is TypeVariable -> type.asElement().simpleName.toString()
            is WildcardType ->
                when {
                    type.extendsBound != null -> "? extends " + name(type.extendsBound)
                    type.superBound != null -> "? super " + name(type.superBound)
                    else -> "?"
                }
            is IntersectionType -> type.bounds.joinToString("&", transform = ::name)
            is PrimitiveType -> type.kind.name.lowercase(java.util.Locale.ROOT)
            is NoType -> if (type.kind == TypeKind.VOID) "void" else ""
            else ->
                throw IllegalArgumentException("Unsupported compiler type: $type (${type.kind})")
        }
    }
}
