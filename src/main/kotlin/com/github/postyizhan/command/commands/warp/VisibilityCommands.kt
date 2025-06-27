package com.github.postyizhan.command.commands.warp

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 设置地标为公开命令 - /pw warp public <name>
 */
class PublicCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "public", "postwarps.public", "commands.warp.public.description", true
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            sendMessage(sender, "commands.warp.public.usage")
            return true
        }
        
        val warpName = args[0]
        
        // 查找地标
        val warp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)
        if (warp == null) {
            sendMessage(sender, "commands.warp.public.not-found", "name" to warpName)
            return true
        }
        
        // 检查是否为地标所有者
        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            sendMessage(sender, "commands.warp.public.no-permission")
            return true
        }
        
        // 检查地标是否已经是公开的
        if (warp.isPublic) {
            sendMessage(sender, "commands.warp.public.already-public", "name" to warpName)
            return true
        }
        
        try {
            val success = plugin.getDatabaseManager().setWarpPublic(warpName, player.uniqueId, true)
            
            if (success) {
                sendMessage(sender, "commands.warp.public.success", "name" to warpName)
                logDebug("Player ${player.name} set warp '$warpName' to public")
            } else {
                sendMessage(sender, "commands.warp.public.failed")
                logDebug("Failed to set warp '$warpName' to public for player ${player.name}")
            }
        } catch (e: Exception) {
            sendMessage(sender, "commands.warp.public.error")
            logDebug("Error setting warp '$warpName' to public for player ${player.name}: ${e.message}")
        }
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1 && sender is Player) {
            // 只显示玩家自己的私有地标
            val playerWarps = plugin.getDatabaseManager().getPlayerWarps(sender.uniqueId)
            val privateWarps = playerWarps.filter { !it.isPublic }
            val warpNames = privateWarps.map { it.name }
            return filterCompletions(warpNames, args[0])
        }

        return emptyList()
    }
}

/**
 * 设置地标为私有命令 - /pw warp private <name>
 */
class PrivateCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "private", "postwarps.private", "commands.warp.private.description", true
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            sendMessage(sender, "commands.warp.private.usage")
            return true
        }
        
        val warpName = args[0]
        
        // 查找地标
        val warp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)
        if (warp == null) {
            sendMessage(sender, "commands.warp.private.not-found", "name" to warpName)
            return true
        }
        
        // 检查是否为地标所有者
        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            sendMessage(sender, "commands.warp.private.no-permission")
            return true
        }
        
        // 检查地标是否已经是私有的
        if (!warp.isPublic) {
            sendMessage(sender, "commands.warp.private.already-private", "name" to warpName)
            return true
        }
        
        try {
            val success = plugin.getDatabaseManager().setWarpPublic(warpName, player.uniqueId, false)
            
            if (success) {
                sendMessage(sender, "commands.warp.private.success", "name" to warpName)
                logDebug("Player ${player.name} set warp '$warpName' to private")
            } else {
                sendMessage(sender, "commands.warp.private.failed")
                logDebug("Failed to set warp '$warpName' to private for player ${player.name}")
            }
        } catch (e: Exception) {
            sendMessage(sender, "commands.warp.private.error")
            logDebug("Error setting warp '$warpName' to private for player ${player.name}: ${e.message}")
        }
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1 && sender is Player) {
            // 只显示玩家自己的公开地标
            val playerWarps = plugin.getDatabaseManager().getPlayerWarps(sender.uniqueId)
            val publicWarps = playerWarps.filter { it.isPublic }
            val warpNames = publicWarps.map { it.name }
            return filterCompletions(warpNames, args[0])
        }

        return emptyList()
    }
}
