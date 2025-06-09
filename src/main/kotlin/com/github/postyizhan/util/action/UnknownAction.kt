package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 未知动作处理器
 */
class UnknownAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        logDebug("Unknown action: $actionValue from player ${player.name}")
        sendMessage(player, "unknown_action", "action" to actionValue)
    }
}
