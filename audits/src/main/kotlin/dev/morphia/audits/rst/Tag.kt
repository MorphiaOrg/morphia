package dev.morphia.audits.rst

class Tag(val type: String, val value: String) {
    private val attributes: MutableList<String> = mutableListOf()

    operator fun plusAssign(attribute: String) {
        attributes += attribute
    }

    override fun toString(): String {
        val attrs =
            if (attributes.isEmpty()) ""
            else {
                ", $attributes"
            }
        return "(type='$type', value='$value'$attrs)"
    }
}
