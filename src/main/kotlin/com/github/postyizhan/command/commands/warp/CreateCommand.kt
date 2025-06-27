package com.github.postyizhan.command.commands.warp

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import com.github.postyizhan.model.Warp
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

/**
 * 创建地标命令 - /pw warp create <name> [description]
 */
class CreateCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "create", "postwarps.create", "commands.warp.create.description", true
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            sendMessage(sender, "commands.warp.create.usage")
            return true
        }
        
        val warpName = args[0]
        val description = if (args.size > 1) args.sliceArray(1 until args.size).joinToString(" ") else ""
        
        // 检查地标名称是否已存在
        val existingWarp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)
        if (existingWarp != null) {
            sendMessage(sender, "commands.warp.create.name-exists", "name" to warpName)
            return true
        }
        
        // 检查玩家地标数量限制
        val playerWarps = plugin.getDatabaseManager().getPlayerWarps(player.uniqueId)
        val maxWarps = getMaxWarps(player)
        
        if (playerWarps.size >= maxWarps) {
            sendMessage(sender, "commands.warp.create.limit-reached", "max" to maxWarps.toString())
            return true
        }
        
        // 创建地标
        val warp = Warp(
            id = 0, // 数据库会自动分配ID
            name = warpName,
            owner = player.uniqueId,
            ownerName = player.name,
            worldName = player.world.name,
            x = player.location.x,
            y = player.location.y + 1.0, // Y轴坐标+1避免卡到地里
            z = player.location.z,
            yaw = player.location.yaw,
            pitch = player.location.pitch,
            description = description,
            isPublic = false,
            createTime = System.currentTimeMillis()
        )
        
        try {
            val success = plugin.getDatabaseManager().createWarp(warp)
            if (success) {
                sendMessage(sender, "commands.warp.create.success", "name" to warpName)
                logDebug("Player ${player.name} created warp '$warpName' at ${player.location}")
            } else {
                sendMessage(sender, "commands.warp.create.failed")
                logDebug("Failed to create warp '$warpName' for player ${player.name}")
            }
        } catch (e: Exception) {
            sendMessage(sender, "commands.warp.create.error")
            logDebug("Error creating warp '$warpName' for player ${player.name}: ${e.message}")
        }
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        // 第一个参数是地标名称，不提供补全
        // 第二个及以后的参数是描述，不提供补全
        return emptyList()
    }
    
    /**
     * 获取玩家最大地标数量
     */
    private fun getMaxWarps(player: Player): Int {
        // 检查权限节点获取最大地标数量
        for (i in 100 downTo 1) {
            if (player.hasPermission("postwarps.limit.$i")) {
                return i
            }
        }
        
        // 默认最大地标数量
        return plugin.getConfigManager().getConfig().getInt("default-max-warps", 10)
    }
}
