package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

abstract class AbstractAction(protected val plugin: PostWarps) : Action {

    protected fun extractActionValue(action: String, prefix: String): String {
        return action.substring(prefix.length).trim()
    }
    

    protected fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] $message")
        }
    }
    

    protected fun logWarning(message: String) {
        plugin.logger.warning(message)
    }
    

    protected fun getMessage(player: Player, key: String, vararg replacements: Pair<String, String>): String {
        var message = MessageUtil.getMessage("actions.$key", player)
        replacements.forEach { (placeholder, value) ->
            message = message.replace("{$placeholder}", value)
        }
        return MessageUtil.color(message)
    }


    protected fun sendMessage(player: Player, key: String, vararg replacements: Pair<String, String>) {
        player.sendMessage(getMessage(player, key, *replacements))
    }


}
