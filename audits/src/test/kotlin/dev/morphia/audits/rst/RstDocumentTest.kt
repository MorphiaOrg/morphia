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
        val operator = "meta"
        val lines = File("${RstAuditor.aggRoot}/${operator}.txt").readLines().toMutableList()
        val partition = DASH.partition(lines).entries.last()

        println("**************** partition = ${partition.key}")
        val examples = TILDE.partition(partition.value)
        val cases =
            examples
                .map { it.key.sanitize() to it.value.extractTabs() }
                .flatMap { (name, map) ->
                    val mapTo = map.entries.map { (key, value) -> "$name :: $key" to value }
                    mapTo
                }
                .toMap()
        //            .map {
        //                OperatorExample(it.key, it.value)
        //            }
        println("**************** cases = ${cases}")
    }

    private fun MutableList<String>.extractTabs(): Map<String, MutableList<String>> {
        val main = removeWhile { !it.startsWith(TABS_START) }.toMutableList()
        val tabs = multiMapOf<String, String>()
        while (contains(TABS_START)) {
            val localTabs = mutableMapOf<String, MutableList<String>>()
            removeFirst()
            removeWhile { it.isBlank() }
            extractFancyTabs(localTabs)
            extractSimpleTabs(localTabs)
            val appendix = removeWhile { !it.startsWith(TABS_START) }.toMutableList()
            localTabs.values.forEach { it += appendix }

            localTabs.forEach { tabs[it.key] += it.value }
        }
        return if (tabs.isEmpty()) {
            mapOf("main" to main)
        } else {
            val tabMaps = mutableMapOf<String, MutableList<String>>()
            tabs.forEach { entry ->
                entry.value.forEachIndexed { index, list ->
                    tabMaps.put(if (index == 0) entry.key else "${entry.key} [${index}]", list)
                }
            }

            tabMaps
        }
    }

    private fun MutableList<String>.extractSimpleTabs(
        tabs: MutableMap<String, MutableList<String>>
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

    fun <K, V> multiMapOf(): MultiMap<K, MutableList<V>> = MultiMap()

    fun String.atLeastIndent(indent: Int) = isBlank() || findIndent() >= indent

    @Test
    fun testAbs() {
        val document = readAggOperator("abs")
        assertEquals(document.exampleSection.examples.size, 1)
        val example = document.exampleSection.examples.first()
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
        val document = readAggOperator("acos")
        val examples = document.exampleSection.examples

        assertEquals(
            examples.size,
            2,
            "Should have a subsection for each " + "tab: ${examples.map { it.name }}"
        )
        compareBlocks(examples[0], examples[1])
    }

    @Test
    fun testMeta() {
        val document = readAggOperator("meta")
        val examples = document.exampleSection.examples

        assertEquals(
            examples.size,
            7,
            "Should have a subsection for each " + "tab: ${examples.map { it.name }}"
        )
        validateExample("main", examples[0], ExampleValidator(false))
        var name = "\$meta: \"textScore\""
        val validator = ExampleValidator(index = true)
        validateExample("$name :: Aggregation tab", examples[1], validator)
        validateExample("$name :: Find and Project tab", examples[2], validator)
        var comparator = BlockComparator(data = true, index = true)
        compareBlocks(examples[1], examples[2], comparator)

        comparator = BlockComparator(data = true, expected = true, index = true)

        name = "\$meta: \"indexKey\""
        validateExample("$name :: Aggregation tab", examples[3], validator)
        validateExample("$name :: Find and Project tab", examples[4], validator)
        compareBlocks(examples[3], examples[4], comparator)

        validateExample("$name :: Aggregation tab", examples[5], validator)
        validateExample("$name :: Find and Project tab", examples[6], validator)
        compareBlocks(examples[5], examples[6], comparator)
    }

    private fun compareBlocks(
        first: OperatorExample,
        second: OperatorExample,
        comparator: BlockComparator = BlockComparator(),
    ) {
        comparator.action(first, second)
        comparator.data(first, second)
        comparator.expected(first, second)
        comparator.index(first, second)
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

class MultiMap<K, V> : LinkedHashMap<K, MutableList<V>>() {
    override fun get(key: K): MutableList<V> {
        if (!containsKey(key)) {
            put(key, mutableListOf())
        }
        return super.get(key)!!
    }
    /*
        override fun put(key: K, value: MutableList<V>): MutableList<V> {
            get(key) += value

            return value
        }
    */
}
