package com.github.postyizhan.command.commands.warp

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import com.github.postyizhan.command.base.SubCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Warp主命令 - 处理 /pw warp 命令
 */
class WarpCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "warp", "postwarps.warp", "commands.warp.description", true
) {
    
    private val subCommands = mapOf(
        "create" to CreateCommand(plugin),
        "delete" to DeleteCommand(plugin),
        "edit" to EditCommand(plugin),
        "list" to ListCommand(plugin),
        "tp" to TeleportCommand(plugin),
        "info" to InfoCommand(plugin),
        "public" to PublicCommand(plugin),
        "private" to PrivateCommand(plugin)
    )
    
    override fun getSubCommands(): Map<String, SubCommand> = subCommands
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            showWarpHelp(sender)
            return true
        }
        
        val subCommandName = args[0].lowercase()
        val subCommand = subCommands[subCommandName]
        
        if (subCommand == null) {
            sendMessage(sender, "commands.warp.unknown-subcommand")
            return true
        }
        
        // 检查子命令权限
        if (!sender.hasPermission(subCommand.getPermission())) {
            sendMessage(sender, "general.no-permission")
            return true
        }
        
        // 执行子命令
        val subArgs = if (args.size > 1) args.sliceArray(1 until args.size) else emptyArray()
        return subCommand.execute(sender, subArgs)
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.isEmpty()) {
            return getAvailableSubCommands(sender)
        }
        
        if (args.size == 1) {
            return filterCompletions(getAvailableSubCommands(sender), args[0])
        }
        
        // 子命令的Tab补全
        val subCommandName = args[0].lowercase()
        val subCommand = subCommands[subCommandName]
        
        if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
            val subArgs = args.sliceArray(1 until args.size)
            return subCommand.tabComplete(sender, subArgs)
        }
        
        return emptyList()
    }
    
    /**
     * 获取可用的子命令
     */
    private fun getAvailableSubCommands(sender: CommandSender): List<String> {
        return subCommands.values
            .filter { sender.hasPermission(it.getPermission()) }
            .filter { !it.isPlayerOnly() || sender is Player }
            .map { it.getName() }
    }
    
    /**
     * 显示Warp命令帮助
     */
    private fun showWarpHelp(sender: CommandSender) {
        sendMessage(sender, "help_warp.header")

        subCommands.values
            .filter { sender.hasPermission(it.getPermission()) }
            .filter { !it.isPlayerOnly() || sender is Player }
            .forEach { subCommand ->
                val helpKey = "help_warp.${subCommand.getName()}"
                sendMessage(sender, helpKey)
            }

        sendMessage(sender, "help_warp.footer")
    }
}
