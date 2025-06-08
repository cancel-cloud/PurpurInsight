package cancelcloud.service

import cancelcloud.PurpurInsightPlugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

object LinkService {
    private lateinit var file: File
    private lateinit var cfg: FileConfiguration
    private val pending = mutableMapOf<UUID, Long>()

    fun load(plugin: PurpurInsightPlugin) {
        file = File(plugin.dataFolder, "links.yml")
        if (!file.exists()) {
            file.createNewFile()
        }
        cfg = YamlConfiguration.loadConfiguration(file)
    }

    fun getDiscordId(uuid: UUID): Long? {
        if (!::cfg.isInitialized) return null
        return cfg.getLong("links.$uuid", 0).takeIf { it != 0L }
    }

    fun link(uuid: UUID, discordId: Long) {
        cfg.set("links.$uuid", discordId)
        save()
    }

    private fun save() {
        cfg.save(file)
    }

    fun createRequest(uuid: UUID, discordId: Long) {
        pending[uuid] = discordId
    }

    fun takeRequest(uuid: UUID): Long? = pending.remove(uuid)

    fun requestExists(uuid: UUID, discordId: Long): Boolean = pending[uuid] == discordId

    fun getRequestByDiscord(discordId: Long): UUID? = pending.entries.firstOrNull { it.value == discordId }?.key
}
