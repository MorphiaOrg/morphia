package dev.morphia.audits.rst

import dev.morphia.audits.RstAuditor
import dev.morphia.audits.findIndent
import dev.morphia.audits.rst.OperatorExample.Companion.sanitize
import dev.morphia.audits.rst.RstDocument.Companion.FANCY_TAB_START
import dev.morphia.audits.rst.RstDocument.Companion.SIMPLE_TAB_START
import dev.morphia.audits.rst.RstDocument.Companion.TABS_START
import dev.morphia.audits.rst.Separator.DASH
import dev.morphia.audits.rst.Separator.TILDE
import java.io.File
import java.io.StringWriter
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class RstDocumentTest {
    @Test
    fun testQuickParse() {
        parseDoc("meta")
    }

    private fun parseDoc(operator: String): List<OperatorExample> {
        val lines = File("${RstAuditor.aggRoot}/${operator}.txt").readLines().toMutableList()
        val partition = DASH.partition(lines).entries.last()
        val cases =
            TILDE.partition(partition.value)
                .map { it.value.extractTabs(it.key.sanitize()) }
                .flatMap { it.entries }
                .map { OperatorExample(it.key, it.value) }
        return cases
    }

    private fun MutableList<String>.extractTabs(name: String): Map<String, MutableList<String>> {
        val main = removeWhile { !it.startsWith(TABS_START) }.toMutableList()
        val tabs = dedupeMap<String>()
        while (contains(TABS_START)) {
            val localTabs = mutableMapOf<String, MutableList<String>>()
            removeFirst()
            removeWhile { it.isBlank() }
            extractFancyTabs(localTabs)
            extractSimpleTabs(localTabs)
            val appendix = removeWhile { !it.startsWith(TABS_START) }.toMutableList()
            //            localTabs.values.forEach { it += appendix }

            localTabs.forEach {
                tabs["$name :: ${it.key} tab"] = (main + it.value + appendix).toMutableList()
            }
        }
        return if (tabs.isEmpty()) {
            mapOf("main" to main)
        } else {
            //            val tabMaps = mutableMapOf<String, MutableList<String>>()
            //            tabs.forEach { entry ->
            //                entry.value.forEachIndexed { index, list ->
            //                    tabMaps.put(if (index == 0) entry.key else "${entry.key}
            // [${index}]", list)
            //                }
            //            }

            tabs
        }
    }

    private fun MutableList<String>.extractSimpleTabs(
        tabs: MutableMap<String, MutableList<String>>,
    ) {
        if (isNotEmpty() && first().trim().startsWith(SIMPLE_TAB_START)) {
            removeFirst()
            removeWhile { it.isBlank() }
            while (isNotEmpty() && first().trim().startsWith(SIMPLE_TAB_START)) {
                val first = removeFirst()
                val indent = first.findIndent()
                var name = "wut"
                val list =
                    mutableListOf(first) +
                        removeWhile {
                            !first().trim().startsWith(SIMPLE_TAB_START) && it.atLeastIndent(indent)
                        }

                tabs[name] = list.toMutableList()
            }
        }
    }

    private fun MutableList<String>.extractFancyTabs(
        tabs: MutableMap<String, MutableList<String>>,
    ) {
        while (isNotEmpty() && first().trim().startsWith(FANCY_TAB_START)) {
            val first = removeFirst()
            val indent = first.findIndent()
            var name = first.substringAfter(FANCY_TAB_START).trim()
            val list = removeWhile {
                !first().trim().startsWith(FANCY_TAB_START) && it.atLeastIndent(indent)
            }

            tabs[name] = list.toMutableList()
        }
    }

    fun <V> dedupeMap(): DedupeMap<MutableList<V>> = DedupeMap()

    fun String.atLeastIndent(indent: Int) = isBlank() || findIndent() >= indent

    @Test
    fun testAbs() {
        val examples = parseDoc("abs")
        assertEquals(examples.size, 1)
        val example = examples.first()
        val data = StringWriter().also { example.dataBlock?.write(it) }
        val action = StringWriter().also { example.actionBlock?.write(it) }
        val expected = StringWriter().also { example.expectedBlock?.write(it) }

        assertEquals(
            data.toString(),
            """
            { _id: 1, startTemp: 50, endTemp: 80 },
            { _id: 2, startTemp: 40, endTemp: 40 },
            { _id: 3, startTemp: 90, endTemp: 70 },
            { _id: 4, startTemp: 60, endTemp: 70 }
        """
                .trimIndent(),
            "Data block should match"
        )
        assertEquals(
            expected.toString(),
            """
               { "_id" : 1, "delta" : 30 }
               { "_id" : 2, "delta" : 0 }
               { "_id" : 3, "delta" : 20 }
               { "_id" : 4, "delta" : 10 }
        """
                .trimIndent(),
            "Expected block should match"
        )
        assertEquals(
            action.toString(),
            """
            [
               {
                  ${"$"}project: { delta: { ${"$"}abs: { ${"$"}subtract: [ "${"$"}startTemp", "${"$"}endTemp" ] } } }
               }
            ]
        """
                .trimIndent(),
            "Action block should match"
        )
    }

    @Test
    fun testAcos() {
        val examples = parseDoc("acos")

        assertEquals(
            examples.size,
            2,
            "Should have a subsection for each tab: ${examples.map { it.name }}"
        )
    }

    @Test
    fun testMeta() {
        val examples = parseDoc("meta")

        assertEquals(
            examples.size,
            7,
            "Should have a subsection for each tab: ${examples.map { it.name }}"
        )
        validateExample("main", examples[0], ExampleValidator(false))
        var name = "\$meta: \"textScore\""
        val validator = ExampleValidator(index = true)
        validateExample("$name :: Aggregation tab", examples[1], validator)
        validateExample("$name :: Find and Project tab", examples[2], validator)

        name = "\$meta: \"indexKey\""
        validateExample("$name :: Aggregation tab", examples[3], validator)
        validateExample("$name :: Find and Project tab", examples[4], validator)

        validateExample("$name :: Aggregation tab [1]", examples[5], validator)
        validateExample("$name :: Find and Project tab [1]", examples[6], validator)
    }

    private fun validateExample(
        name: String,
        example: OperatorExample,
        validator: ExampleValidator = ExampleValidator(),
    ) {
        validator.name(example, name)
        validator.data(example)
        validator.action(example)
        validator.expected(example)
        validator.index(example)
    }

    private fun readAggOperator(operator: String): RstDocument {
        return RstDocument.read(File("${RstAuditor.aggRoot}/${operator}.txt"))
    }
}

class DedupeMap<V> : LinkedHashMap<String, V>() {
    override fun put(key: String, value: V): V? {
        var count = 1
        var newKey = key
        while (contains(newKey)) {
            newKey = "$key [$count]"
        }

        return super.put(newKey, value)
    }
}
