package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * 控制台命令动作处理器
 */
class ConsoleCommandAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val command = extractActionValue(actionValue, ActionType.CONSOLE_COMMAND.prefix)
        logDebug("Executing console command for player ${player.name}: $command")
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }
}
