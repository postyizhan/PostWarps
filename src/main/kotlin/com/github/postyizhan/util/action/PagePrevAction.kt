package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 上一页动作处理器
 */
class PagePrevAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val data = plugin.getMenuManager().getPlayerData(player)
        val currentPage = data["page"] as? Int ?: 0
        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        
        // 确保页码不小于0
        if (currentPage <= 0) {
            logDebug("Player ${player.name} tried to go to previous page but already on first page")
            return
        }
        
        // 设置新的页码
        data["page"] = currentPage - 1
        logDebug("Player ${player.name} turning to previous page: ${currentPage - 1}")
        
        // 重新打开当前菜单
        plugin.getMenuManager().openMenu(player, currentMenu)
    }
}
