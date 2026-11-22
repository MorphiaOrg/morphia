package dev.morphia.audits.coverage

/** MongoDB release numbers, including naturally ordered prerelease identifiers such as rc10. */
data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int, val qualifier: String?) :
    Comparable<ReleaseVersion> {
    init {
        require(major >= 0 && minor >= 0 && patch >= 0) { "Negative release component" }
        require(
            qualifier == null || qualifier.matches(Regex("[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*"))
        ) {
            "Invalid release qualifier"
        }
    }

    fun canonical(): ReleaseVersion =
        copy(
            qualifier =
                qualifier?.let { text ->
                    Regex("[0-9]+|[A-Za-z]+").findAll(text).joinToString(".") { match ->
                        if (match.value.all(Char::isDigit)) match.value.toBigInteger().toString()
                        else match.value
                    }
                }
        )

    /** As release notes write it: `8.0`, `7.0.3`, `8.1-rc0`. */
    fun display(): String =
        "$major.$minor" + (if (patch == 0) "" else ".$patch") + qualifier?.let { "-$it" }.orEmpty()

    override fun compareTo(other: ReleaseVersion): Int {
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
            .takeIf { it != 0 }
            ?.let {
                return it
            }
        if (qualifier == null || other.qualifier == null)
            return when {
                qualifier == other.qualifier -> 0
                qualifier == null -> 1
                else -> -1
            }
        val left = Regex("[0-9]+|[A-Za-z]+").findAll(qualifier).map { it.value }.toList()
        val right = Regex("[0-9]+|[A-Za-z]+").findAll(other.qualifier).map { it.value }.toList()
        for (index in 0 until minOf(left.size, right.size)) {
            val a = left[index]
            val b = right[index]
            val compared =
                if (a.all(Char::isDigit) && b.all(Char::isDigit))
                    a.toBigInteger().compareTo(b.toBigInteger())
                else a.compareTo(b)
            if (compared != 0) return compared
        }
        return left.size.compareTo(right.size)
    }

    companion object {
        fun parse(text: String): ReleaseVersion? {
            val match =
                Regex(
                        "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\.(0|[1-9][0-9]*))?(?:-([0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*))?"
                    )
                    .matchEntire(text.trim()) ?: return null
            return ReleaseVersion(
                match.groupValues[1].toIntOrNull() ?: return null,
                match.groupValues[2].toIntOrNull() ?: return null,
                match.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull()
                    ?: if (match.groupValues[3].isEmpty()) 0 else return null,
                match.groupValues[4].takeIf { it.isNotEmpty() },
            )
        }
    }
}

data class VersionRange(
    val minimum: ReleaseVersion?,
    val minimumInclusive: Boolean,
    val maximum: ReleaseVersion?,
    val maximumInclusive: Boolean,
) {
    init {
        require(minimum != null || maximum != null) {
            "An unbounded version claim conveys no release evidence"
        }
        if (minimum != null && maximum != null)
            require(
                minimum < maximum ||
                    (minimum.compareTo(maximum) == 0 && minimumInclusive && maximumInclusive)
            ) {
                "Empty or reversed version range"
            }
    }

    fun exact(): ReleaseVersion? =
        minimum?.takeIf {
            maximum != null && it.compareTo(maximum) == 0 && minimumInclusive && maximumInclusive
        }

    fun equivalent(other: VersionRange): Boolean =
        ((minimum == null && other.minimum == null) ||
            (minimum != null &&
                other.minimum != null &&
                minimum.compareTo(other.minimum) == 0 &&
                minimumInclusive == other.minimumInclusive)) &&
            ((maximum == null && other.maximum == null) ||
                (maximum != null &&
                    other.maximum != null &&
                    maximum.compareTo(other.maximum) == 0 &&
                    maximumInclusive == other.maximumInclusive))

    companion object {
        fun parse(text: String): VersionRange? {
            val value = text.trim()
            ReleaseVersion.parse(value)?.let {
                return VersionRange(it, true, it, true)
            }
            if (value.length > 2 && value.first() in "[(" && value.last() in "])") {
                val sides = value.substring(1, value.lastIndex).split(',')
                if (sides.size != 2) return null
                val low =
                    if (sides[0].isBlank()) null else ReleaseVersion.parse(sides[0]) ?: return null
                val high =
                    if (sides[1].isBlank()) null else ReleaseVersion.parse(sides[1]) ?: return null
                return runCatching {
                        VersionRange(low, value.first() == '[', high, value.last() == ']')
                    }
                    .getOrNull()
            }
            val regex = Regex("(>=|<=|>|<)\\s*([^\\s]+)")
            val bounds = regex.findAll(value).toList()
            if (bounds.isEmpty() || regex.replace(value, "").isNotBlank()) return null
            var low: ReleaseVersion? = null
            var high: ReleaseVersion? = null
            var lowInclusive = false
            var highInclusive = false
            for (bound in bounds) {
                val version = ReleaseVersion.parse(bound.groupValues[2]) ?: return null
                val op = bound.groupValues[1]
                if (op.startsWith('>')) {
                    if (low != null) return null
                    low = version
                    lowInclusive = op == ">="
                } else {
                    if (high != null) return null
                    high = version
                    highInclusive = op == "<="
                }
            }
            return runCatching { VersionRange(low, lowInclusive, high, highInclusive) }.getOrNull()
        }
    }
}
