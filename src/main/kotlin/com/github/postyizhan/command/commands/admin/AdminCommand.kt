package com.github.postyizhan.command.commands.admin

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.base.AbstractSubCommand
import com.github.postyizhan.command.base.SubCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 管理员命令 - /pw admin
 * 这是一个隐藏的命令分组，不在主帮助中显示
 */
class AdminCommand(plugin: PostWarps) : AbstractSubCommand(
    plugin, "admin", "postwarps.admin", "commands.admin.description", false
) {
    
    private val subCommands = mapOf(
        "info" to InfoCommand(plugin),
        "economy" to EconomyCommand(plugin)
    )
    
    override fun getSubCommands(): Map<String, SubCommand> = subCommands
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (!checkPermission(sender)) return true
        
        if (args.isEmpty()) {
            showAdminHelp(sender)
            return true
        }
        
        val subCommandName = args[0].lowercase()
        val subCommand = subCommands[subCommandName]
        
        if (subCommand == null) {
            sendMessage(sender, "commands.admin.unknown-subcommand")
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
     * 显示管理员命令帮助
     */
    private fun showAdminHelp(sender: CommandSender) {
        sendMessage(sender, "help_admin.header")

        subCommands.values
            .filter { sender.hasPermission(it.getPermission()) }
            .filter { !it.isPlayerOnly() || sender is Player }
            .forEach { subCommand ->
                val helpKey = "help_admin.${subCommand.getName()}"
                sendMessage(sender, helpKey)
            }

        sendMessage(sender, "help_admin.footer")
    }
}
