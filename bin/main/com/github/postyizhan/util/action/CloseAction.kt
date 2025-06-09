package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 关闭菜单动作处理器
 */
class CloseAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        logDebug("Closing inventory for player ${player.name}")
        player.closeInventory()
    }
}
