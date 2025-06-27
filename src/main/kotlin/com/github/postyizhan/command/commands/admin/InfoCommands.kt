package com.github.postyizhan.command.commands.admin

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 管理员信息命令 - /pw admin info
 */
class InfoCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "info", "postwarps.admin", "commands.admin.info.description", false
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (!checkPermission(sender)) return true
        
        // 显示插件信息
        sendMessage(sender, "commands.admin.info.header")
        
        // 基本信息
        sendMessage(sender, "commands.admin.info.version", 
            "version" to plugin.description.version,
            "name" to plugin.description.name
        )
        
        // 数据库信息
        val databaseManager = plugin.getDatabaseManager()
        val totalWarps = databaseManager.getAllWarps().size
        val publicWarps = databaseManager.getAllPublicWarps().size
        val privateWarps = totalWarps - publicWarps
        
        sendMessage(sender, "commands.admin.info.database",
            "total" to totalWarps.toString(),
            "public" to publicWarps.toString(),
            "private" to privateWarps.toString()
        )
        
        // 在线玩家信息
        val onlinePlayers = Bukkit.getOnlinePlayers().size
        val maxPlayers = Bukkit.getMaxPlayers()
        
        sendMessage(sender, "commands.admin.info.players",
            "online" to onlinePlayers.toString(),
            "max" to maxPlayers.toString()
        )
        
        // 集成信息
        val vaultManager = plugin.getVaultManager()
        val economyStatus = if (vaultManager.hasEconomy()) "enabled" else "disabled"
        val permissionStatus = if (vaultManager.hasPermission()) "enabled" else "disabled"
        
        sendMessage(sender, "commands.admin.info.integrations",
            "economy" to economyStatus,
            "permission" to permissionStatus
        )
        
        // PlayerPoints信息
        val playerPointsManager = plugin.getPlayerPointsManager()
        val playerPointsStatus = if (playerPointsManager.isAvailable()) "enabled" else "disabled"

        sendMessage(sender, "commands.admin.info.playerpoints",
            "status" to playerPointsStatus
        )

        // PlaceholderAPI信息
        val placeholderAPIManager = plugin.getPlaceholderAPIManager()
        val placeholderAPIStatus = if (placeholderAPIManager.isAvailable()) "enabled" else "disabled"

        sendMessage(sender, "commands.admin.info.placeholderapi",
            "status" to placeholderAPIStatus
        )
        
        // 配置信息
        val config = plugin.getConfigManager().getConfig()
        val debugMode = if (plugin.isDebugEnabled()) "enabled" else "disabled"
        val language = config.getString("language", "zh_CN") ?: "unknown"

        sendMessage(sender, "commands.admin.info.config",
            "debug" to debugMode,
            "language" to language
        )
        
        sendMessage(sender, "commands.admin.info.footer")
        
        return true
    }
}
