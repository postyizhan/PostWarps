package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 消息动作处理器
 */
class MessageAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val message = extractActionValue(actionValue, ActionType.MESSAGE.prefix)
        logDebug("Sending message to player ${player.name}: $message")
        player.sendMessage(MessageUtil.color(message))
    }
}
