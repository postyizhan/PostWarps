package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 删除地标动作处理器
 */
class WarpDeleteAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val name = extractActionValue(actionValue, ActionType.WARP_DELETE.prefix)
        logDebug("Player ${player.name} deleting warp: $name")
        
        // 从数据库中获取地标
        val warp = if (name.isEmpty()) {
            val data = plugin.getMenuManager().getPlayerData(player)
            val warpId = data["warp_id"] as? Int ?: run {
                logDebug("No warp_id found in player data")
                return
            }
            plugin.getDatabaseManager().getWarp(warpId)
        } else {
            plugin.getDatabaseManager().getWarp(name, player.uniqueId)
        }
        
        if (warp == null) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("delete.not-found")
                    .replace("{name}", if (name.isEmpty()) "selected warp" else name)
            ))
            return
        }
        
        // 检查是否是自己的地标
        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("delete.not-owner")
            ))
            return
        }
        
        logDebug("Deleting warp ID: ${warp.id}, name: ${warp.name}")
        
        // 删除地标
        val success = plugin.getDatabaseManager().deleteWarp(warp.id)
        if (success) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("delete.success")
                    .replace("{name}", warp.name)
            ))
            
            // 关闭菜单
            player.closeInventory()
        } else {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("delete.failed")
            ))
        }
    }
}
