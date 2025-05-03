package cancelcloud.util

import cancelcloud.service.ServerStats
import net.dv8tion.jda.api.EmbedBuilder
import java.time.Duration

object EmbedBuilderUtil {
    fun buildEmbed(stats: ServerStats): EmbedBuilder {
        val up = Duration.ofMillis(stats.uptimeMillis)
        val upStr = "%dd %dh %dm".format(up.toDays(), up.toHoursPart(), up.toMinutesPart())

        return EmbedBuilder()
            .setTitle("Server-Statistiken")
            .addField("Online-Spieler", "${stats.onlinePlayers}/${stats.maxPlayers}", true)
            .addField("TPS (1/5/15)", "${"%.2f".format(stats.tps1)}/${"%.2f".format(stats.tps5)}/${"%.2f".format(stats.tps15)}", true)
            .addField("MSPT", "${"%.2f".format(1000.0 / stats.tps1)} ms", true)
            .addField("RAM", "${stats.ramUsed / 1_048_576} MiB / ${stats.ramMax / 1_048_576} MiB", true)
            .addField("CPU-Last", "${"%.1f".format(stats.cpuLoad)} %", true)
            .addField("Uptime", upStr, true)
            .addField("Version", "[Purpur 1.21.4](${stats.versionUrl})", false)
            .addField("Plugins", "${stats.pluginCount}", true)
            .addField("Welten", "${stats.worldCount}", true)
            .addField("Chunks", "${stats.chunkCount}", true)
            .addField("Entities", "${stats.entityCount}", true)
            .addField("Durchschn. Latenz", "${stats.avgLatency} ms", true)
            .addField("Top-Spielzeit", stats.topPlaytime.joinToString("\n") { it.name }, false)
            .addField("Festplatte", "${stats.diskTotal / (1024*1024*1024)} GiB total / ${stats.diskFree / (1024*1024*1024)} GiB frei", false)
            .setFooter("PurpurStats • cancelcloud", null)
    }
}