package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import org.bukkit.entity.Player

/**
 * 设置地标位置动作处理器
 */
class WarpSetLocationAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        logDebug("Player ${player.name} setting warp location")
        
        // 获取当前选中的地标ID
        val data = plugin.getMenuManager().getPlayerData(player)
        val warpId = data["warp_id"] as? Int ?: run {
            logDebug("No warp_id found in player data")
            sendMessage(player, "warp_location.no_warp_selected")
            return
        }
        
        // 从数据库中获取地标
        val warp = plugin.getDatabaseManager().getWarp(warpId) ?: run {
            logDebug("Warp with ID $warpId not found")
            sendMessage(player, "warp_location.not_found")
            return
        }
        
        // 检查是否是自己的地标
        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            sendMessage(player, "warp_location.no_permission")
            return
        }
        
        // 创建新的地标对象，更新位置
        @Suppress("UNUSED_VARIABLE")
        val newWarp = Warp(
            id = warp.id,
            name = warp.name,
            owner = warp.owner,
            ownerName = warp.ownerName,
            worldName = player.world.name,
            x = player.location.x,
            y = player.location.y,
            z = player.location.z,
            yaw = player.location.yaw,
            pitch = player.location.pitch,
            isPublic = warp.isPublic,
            description = warp.description,
            createTime = warp.createTime
        )
        
        // 更新地标位置
        val success = plugin.getDatabaseManager().updateWarpLocation(
            warp.id,
            player.world.name,
            player.location.x,
            player.location.y,
            player.location.z,
            player.location.yaw,
            player.location.pitch
        )
        
        if (success) {
            sendMessage(player, "warp_location.success")
        } else {
            sendMessage(player, "warp_location.failed")
        }
        
        // 关闭当前菜单
        player.closeInventory()
    }
}
