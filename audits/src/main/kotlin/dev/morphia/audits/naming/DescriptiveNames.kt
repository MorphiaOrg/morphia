package dev.morphia.audits.naming

import java.text.Normalizer
import java.util.Locale
import javax.lang.model.SourceVersion

/** Input is the AST's plain text, never raw RST or the example's ordinal. */
object DescriptiveNames {
    const val MAX_STEM = 80
    const val MAX_RESOURCE_PATH_BYTES = 240
    const val MAX_FIXTURE_LEAF_BYTES = 32
    private val identifier = Regex("[a-z][A-Za-z0-9]*")
    private val generic = Regex("(?:main|examples?|usage|overview|examples?[0-9]+|[0-9]+)")
    private val devices = Regex("(?:con|prn|aux|nul|com[1-9]|lpt[1-9])")
    private val restricted =
        setOf("var", "yield", "record", "sealed", "permits", "when", "true", "false", "null")

    fun isGeneric(title: String): Boolean {
        val words = words(title)
        return words.isEmpty() || generic.matches(words.joinToString("").lowercase(Locale.ROOT))
    }

    fun stem(title: String): String {
        require(!isGeneric(title)) {
            "A descriptive title is required; recover context or provide a reviewed name"
        }
        val words = words(title).map { it.lowercase(Locale.ROOT) }
        val camel = words.first() + words.drop(1).joinToString("") { capitalize(it) }
        val result =
            if (!camel.first().isLetter() || reserved(camel)) "case" + capitalize(camel) else camel
        validate(result)
        return result
    }

    fun validate(stem: String) {
        require(stem.length <= MAX_STEM) {
            "Names over $MAX_STEM characters require a reviewed bounded abbreviation"
        }
        require(identifier.matches(stem) && !reserved(stem) && !isGeneric(stem)) {
            "Invalid descriptive Java/folder identifier: $stem"
        }
    }

    fun method(stem: String): String {
        validate(stem)
        return "test" + capitalize(stem)
    }

    fun resourcePath(directory: String, stem: String): String {
        validate(stem)
        val result = "$directory/$stem"
        require(
            result.toByteArray(Charsets.UTF_8).size + 1 + MAX_FIXTURE_LEAF_BYTES <=
                MAX_RESOURCE_PATH_BYTES
        ) {
            "Resource path exceeds $MAX_RESOURCE_PATH_BYTES UTF-8 bytes"
        }
        return result
    }

    fun fixturePath(directory: String, stem: String, fileName: String): String {
        require(
            fileName.matches(Regex("[A-Za-z0-9][A-Za-z0-9_.-]*")) &&
                !fileName.endsWith('.') &&
                fileName.toByteArray(Charsets.UTF_8).size <= MAX_FIXTURE_LEAF_BYTES &&
                !reserved(fileName.substringBefore('.'))
        ) {
            "Invalid portable fixture leaf"
        }
        return resourcePath(directory, stem) + "/" + fileName
    }

    private fun reserved(value: String): Boolean =
        SourceVersion.isKeyword(value) ||
            value in restricted ||
            devices.matches(value.lowercase(Locale.ROOT))

    private fun capitalize(value: String): String =
        value.take(1).uppercase(Locale.ROOT) + value.drop(1)

    private fun words(title: String): List<String> {
        val ascii = buildString {
            Normalizer.normalize(title, Normalizer.Form.NFKD).codePoints().forEach { point ->
                val type = Character.getType(point)
                when {
                    type in
                        setOf(
                            Character.NON_SPACING_MARK.toInt(),
                            Character.COMBINING_SPACING_MARK.toInt(),
                            Character.ENCLOSING_MARK.toInt(),
                        ) -> Unit
                    point in 65..90 || point in 97..122 || point in 48..57 -> append(point.toChar())
                    point < 128 ||
                        Character.isWhitespace(point) ||
                        type in
                            setOf(
                                Character.CONNECTOR_PUNCTUATION.toInt(),
                                Character.DASH_PUNCTUATION.toInt(),
                                Character.START_PUNCTUATION.toInt(),
                                Character.END_PUNCTUATION.toInt(),
                                Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
                                Character.FINAL_QUOTE_PUNCTUATION.toInt(),
                                Character.OTHER_PUNCTUATION.toInt(),
                                Character.CONTROL.toInt(),
                                Character.FORMAT.toInt(),
                            ) -> append(' ')
                    else -> append(" unicode${point.toString(16)} ")
                }
            }
        }
        return ascii
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotEmpty)
    }
}
