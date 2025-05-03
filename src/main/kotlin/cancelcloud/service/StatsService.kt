package cancelcloud.service
import cancelcloud.PurpurInsightPlugin
import com.sun.management.OperatingSystemMXBean
import java.io.File
import java.lang.management.ManagementFactory

data class ServerStats(
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val tps1: Double,
    val tps5: Double,
    val tps15: Double,
    val ramUsed: Long,
    val ramMax: Long,
    val cpuLoad: Double,
    val uptimeMillis: Long,
    val versionUrl: String,
    val pluginCount: Int,
    val worldCount: Int,
    val chunkCount: Int,
    val entityCount: Int,
    val avgLatency: Int,
    val topPlaytime: List<PlayerPlaytime>,
    val diskTotal: Long,
    val diskFree: Long
)

object StatsService {
    fun collectAll(): ServerStats {
        val plugin = PurpurInsightPlugin.instance
        val server = plugin.server

        // TPS (Paper API)
        val tps = server.tps  // DoubleArray(3)

        // RAM
        val rt = Runtime.getRuntime()
        val used = rt.totalMemory() - rt.freeMemory()
        val max = rt.maxMemory()

        // CPU
        val os = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val load = os.processCpuLoad * 100

        // Uptime
        val up = ManagementFactory.getRuntimeMXBean().uptime

        // Worlds
        val worlds = server.worlds
        val chunks = worlds.sumOf { it.loadedChunks.size }
        val entities = worlds.sumOf { it.entities.size }

        // Latency
        val lat = server.onlinePlayers.map { (it.ping) }.average().toInt()

        // Disk
        val root = File(".")
        val total = root.totalSpace
        val free = root.freeSpace

        return ServerStats(
            onlinePlayers = server.onlinePlayers.size,
            maxPlayers = server.maxPlayers,
            tps1 = tps.getOrNull(0) ?: 0.0,
            tps5 = tps.getOrNull(1) ?: 0.0,
            tps15 = tps.getOrNull(2) ?: 0.0,
            ramUsed = used,
            ramMax = max,
            cpuLoad = load,
            uptimeMillis = up,
            versionUrl = "https://purpurmc.org/download/purpur/1.21.4",
            pluginCount = server.pluginManager.plugins.size,
            worldCount = worlds.size,
            chunkCount = chunks,
            entityCount = entities,
            avgLatency = lat,
            topPlaytime = PlayerDataService.topPlaytime(5),
            diskTotal = total,
            diskFree = free
        )
    }
}