
package com.obsidianinteractive.bordercontrol.config

import com.mojang.logging.LogUtils
import java.nio.file.Files
import java.nio.file.Path
import net.minecraftforge.fml.loading.FMLPaths

object Config {

    private val LOGGER = LogUtils.getLogger()

    enum class IntegrationMode {
        AUTO,
        ENABLED,
        DISABLED
    }

    private val path: Path
        get() = FMLPaths.CONFIGDIR.get()
            .resolve("bordercontrol/config.j3s")

    private val integrationDefaults = linkedMapOf(
        "ftb-chunks" to IntegrationMode.AUTO,
        "ftb-teams" to IntegrationMode.AUTO,
        "ftb-ranks" to IntegrationMode.AUTO,
        "ftb-essentials" to IntegrationMode.AUTO,
        "luckperms" to IntegrationMode.AUTO,
        "immersive-vehicles" to IntegrationMode.AUTO,
        "create" to IntegrationMode.AUTO,
        "open-parties-and-claims" to IntegrationMode.AUTO
    )

    private data class Settings(
        val maxBorderSize: Int,
        val integrations: Map<String, IntegrationMode>
    )

    @Volatile
    private var settings = Settings(
        64,
        integrationDefaults.toMap()
    )

    val maxBorderSize: Int
        get() = settings.maxBorderSize

    fun getIntegrationMode(name: String): IntegrationMode {
        return settings.integrations[name]
            ?: IntegrationMode.DISABLED
    }

    fun isIntegrationEnabled(name: String): Boolean {
        return getIntegrationMode(name) !=
                IntegrationMode.DISABLED
    }

    fun getIntegrations(): Map<String, IntegrationMode> {
        return settings.integrations.toMap()
    }

    private fun parseIntegrationMode(
        value: J3S.Value?
    ): IntegrationMode? {

        return when (value) {

            is J3S.Value.BooleanValue -> {
                if (value.value) {
                    IntegrationMode.ENABLED
                } else {
                    IntegrationMode.DISABLED
                }
            }

            is J3S.Value.RawValue -> {
                when (value.value.lowercase()) {
                    "auto" -> IntegrationMode.AUTO
                    else -> null
                }
            }

            else -> null
        }
    }

    private fun writeIntegrationMode(
        mode: IntegrationMode
    ): J3S.Value {

        return when (mode) {

            IntegrationMode.AUTO ->
                J3S.Value.RawValue("auto")

            IntegrationMode.ENABLED ->
                J3S.Value.BooleanValue(true)

            IntegrationMode.DISABLED ->
                J3S.Value.BooleanValue(false)
        }
    }

    @Synchronized
    fun load() {

        Files.createDirectories(path.parent)

        val exists = Files.exists(path)

        val document = if (exists) {
            J3S.parse(Files.readString(path))
        } else {
            createDefaultDocument()
        }

        var changed = !exists

        if (
            document.setDefault(
                "general",
                "max-border-size",
                J3S.Value.IntegerValue(64)
            )
        ) {
            changed = true
        }

        integrationDefaults.forEach { (name, mode) ->

            if (
                document.setDefault(
                    "integrations",
                    name,
                    writeIntegrationMode(mode)
                )
            ) {
                changed = true
            }
        }

        val size = (
                document.get("general", "max-border-size")
                        as? J3S.Value.IntegerValue
                )?.value ?: error("Invalid max-border-size")

        require(size in 1L..Int.MAX_VALUE.toLong()) {
            "max-border-size must be a positive integer"
        }

        val loadedIntegrations =
            linkedMapOf<String, IntegrationMode>()

        integrationDefaults.forEach { (name, default) ->

            val value = document.get(
                "integrations",
                name
            )

            val mode = parseIntegrationMode(value)
                ?: run {

                    LOGGER.warn(
                        "[BorderControl] Invalid integration setting " +
                                "'$name'. Using default: $default"
                    )

                    default
                }

            loadedIntegrations[name] = mode
        }

        if (changed) {

            Files.writeString(
                path,
                J3S.write(document)
            )

            LOGGER.info(
                "[BorderControl] Updated configuration file."
            )
        }

        settings = Settings(
            size.toInt(),
            loadedIntegrations.toMap()
        )

        LOGGER.info(
            "[BorderControl] Configuration loaded."
        )

        LOGGER.info(
            "[BorderControl] Maximum border size: $maxBorderSize"
        )

        loadedIntegrations.forEach { (name, mode) ->

            LOGGER.info(
                "[BorderControl] Integration config: " +
                        "$name = ${J3S.writeValue(writeIntegrationMode(mode))}"
            )
        }
    }

    private fun createDefaultDocument(): J3S.Document {

        val document = J3S.Document()

        document.elements.add(
            J3S.Comment(" BorderControl Configuration")
        )

        document.elements.add(
            J3S.Comment(" J3S Configuration Format")
        )

        document.elements.add(J3S.BlankLine)

        document.elements.add(J3S.Title("general"))

        document.elements.add(
            J3S.Comment(" Maximum border width and length.")
        )

        document.set(
            "general",
            "max-border-size",
            J3S.Value.IntegerValue(64)
        )

        document.elements.add(J3S.BlankLine)

        document.elements.add(J3S.Title("integrations"))

        document.elements.add(
            J3S.Comment(" auto = Enable if the mod is installed.")
        )

        document.elements.add(
            J3S.Comment(" true = Explicitly enable integration.")
        )

        document.elements.add(
            J3S.Comment(" false = Explicitly disable integration.")
        )

        document.elements.add(
            J3S.Comment(" Xaero map integrations are always automatic.")
        )

        integrationDefaults.forEach { (name, mode) ->

            document.set(
                "integrations",
                name,
                writeIntegrationMode(mode)
            )
        }

        document.elements.add(J3S.BlankLine)

        return document
    }
}