package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 切换地标数据动作处理器
 */
class WarpToggleAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val key = extractActionValue(actionValue, ActionType.WARP_TOGGLE.prefix)
        logDebug("Player ${player.name} toggling warp data for key: $key")
        
        // 如果是公开/私有切换，需要更新数据库
        if (key == "public") {
            // 获取当前地标ID
            val playerData = plugin.getMenuManager().getPlayerData(player)
            val warpId = playerData["warp_id"] as? Int
            
            if (warpId != null) {
                // 获取地标
                val warp = plugin.getDatabaseManager().getWarp(warpId)
                
                if (warp != null) {
                    // 检查是否是自己的地标
                    if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
                        sendMessage(player, "warp_toggle.no_permission")
                        return
                    }
                    
                    // 切换状态
                    val newState = !warp.isPublic
                    
                    // 更新数据库
                    val success = plugin.getDatabaseManager().setWarpPublic(warpId, newState)
                    
                    if (success) {
                        // 更新内存中的数据
                        (playerData as? MutableMap<String, Any>)?.put("public", newState)
                        
                        // 显示确认消息
                        if (newState) {
                            sendMessage(player, "warp_toggle.success_public")
                        } else {
                            sendMessage(player, "warp_toggle.success_private")
                        }
                        
                        logDebug("Updated warp ID: $warpId public state to: $newState")
                    } else {
                        sendMessage(player, "warp_toggle.failed")
                        logDebug("Failed to update warp ID: $warpId public state")
                    }
                } else {
                    sendMessage(player, "warp_toggle.not_found")
                    return
                }
            } else {
                logDebug("No warp_id found in player data")
                // 仅切换菜单显示状态
                val data = plugin.getMenuManager().getPlayerData(player)
                val currentValue = data[key] as? Boolean ?: false
                data[key] = !currentValue
            }
        } else {
            // 切换其他菜单状态
            val data = plugin.getMenuManager().getPlayerData(player)
            val currentValue = data[key] as? Boolean ?: false
            data[key] = !currentValue
            
            logDebug("Toggled ${key} from ${currentValue} to ${!currentValue}")
        }
        
        // 重新打开当前菜单
        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        plugin.getMenuManager().openMenu(player, currentMenu)
    }
}
