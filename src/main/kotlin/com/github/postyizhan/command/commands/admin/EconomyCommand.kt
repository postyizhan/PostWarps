package com.github.postyizhan.command.commands.admin

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 管理员经济命令 - /pw admin economy <player>
 */
class EconomyCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "economy", "postwarps.admin", "commands.admin.economy.description", false
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            sendMessage(sender, "commands.admin.economy.usage")
            return true
        }
        
        val targetPlayerName = args[0]
        val targetPlayer = Bukkit.getPlayer(targetPlayerName)
        
        if (targetPlayer == null) {
            sendMessage(sender, "commands.admin.economy.player-not-found", "player" to targetPlayerName)
            return true
        }
        
        val economyService = plugin.getEconomyService()
        
        // 显示玩家经济信息
        sendMessage(sender, "commands.admin.economy.header", "player" to targetPlayer.name)
        
        // 显示余额信息
        if (economyService.isAvailable(targetPlayer)) {
            val balanceInfo = economyService.getBalanceInfo(targetPlayer)
            sendMessage(sender, "commands.admin.economy.balance",
                "balance" to balanceInfo
            )

            // 显示费用信息
            val costInfo = economyService.getCostInfo(targetPlayer)
            costInfo.forEach { info ->
                sender.sendMessage(com.github.postyizhan.util.MessageUtil.color(info))
            }
        } else {
            sendMessage(sender, "commands.admin.economy.disabled")
        }
        
        // 显示PlayerPoints信息
        val playerPointsManager = plugin.getPlayerPointsManager()
        if (playerPointsManager.isAvailable()) {
            val points = playerPointsManager.getBalance(targetPlayer)
            sendMessage(sender, "commands.admin.economy.playerpoints",
                "points" to points.toString()
            )
        } else {
            sendMessage(sender, "commands.admin.economy.playerpoints-disabled")
        }

        // 显示权限组信息
        val groupConfig = plugin.getGroupConfig()
        val playerGroupConfig = groupConfig.getPlayerGroupConfig(targetPlayer)
        sendMessage(sender, "commands.admin.economy.group",
            "group" to playerGroupConfig.groupName
        )

        // 显示传送配置
        val teleportConfig = playerGroupConfig.teleportConfig
        sendMessage(sender, "commands.admin.economy.teleport-config",
            "delay" to teleportConfig.delay.toString(),
            "cancel_on_move" to teleportConfig.cancelOnMove.toString(),
            "cancel_on_damage" to teleportConfig.cancelOnDamage.toString()
        )
        
        sendMessage(sender, "commands.admin.economy.footer")
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            // 提供在线玩家名称补全
            val playerNames = Bukkit.getOnlinePlayers().map { it.name }
            return filterCompletions(playerNames, args[0])
        }
        
        return emptyList()
    }
}
