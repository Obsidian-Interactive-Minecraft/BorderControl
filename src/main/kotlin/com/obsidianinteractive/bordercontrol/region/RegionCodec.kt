
package com.obsidianinteractive.bordercontrol.region

object RegionCodec {

    private class Reader(document: J4S.Document) {

        private val elements = document.elements.filterNot {
            it is J4S.Comment || it == J4S.BlankLine
        }

        private var index = 0

        fun hasNext(): Boolean =
            index < elements.size

        fun peek(): J4S.Element? =
            elements.getOrNull(index)

        fun next(): J4S.Element {

            require(hasNext()) {
                "Unexpected end of region document"
            }

            return elements[index++]
        }

        inline fun <reified T : J4S.Element> expect(): T {

            val element = next()

            return element as? T
                ?: error(
                    "Expected ${T::class.simpleName}, got $element"
                )
        }
    }

    private fun readInteger(value: J4S.Value): Int {

        val number = (
                value as? J4S.Value.IntegerValue
                )?.value ?: error("Expected integer")

        require(
            number in Int.MIN_VALUE.toLong()..
                    Int.MAX_VALUE.toLong()
        ) {
            "Integer out of range"
        }

        return number.toInt()
    }

    private fun readString(value: J4S.Value): String {

        return when (value) {

            is J4S.Value.StringValue -> value.value

            is J4S.Value.RawValue -> value.value

            else -> error("Expected string")
        }
    }

    private fun readPoint(reader: Reader): BorderPoint {

        val tuple = reader.expect<J4S.Tuple>()

        require(tuple.values.size == 4) {
            "Border point must contain four values"
        }

        return BorderPoint(
            readInteger(tuple.values[0]),
            readInteger(tuple.values[1]),
            readInteger(tuple.values[2]),
            readString(tuple.values[3])
        )
    }

    private fun readPlayer(reader: Reader): PlayerAccess {

        val entry = reader.expect<J4S.EntryStart>()

        require(entry.values.size == 2) {
            "Player entry requires username and UUID"
        }

        val username = readString(entry.values[0])
        val uuid = readString(entry.values[1])

        val properties = mutableMapOf<String, Boolean>()

        while (reader.peek() != J4S.EntryEnd) {

            val property = reader.expect<J4S.Property>()

            require(
                property.name in setOf(
                    "build",
                    "break",
                    "interact"
                )
            ) {
                "Unknown player permission: ${property.name}"
            }

            val value = (
                    property.value as? J4S.Value.BooleanValue
                    )?.value ?: error("Expected Boolean permission")

            require(
                properties.put(property.name, value) == null
            ) {
                "Duplicate player permission: ${property.name}"
            }
        }

        reader.expect<J4S.EntryEnd>()

        return PlayerAccess(
            username,
            uuid,
            properties["build"],
            properties["break"],
            properties["interact"]
        )
    }

    private fun readPlayerList(
        reader: Reader,
        start: J4S.ListStart
    ): List<PlayerAccess> {

        val players = mutableListOf<PlayerAccess>()

        repeat(start.count) {
            players.add(readPlayer(reader))
        }

        reader.expect<J4S.ListEnd>()

        return players
    }

    fun decode(document: J4S.Document): List<BorderRegion> {

        val reader = Reader(document)
        val regions = mutableListOf<BorderRegion>()

        var owner: String? = null

        while (reader.hasNext()) {

            if (reader.peek() is J4S.Title) {

                owner = reader.expect<J4S.Title>().name

                continue
            }

            val start = reader.expect<J4S.ObjectStart>()

            val currentOwner = owner
                ?: error("Region declared without owner")

            val id = start.identifier.toIntOrNull()
                ?: error("Invalid region ID")

            val first = readPoint(reader)
            val second = readPoint(reader)

            reader.expect<J4S.GroupStart>()

            val type = reader.expect<J4S.Property>()

            require(type.name == "type") {
                "Expected region type"
            }

            val mode = AccessMode.valueOf(
                readString(type.value).uppercase()
            )

            var allowlist = emptyList<PlayerAccess>()
            var blocklist = emptyList<PlayerAccess>()
            var teamPermissions = TeamAccess.defaults()

            val seenLists = mutableSetOf<String>()

            while (reader.peek() is J4S.ListStart) {

                val list = reader.expect<J4S.ListStart>()

                require(seenLists.add(list.name)) {
                    "Duplicate region list: ${list.name}"
                }

                when (list.name) {

                    "allowlist" -> {
                        allowlist = readPlayerList(reader, list)
                    }

                    "blocklist" -> {
                        blocklist = readPlayerList(reader, list)
                    }

                    "team-permissions" -> {
                        teamPermissions = readTeamPermissions(
                            reader,
                            list
                        )
                    }

                    else -> {
                        error("Unknown region list: ${list.name}")
                    }
                }
            }

            reader.expect<J4S.GroupEnd>()
            reader.expect<J4S.ObjectBodyEnd>()
            reader.expect<J4S.ObjectEnd>()

            regions.add(
                BorderRegion(
                    owner = currentOwner,
                    id = id,
                    first = first,
                    second = second,
                    mode = mode,
                    allowlist = allowlist,
                    blocklist = blocklist,
                    teamPermissions = teamPermissions
                )
            )
        }

        return regions
    }

