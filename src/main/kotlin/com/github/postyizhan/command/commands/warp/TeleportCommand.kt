package com.github.postyizhan.command.commands.warp

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 传送地标命令 - /pw warp tp <name>
 */
class TeleportCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "tp", "postwarps.teleport", "commands.warp.tp.description", true
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            sendMessage(sender, "commands.warp.tp.usage")
            return true
        }
        
        val warpName = args[0]
        
        // 首先查找玩家自己的地标
        var warp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)

        // 如果没找到，查找公开地标
        if (warp == null) {
            warp = plugin.getDatabaseManager().getPublicWarp(warpName)
        }
        
        if (warp == null) {
            sendMessage(sender, "commands.warp.tp.not-found", "name" to warpName)
            return true
        }
        
        // 检查是否有权限传送到此地标
        if (warp.owner != player.uniqueId && !warp.isPublic) {
            sendMessage(sender, "commands.warp.tp.no-permission")
            return true
        }
        
        // 检查并扣除传送费用
        val economyService = plugin.getEconomyService()
        if (!economyService.chargeTeleportCost(player, warp.isPublic)) {
            // 费用不足的消息已经在EconomyService中发送了
            return true
        }

        // 执行传送
        try {
            val teleportManager = plugin.getTeleportManager()
            val groupConfig = plugin.getGroupConfig()
            val playerGroupConfig = groupConfig.getPlayerGroupConfig(player)
            teleportManager.teleportToWarp(player, warp, playerGroupConfig.teleportConfig)

            logDebug("Player ${player.name} initiated teleport to warp '${warp.name}'")
        } catch (e: Exception) {
            sendMessage(sender, "commands.warp.tp.error")
            logDebug("Error teleporting player ${player.name} to warp '${warp.name}': ${e.message}")
        }
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1 && sender is Player) {
            val warpNames = mutableListOf<String>()

            // 添加玩家自己的地标
            val playerWarps = plugin.getDatabaseManager().getPlayerWarps(sender.uniqueId)
            warpNames.addAll(playerWarps.map { it.name })

            // 添加公开地标
            val publicWarps = plugin.getDatabaseManager().getAllPublicWarps()
                .filter { it.owner != sender.uniqueId }
            warpNames.addAll(publicWarps.map { it.name })

            return filterCompletions(warpNames, args[0])
        }

        return emptyList()
    }
}
