package cancelcloud.listener

import cancelcloud.PurpurInsightPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener : Listener {
    private val joinTimes = mutableMapOf<String, Long>()

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        joinTimes[e.player.uniqueId.toString()] = System.currentTimeMillis()
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        val id = e.player.uniqueId.toString()
        val join = joinTimes.remove(id) ?: return
        val delta = System.currentTimeMillis() - join

        val cfg = PurpurInsightPlugin.instance.config
        val path = "playtime.$id"
        val previous = cfg.getLong(path, 0L)
        cfg.set(path, previous + delta)
        PurpurInsightPlugin.instance.saveConfig()
    }
}