package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 菜单动作处理器
 */
class MenuAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val menuName = extractActionValue(actionValue, ActionType.MENU.prefix)
        logDebug("Opening menu for player ${player.name}: $menuName")
        
        // 获取当前玩家数据
        val playerData = plugin.getMenuManager().getPlayerData(player)
        
        // 如果是打开创建菜单，设置默认值
        if (menuName == "create") {
            (playerData as? MutableMap<String, Any>)?.apply {
                // 设置默认的公开状态为false（私有）
                if (!containsKey("is_public")) {
                    put("is_public", false)
                }
            }
            logDebug("Player ${player.name} opening create menu, set default is_public=${playerData["is_public"]}")
        }

        // 如果是打开设置菜单，确保传递当前选中的地标ID
        if (menuName == "settings") {
            val warpId = playerData["warp_id"] as? Int
            
            if (warpId != null) {
                logDebug("Player ${player.name} opening settings menu for warp ID: $warpId")
                
                // 获取地标数据，添加到playerData
                val warp = plugin.getDatabaseManager().getWarp(warpId)
                if (warp != null) {
                    (playerData as? MutableMap<String, Any>)?.apply {
                        put("name", warp.name)
                        put("desc", warp.description)
                        put("is_public", warp.isPublic)
                    }
                    logDebug("Added warp data to player data: name=${warp.name}, desc=${warp.description}, public=${warp.isPublic}")
                }
            } else {
                logDebug("Warning: Opening settings menu but no warp_id found in player data")
            }
        }
        
        // 打开菜单
        plugin.getMenuManager().openMenu(player, menuName, playerData)
    }
}
