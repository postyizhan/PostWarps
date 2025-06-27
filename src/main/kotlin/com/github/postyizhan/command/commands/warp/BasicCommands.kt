package com.github.postyizhan.command.commands.warp

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import com.github.postyizhan.model.Warp
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.*

/**
 * 编辑地标命令 - /pw warp edit <name> <description>
 */
class EditCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "edit", "postwarps.edit", "commands.warp.edit.description", true
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        if (args.size < 2) {
            sendMessage(sender, "commands.warp.edit.usage")
            return true
        }
        
        val warpName = args[0]
        val newDescription = args.sliceArray(1 until args.size).joinToString(" ")
        
        // 查找地标
        val warp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)
        if (warp == null) {
            sendMessage(sender, "commands.warp.edit.not-found", "name" to warpName)
            return true
        }

        // 检查是否为地标所有者
        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            sendMessage(sender, "commands.warp.edit.no-permission")
            return true
        }

        try {
            val success = plugin.getDatabaseManager().updateWarpDescription(warpName, player.uniqueId, newDescription)
            
            if (success) {
                sendMessage(sender, "commands.warp.edit.success", "name" to warpName)
                logDebug("Player ${player.name} edited warp '$warpName' description")
            } else {
                sendMessage(sender, "commands.warp.edit.failed")
                logDebug("Failed to edit warp '$warpName' for player ${player.name}")
            }
        } catch (e: Exception) {
            sendMessage(sender, "commands.warp.edit.error")
            logDebug("Error editing warp '$warpName' for player ${player.name}: ${e.message}")
        }
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1 && sender is Player) {
            val playerWarps = plugin.getDatabaseManager().getPlayerWarps(sender.uniqueId)
            val warpNames = playerWarps.map { it.name }
            return filterCompletions(warpNames, args[0])
        }

        return emptyList()
    }
}

/**
 * 列出地标命令 - /pw warp list [page]
 */
class ListCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "list", "postwarps.list", "commands.warp.list.description", true
) {
    
    private val warpsPerPage = 10
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        val page = if (args.isNotEmpty()) {
            args[0].toIntOrNull() ?: 1
        } else 1
        
        if (page < 1) {
            sendMessage(sender, "commands.warp.list.invalid-page")
            return true
        }
        
        val playerWarps = plugin.getDatabaseManager().getPlayerWarps(player.uniqueId)
        
        if (playerWarps.isEmpty()) {
            sendMessage(sender, "commands.warp.list.no-warps")
            return true
        }
        
        val totalPages = (playerWarps.size + warpsPerPage - 1) / warpsPerPage
        
        if (page > totalPages) {
            sendMessage(sender, "commands.warp.list.page-not-found", "max" to totalPages.toString())
            return true
        }
        
        val startIndex = (page - 1) * warpsPerPage
        val endIndex = minOf(startIndex + warpsPerPage, playerWarps.size)
        val warpsOnPage = playerWarps.subList(startIndex, endIndex)
        
        // 显示列表头部
        sendMessage(sender, "commands.warp.list.header", 
            "page" to page.toString(), 
            "total" to totalPages.toString()
        )
        
        // 显示地标列表
        warpsOnPage.forEach { warp ->
            val visibility = if (warp.isPublic) "public" else "private"
            sendMessage(sender, "commands.warp.list.item",
                "name" to warp.name,
                "world" to warp.worldName,
                "visibility" to visibility,
                "description" to (warp.description.ifEmpty { "无描述" })
            )
        }
        
        // 显示列表底部
        sendMessage(sender, "commands.warp.list.footer")
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            // 提供页码补全
            return filterCompletions((1..10).map { it.toString() }, args[0])
        }
        
        return emptyList()
    }
}

/**
 * 地标信息命令 - /pw warp info <name>
 */
class InfoCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "info", "postwarps.info", "commands.warp.info.description", true
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            sendMessage(sender, "commands.warp.info.usage")
            return true
        }
        
        val warpName = args[0]
        
        // 查找地标
        var warp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)

        // 如果没找到，查找公开地标
        if (warp == null) {
            warp = plugin.getDatabaseManager().getPublicWarp(warpName)
        }
        
        if (warp == null) {
            sendMessage(sender, "commands.warp.info.not-found", "name" to warpName)
            return true
        }
        
        // 显示地标信息
        sendMessage(sender, "commands.warp.info.header", "name" to warp.name)
        sendMessage(sender, "commands.warp.info.owner", "owner" to warp.ownerName)
        sendMessage(sender, "commands.warp.info.world", "world" to warp.worldName)
        sendMessage(sender, "commands.warp.info.coordinates", 
            "x" to String.format("%.1f", warp.x),
            "y" to String.format("%.1f", warp.y),
            "z" to String.format("%.1f", warp.z)
        )
        
        val visibility = if (warp.isPublic) "public" else "private"
        sendMessage(sender, "commands.warp.info.visibility", "visibility" to visibility)
        
        val description = warp.description.ifEmpty { "无描述" }
        sendMessage(sender, "commands.warp.info.warp-description", "description" to description)
        
        val createTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(warp.createTime))
        sendMessage(sender, "commands.warp.info.created", "time" to createTime)
        
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
