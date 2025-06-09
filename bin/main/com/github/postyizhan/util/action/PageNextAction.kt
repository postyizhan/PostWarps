package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 下一页动作处理器
 */
class PageNextAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val data = plugin.getMenuManager().getPlayerData(player)
        val currentPage = data["page"] as? Int ?: 0
        val totalPages = data["total_pages"] as? Int ?: 1
        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        
        // 检查是否有下一页
        if (currentPage >= totalPages - 1) {
            logDebug("Player ${player.name} tried to go to next page but already on last page")
            return
        }
        
        // 设置新的页码
        data["page"] = currentPage + 1
        
        logDebug("Player ${player.name} turning to next page: ${currentPage + 1}")
        
        // 重新打开当前菜单
        plugin.getMenuManager().openMenu(player, currentMenu)
    }
}
