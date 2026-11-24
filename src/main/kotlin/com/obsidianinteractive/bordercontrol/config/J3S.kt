
package com.obsidianinteractive.bordercontrol.config

object J3S {

    // J3S document elements
    sealed interface Element

    data class Title(
        val name: String
    ) : Element

    data class Comment(
        val content: String,
        val prefix: String = "//"
    ) : Element

    data class Property(
        val name: String,
        val value: Value
    ) : Element

    data object BlankLine : Element

    // J3S value types
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

    private val titlePattern =
        Regex("""^\[\{'([^']+)'\}\]$""")

    private val propertyPattern =
        Regex("""^'([^']+)'\s*=\s*(.*):$""")

    private val decimalPattern =
        Regex("""^[+-]?(?:\d+\.\d*|\d*\.\d+)(?:[eE][+-]?\d+)?$""")

    class Document(
        val elements: MutableList<Element> = mutableListOf()
    ) {

        fun hasTitle(name: String): Boolean {
            return elements.any {
                it is Title && it.name == name
            }
        }

        fun getProperties(
            title: String
        ): Map<String, Value> {

            val properties = linkedMapOf<String, Value>()
            var currentTitle: String? = null

            for (element in elements) {
                when (element) {
                    is Title -> currentTitle = element.name

                    is Property -> {
                        if (currentTitle == title) {
                            properties[element.name] = element.value
                        }
                    }

                    else -> Unit
                }
            }

            return properties
        }

        fun get(
            title: String,
            property: String
        ): Value? {
            return getProperties(title)[property]
        }

        fun set(
            title: String,
            property: String,
            value: Value
        ) {

            require(title.isNotBlank())
            require(property.isNotBlank())

            if (!hasTitle(title)) {

                if (
                    elements.isNotEmpty() &&
                    elements.last() != BlankLine
                ) {
                    elements.add(BlankLine)
                }

                elements.add(Title(title))
                elements.add(Property(property, value))

                return
            }

            val start = elements.indexOfFirst {
                it is Title && it.name == title
            }

            var end = start + 1

            while (
                end < elements.size &&
                elements[end] !is Title
            ) {
                val element = elements[end]

                if (
                    element is Property &&
                    element.name == property
                ) {
                    elements[end] = Property(property, value)
                    return
                }

                end++
            }

            // Insert before trailing blank lines.
            while (
                end > start + 1 &&
                elements[end - 1] == BlankLine
            ) {
                end--
            }

            elements.add(
                end,
                Property(property, value)
            )
        }

        fun setDefault(
            title: String,
            property: String,
            value: Value
        ): Boolean {

            if (get(title, property) != null) {
                return false
            }

            set(title, property, value)

            return true
        }
    }

    fun parse(source: String): Document {

        val document = Document()
        val titles = mutableSetOf<String>()
        val properties = mutableSetOf<String>()

        var currentTitle: String? = null

        source.lineSequence().forEachIndexed { index, raw ->

            val line = raw.trim()
            val lineNumber = index + 1

            if (line.isEmpty()) {
                document.elements.add(BlankLine)
                return@forEachIndexed
            }

            if (line.startsWith("//")) {
                document.elements.add(
                    Comment(
                        line.removePrefix("//"),
                        "//"
                    )
                )

                return@forEachIndexed
            }

            if (line.startsWith("#")) {
                document.elements.add(
                    Comment(
                        line.removePrefix("#"),
                        "#"
                    )
                )

                return@forEachIndexed
            }

            val title = titlePattern.matchEntire(line)

            if (title != null) {

                val name = title.groupValues[1]

                require(titles.add(name)) {
                    "Duplicate J3S title '$name' at line $lineNumber"
                }

                currentTitle = name
                properties.clear()

                document.elements.add(Title(name))

                return@forEachIndexed
            }

            val property = propertyPattern.matchEntire(line)
                ?: error(
                    "Invalid J3S syntax at line $lineNumber: $line"
                )

            require(currentTitle != null) {
                "J3S property outside title at line $lineNumber"
            }

            val name = property.groupValues[1]
            val rawValue = property.groupValues[2].trim()

            require(properties.add(name)) {
                "Duplicate J3S property '$name' at line $lineNumber"
            }

            document.elements.add(
                Property(
                    name,
                    parseValue(rawValue)
                )
            )
        }

        return document
    }

    fun parseValue(raw: String): Value {

        val value = raw.trim()

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

    fun write(document: Document): String {

        return document.elements.joinToString("\n") { element ->

            when (element) {

                is Title ->
                    "[{'${element.name}'}]"

                is Comment ->
                    "${element.prefix}${element.content}"

                is Property ->
                    "'${element.name}' = ${writeValue(element.value)}:"

                BlankLine -> ""
            }
        }
    }
}