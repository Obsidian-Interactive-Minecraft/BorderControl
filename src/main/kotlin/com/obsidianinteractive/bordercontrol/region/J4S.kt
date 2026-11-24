
package com.obsidianinteractive.bordercontrol.region

object J4S {

    sealed interface Element

    data class Title(
        val name: String
    ) : Element

    data class Comment(
        val content: String,
        val prefix: String = "//"
    ) : Element

    data object BlankLine : Element

    data class ObjectStart(
        val identifier: String
    ) : Element

    data object ObjectBodyEnd : Element

    data class ObjectEnd(
        val comma: Boolean = false
    ) : Element

    data class Tuple(
        val values: List<Value>
    ) : Element

    data object GroupStart : Element

    data object GroupEnd : Element

    data class Property(
        val name: String,
        val value: Value,
        val comma: Boolean = false
    ) : Element

    data class ListStart(
        val name: String,
        val count: Int
    ) : Element

    data class ListEnd(
        val comma: Boolean = false
    ) : Element

    data class EntryStart(
        val values: List<Value>
    ) : Element

    data object EntryEnd : Element

    sealed interface Value {

        data class BooleanValue(
            val value: Boolean
        ) : Value

        data class IntegerValue(
            val value: Long
        ) : Value

        data class DecimalValue(
            val value: Double
        ) : Value

        data class StringValue(
            val value: String
        ) : Value

        data class RawValue(
            val value: String
        ) : Value
    }

    data class Document(
        val elements: MutableList<Element> =
            mutableListOf()
    )

    private val titlePattern =
        Regex("""^\[\{'([^']+)'\}\]$""")

    private val objectPattern =
        Regex("""^\{([^\s\[\]{}]+)\s*\[$""")

    private val tuplePattern =
        Regex("""^\((.*)\):$""")

    private val propertyPattern =
        Regex("""^"([^"]+)"\s*=\s*(.*?)\s*:(,?)$""")

    private val listPattern =
        Regex("""^([\w.-]+)<~(\d+)~$""")

    private val decimalPattern =
        Regex("""^[+-]?(?:\d+\.\d*|\d*\.\d+)(?:[eE][+-]?\d+)?$""")

    fun parseValue(raw: String): Value {

        val value = raw.trim()

        require(value.isNotEmpty()) {
            "Empty J4S value"
        }

        return when {

            value == "true" ->
                Value.BooleanValue(true)

            value == "false" ->
                Value.BooleanValue(false)

            value.startsWith("\"") &&
                    value.endsWith("\"") &&
                    value.length >= 2 ->
                Value.StringValue(
                    value.substring(1, value.length - 1)
                )

            value.toLongOrNull() != null ->
                Value.IntegerValue(value.toLong())

            decimalPattern.matches(value) &&
                    value.toDoubleOrNull()?.isFinite() == true ->
                Value.DecimalValue(value.toDouble())

            else ->
                Value.RawValue(value)
        }
    }

    fun writeValue(value: Value): String {

        return when (value) {

            is Value.BooleanValue ->
                value.value.toString()

            is Value.IntegerValue ->
                value.value.toString()

            is Value.DecimalValue ->
                value.value.toString()

            is Value.StringValue ->
                "\"${value.value}\""

            is Value.RawValue ->
                value.value
        }
    }

    private fun parseValues(
        source: String
    ): List<Value> {

        if (source.isBlank()) {
            return emptyList()
        }

        val values = mutableListOf<Value>()
        val current = StringBuilder()

        var quoted = false

        for (character in source) {

            when {

                character == '"' -> {
                    quoted = !quoted
                    current.append(character)
                }

                character == ',' && !quoted -> {
                    values.add(
                        parseValue(current.toString())
                    )

                    current.clear()
                }

                else -> current.append(character)
            }
        }

        require(!quoted) {
            "Unterminated J4S string"
        }

        values.add(
            parseValue(current.toString())
        )

        return values
    }

    fun parse(source: String): Document {

        val document = Document()

        source.lineSequence().forEachIndexed { index, raw ->

            val line = raw.trim()
            val lineNumber = index + 1

            val element = try {

                parseElement(line)

            } catch (exception: Exception) {

                throw IllegalArgumentException(
                    "Invalid J4S syntax at line $lineNumber: $line",
                    exception
                )
            }

            document.elements.add(element)
        }

        return document
    }

    private fun parseElement(line: String): Element {

        if (line.isEmpty()) {
            return BlankLine
        }

        if (line.startsWith("//")) {
            return Comment(line.removePrefix("//"))
        }

        if (line.startsWith("#")) {
            return Comment(line.removePrefix("#"), "#")
        }

        titlePattern.matchEntire(line)?.let {
            return Title(it.groupValues[1])
        }

        objectPattern.matchEntire(line)?.let {
            return ObjectStart(it.groupValues[1])
        }

        tuplePattern.matchEntire(line)?.let {
            return Tuple(
                parseValues(it.groupValues[1])
            )
        }

        propertyPattern.matchEntire(line)?.let {
            return Property(
                it.groupValues[1],
                parseValue(it.groupValues[2]),
                it.groupValues[3] == ","
            )
        }

        listPattern.matchEntire(line)?.let {
            return ListStart(
                it.groupValues[1],
                it.groupValues[2].toInt()
            )
        }

        when (line) {

            "(" -> return GroupStart

            ")" -> return GroupEnd

            "]" -> return ObjectBodyEnd

            "}:" -> return ObjectEnd(false)

            "}:," -> return ObjectEnd(true)

            ">" -> return ListEnd(false)

            ">," -> return ListEnd(true)

            "/:" -> return EntryEnd
        }

        if (line.endsWith("\\")) {

            return EntryStart(
                parseValues(
                    line.dropLast(1)
                )
            )
        }

        error("Unrecognized J4S element")
    }

    fun write(document: Document): String =
        buildString {

            var depth = 0

            for (element in document.elements) {

                when (element) {

                    ObjectBodyEnd,
                    GroupEnd,
                    is ListEnd,
                    EntryEnd -> {
                        depth = (depth - 1).coerceAtLeast(0)
                    }

                    else -> Unit
                }

                if (element == BlankLine) {
                    appendLine()
                    continue
                }

                append("    ".repeat(depth))

                appendLine(
                    when (element) {

                        is Title ->
                            "[{'${element.name}'}]"

                        is Comment ->
                            "${element.prefix}${element.content}"

                        is ObjectStart ->
                            "{${element.identifier} ["

                        ObjectBodyEnd ->
                            "]"

                        is ObjectEnd ->
                            if (element.comma) "}:," else "}:"

                        is Tuple ->
                            "(" + element.values.joinToString(", ") {
                                writeValue(it)
                            } + "):"

                        GroupStart ->
                            "("

                        GroupEnd ->
                            ")"

                        is Property ->
                            "\"${element.name}\" = " +
                                    writeValue(element.value) +
                                    ":" +
                                    if (element.comma) "," else ""

                        is ListStart ->
                            "${element.name}<~${element.count}~"

                        is ListEnd ->
                            if (element.comma) ">," else ">"

                        is EntryStart ->
                            element.values.joinToString(", ") {
                                writeValue(it)
                            } + "\\"

                        EntryEnd ->
                            "/:"

                        BlankLine ->
                            ""
                    }
                )

                when (element) {

                    is ObjectStart,
                    GroupStart,
                    is ListStart,
                    is EntryStart -> depth++

                    else -> Unit
                }
            }
        }
}