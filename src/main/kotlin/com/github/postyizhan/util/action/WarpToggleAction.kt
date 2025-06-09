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
        
        val data = plugin.getMenuManager().getPlayerData(player)
        val currentValue = data[key] as? Boolean ?: false
        data[key] = !currentValue
        
        logDebug("Toggled ${key} from ${currentValue} to ${!currentValue}")
        
        // 重新打开当前菜单
        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        plugin.getMenuManager().openMenu(player, currentMenu)
    }
}
