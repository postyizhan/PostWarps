package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 命令动作处理器
 */
class CommandAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val command = extractActionValue(actionValue, ActionType.COMMAND.prefix)
        logDebug("Player ${player.name} executing command: $command")
        player.performCommand(command)
    }
}
