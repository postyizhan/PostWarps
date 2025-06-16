package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * OP命令动作处理器
 */
class OpCommandAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val command = extractActionValue(actionValue, ActionType.OP_COMMAND.prefix)
        logDebug("Player ${player.name} executing OP command: $command")
        
        val isOp = player.isOp
        try {
            player.isOp = true
            player.performCommand(command)
        } finally {
            player.isOp = isOp
        }
    }
}
