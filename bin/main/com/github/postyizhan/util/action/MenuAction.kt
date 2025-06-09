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
        plugin.getMenuManager().openMenu(player, menuName)
    }
}
