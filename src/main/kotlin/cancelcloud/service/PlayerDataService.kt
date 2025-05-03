package cancelcloud.service
import cancelcloud.PurpurInsightPlugin
import java.util.UUID
import java.util.concurrent.TimeUnit

data class PlayerPlaytime(val name: String, val millis: Long)

object PlayerDataService {
    fun topPlaytime(limit: Int): List<PlayerPlaytime> {
        val section = PurpurInsightPlugin.instance.config.getConfigurationSection("playtime") ?: return emptyList()
        return section.getKeys(false)
            .mapNotNull { id ->
                val offline = PurpurInsightPlugin.instance.server.getOfflinePlayer(UUID.fromString(id))
                val time = section.getLong(id)
                if (offline.name == null) null else PlayerPlaytime(offline.name!!, time)
            }
            .sortedByDescending { it.millis }
            .take(limit)
            .map {
                val h = TimeUnit.MILLISECONDS.toHours(it.millis)
                val m = TimeUnit.MILLISECONDS.toMinutes(it.millis) % 60
                PlayerPlaytime(it.name, it.millis).copy(name = "${it.name} (${h}h ${m}m)")
            }
    }
}