    private fun encodePoint(point: BorderPoint): J4S.Tuple {

        return J4S.Tuple(
            listOf(
                J4S.Value.IntegerValue(point.x.toLong()),
                J4S.Value.IntegerValue(point.y.toLong()),
                J4S.Value.IntegerValue(point.z.toLong()),
                J4S.Value.RawValue(point.dimension)
            )
        )
    }

    private fun encodePlayerList(
        document: J4S.Document,
        name: String,
        players: List<PlayerAccess>,
        comma: Boolean
    ) {

        document.elements.add(
            J4S.ListStart(name, players.size)
        )

        players.forEach { player ->

            document.elements.add(
                J4S.EntryStart(
                    listOf(
                        J4S.Value.StringValue(player.username),
                        J4S.Value.StringValue(player.uuid)
                    )
                )
            )

            val properties = listOfNotNull(
                player.build?.let {
                    "build" to it
                },
                player.breakBlocks?.let {
                    "break" to it
                },
                player.interact?.let {
                    "interact" to it
                }
            )

            properties.forEachIndexed { index, property ->

                document.elements.add(
                    J4S.Property(
                        property.first,
                        J4S.Value.BooleanValue(property.second),
                        index < properties.lastIndex
                    )
                )
            }

            document.elements.add(J4S.EntryEnd)
        }

        document.elements.add(J4S.ListEnd(comma))
    }

    fun encode(regions: List<BorderRegion>): J4S.Document {

        val document = J4S.Document()

        var written = 0

        regions.groupBy { it.owner }.forEach { (owner, entries) ->

            document.elements.add(J4S.Title(owner))

            entries.forEach { region ->

                document.elements.add(
                    J4S.ObjectStart(region.id.toString())
                )

                document.elements.add(
                    encodePoint(region.first)
                )

                document.elements.add(
                    encodePoint(region.second)
                )

                document.elements.add(J4S.GroupStart)

                document.elements.add(
                    J4S.Property(
                        "type",
                        J4S.Value.RawValue(
                            region.mode.name.lowercase()
                        )
                    )
                )

                encodePlayerList(
                    document,
                    "allowlist",
                    region.allowlist,
                    true
                )

                encodePlayerList(
                    document,
                    "blocklist",
                    region.blocklist,
                    false
                )

                document.elements.add(J4S.GroupEnd)

                document.elements.add(J4S.ObjectBodyEnd)

                written++

                document.elements.add(
                    J4S.ObjectEnd(
                        written < regions.size
                    )
                )
            }
        }

        return document
    }


    private fun readTeamPermissions(
        reader: Reader,
        start: J4S.ListStart
    ): Map<TeamRole, TeamAccess> {

        require(start.count in 0..TeamRole.entries.size) {
            "Invalid team permission count"
        }

        val permissions =
            TeamAccess.defaults().toMutableMap()

        val seenRoles = mutableSetOf<TeamRole>()

        repeat(start.count) {

            val entry = reader.expect<J4S.EntryStart>()

            require(entry.values.size == 1) {
                "Team permission entry requires one role"
            }

            val role = TeamRole.valueOf(
                readString(entry.values[0]).uppercase()
            )

            require(seenRoles.add(role)) {
                "Duplicate team role: $role"
            }

            val properties =
                mutableMapOf<String, Boolean?>()

            while (reader.peek() != J4S.EntryEnd) {

                val property =
                    reader.expect<J4S.Property>()

                require(
                    property.name in setOf(
                        "cross",
                        "build",
                        "break",
                        "interact"
                    )
                ) {
                    "Unknown team permission: ${property.name}"
                }

                require(
                    !properties.containsKey(property.name)
                ) {
                    "Duplicate team permission"
                }

                val value = when (
                    val raw = property.value
                ) {

                    is J4S.Value.BooleanValue ->
                        raw.value

                    is J4S.Value.RawValue -> {
                        require(raw.value == "inherit") {
                            "Invalid team permission value"
                        }

                        null
                    }

                    else ->
                        error("Invalid team permission value")
                }

                properties[property.name] = value
            }

            reader.expect<J4S.EntryEnd>()

            permissions[role] = TeamAccess(
                cross = properties["cross"],
                build = properties["build"],
                breakBlocks = properties["break"],
                interact = properties["interact"]
            )
        }

        reader.expect<J4S.ListEnd>()

        return permissions.toMap()
    }


    private fun encodeTeamPermissions(
        document: J4S.Document,
        permissions: Map<TeamRole, TeamAccess>
    ) {

        val roles = TeamRole.entries

        document.elements.add(
            J4S.ListStart(
                "team-permissions",
                roles.size
            )
        )

        roles.forEach { role ->

            val access = permissions[role]
                ?: TeamAccess.defaults().getValue(role)

            document.elements.add(
                J4S.EntryStart(
                    listOf(
                        J4S.Value.StringValue(
                            role.name.lowercase()
                        )
                    )
                )
            )

            val properties = listOf(
                "cross" to access.cross,
                "build" to access.build,
                "break" to access.breakBlocks,
                "interact" to access.interact
            )

            properties.forEachIndexed { index, property ->

                val value = property.second

                val encoded = if (value == null) {
                    J4S.Value.RawValue("inherit")
                } else {
                    J4S.Value.BooleanValue(value)
                }

                document.elements.add(
                    J4S.Property(
                        property.first,
                        encoded,
                        index < properties.lastIndex
                    )
                )
            }

            document.elements.add(J4S.EntryEnd)
        }

        document.elements.add(
            J4S.ListEnd(false)
        )
    }
}