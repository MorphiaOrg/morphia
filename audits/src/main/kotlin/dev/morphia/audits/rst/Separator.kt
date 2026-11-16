package dev.morphia.audits.rst

/** Legacy heading partitioning; it is not the future documentation AST contract. */
enum class Separator(separator: Char) {
    DASH('-'),
    TILDE('~');

    private val section = Regex("${Regex.escape(separator.toString())}{5,}")

    fun partition(input: List<String>): Map<String, MutableList<String>> {
        var name = "main"
        val partitions = mutableMapOf(name to mutableListOf<String>())
        val lines = input.toMutableList()
        while (lines.isNotEmpty()) {
            partitions[name]?.plusAssign(lines.removeWhile { !section.matches(lines.first()) })
            if (lines.isNotEmpty()) {
                name = partitions[name]!!.removeLast()
                lines.removeFirst()
                partitions[name] = mutableListOf()
            }
        }
        return partitions
    }
}
