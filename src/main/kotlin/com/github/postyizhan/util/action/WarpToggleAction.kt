package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 切换地标数据动作处理器
 */
class WarpToggleAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val playerData = plugin.getMenuManager().getPlayerData(player)
        val warpId = playerData["warp_id"] as? Int

        val newState = if (warpId != null) {
            // 编辑现有地标
            toggleExistingWarp(player, warpId)
        } else {
            // 创建菜单中的状态切换
            toggleMenuState(playerData)
        } ?: return

        // 更新数据并刷新菜单
        (playerData as? MutableMap<String, Any>)?.put("is_public", newState)
        refreshMenu(player)
    }

    private fun toggleExistingWarp(player: Player, warpId: Int): Boolean? {
        val warp = plugin.getDatabaseManager().getWarp(warpId) ?: run {
            sendMessage(player, "warp_toggle.not_found")
            return null
        }

        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            sendMessage(player, "warp_toggle.no_permission")
            return null
        }

        val newState = !warp.isPublic
        return if (plugin.getDatabaseManager().setWarpPublic(warpId, newState)) {
            sendMessage(player, if (newState) "warp_toggle.success_public" else "warp_toggle.success_private")
            newState
        } else {
            sendMessage(player, "warp_toggle.failed")
            null
        }
    }

    private fun toggleMenuState(playerData: Map<String, Any>): Boolean {
        return !(playerData["is_public"] as? Boolean ?: false)
    }

    private fun refreshMenu(player: Player) {
        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        plugin.getMenuManager().openMenu(player, currentMenu)
    }
}
