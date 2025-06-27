package com.github.postyizhan.command.commands.warp

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 删除地标命令 - /pw warp delete <name>
 */
class DeleteCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "delete", "postwarps.delete", "commands.warp.delete.description", true
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            sendMessage(sender, "commands.warp.delete.usage")
            return true
        }
        
        val warpName = args[0]
        
        // 查找地标
        val warp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)
        if (warp == null) {
            sendMessage(sender, "commands.warp.delete.not-found", "name" to warpName)
            return true
        }
        
        // 检查是否为地标所有者或管理员
        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            sendMessage(sender, "commands.warp.delete.no-permission")
            return true
        }
        
        try {
            val success = plugin.getDatabaseManager().deleteWarp(warp.id)
            if (success) {
                sendMessage(sender, "commands.warp.delete.success", "name" to warpName)
                logDebug("Player ${player.name} deleted warp '$warpName' (ID: ${warp.id})")
            } else {
                sendMessage(sender, "commands.warp.delete.failed")
                logDebug("Failed to delete warp '$warpName' for player ${player.name}")
            }
        } catch (e: Exception) {
            sendMessage(sender, "commands.warp.delete.error")
            logDebug("Error deleting warp '$warpName' for player ${player.name}: ${e.message}")
        }
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1 && sender is Player) {
            // 只显示玩家自己的地标
            val playerWarps = plugin.getDatabaseManager().getPlayerWarps(sender.uniqueId)
            val warpNames = playerWarps.map { it.name }
            return filterCompletions(warpNames, args[0])
        }

        return emptyList()
    }
}
