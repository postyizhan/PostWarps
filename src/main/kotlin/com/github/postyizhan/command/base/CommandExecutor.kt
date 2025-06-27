package com.github.postyizhan.command.base

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * 命令执行器基类
 */
abstract class CommandExecutor(
    protected val plugin: PostWarps
) : org.bukkit.command.CommandExecutor, TabCompleter {
    
    protected val subCommands = mutableMapOf<String, SubCommand>()
    
    /**
     * 注册子命令
     */
    protected fun registerSubCommand(subCommand: SubCommand) {
        subCommands[subCommand.getName().lowercase()] = subCommand
    }
    
    /**
     * 获取所有子命令
     */
    protected fun getAllSubCommands(): Map<String, SubCommand> = subCommands
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        logDebug("Command executed: ${command.name}, args: ${args.joinToString(" ")}, sender: ${sender.name}")
        
        // 如果没有参数，显示帮助
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        // 查找子命令
        val subCommandName = args[0].lowercase()
        val subCommand = subCommands[subCommandName]
        
        if (subCommand == null) {
            sendMessage(sender, "general.unknown-command")
            return true
        }
        
        // 检查权限
        if (!sender.hasPermission(subCommand.getPermission())) {
            sendMessage(sender, "general.no-permission")
            return true
        }
        
        // 检查是否只允许玩家执行
        if (subCommand.isPlayerOnly() && sender !is Player) {
            sendMessage(sender, "general.player-only")
            return true
        }
        
        // 执行子命令
        val subArgs = if (args.size > 1) {
            Array(args.size - 1) { args[it + 1] }
        } else {
            emptyArray()
        }
        return subCommand.execute(sender, subArgs)
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.isEmpty()) {
            return getAvailableCommands(sender)
        }
        
        if (args.size == 1) {
            // 第一级命令补全
            return filterCompletions(getAvailableCommands(sender), args[0])
        }
        
        // 子命令补全
        val subCommandName = args[0].lowercase()
        val subCommand = subCommands[subCommandName]
        
        if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
            val subArgs = Array(args.size - 1) { args[it + 1] }
            return subCommand.tabComplete(sender, subArgs)
        }
        
        return emptyList()
    }
    
    /**
     * 获取可用命令列表
     */
    private fun getAvailableCommands(sender: CommandSender): List<String> {
        return subCommands.values
            .filter { sender.hasPermission(it.getPermission()) }
            .filter { !it.isPlayerOnly() || sender is Player }
            .map { it.getName() }
    }
    
    /**
     * 显示帮助信息
     */
    protected abstract fun showHelp(sender: CommandSender)
    
    /**
     * 发送消息
     */
    protected fun sendMessage(sender: CommandSender, key: String, vararg replacements: Pair<String, String>) {
        val message = if (sender is Player) {
            MessageUtil.getMessage(key, sender)
        } else {
            MessageUtil.getMessage(key)
        }
        
        var processedMessage = message
        replacements.forEach { (placeholder, value) ->
            processedMessage = processedMessage.replace("{$placeholder}", value)
        }
        
        sender.sendMessage(MessageUtil.color(processedMessage))
    }
    
    /**
     * 过滤补全
     */
    protected fun filterCompletions(completions: List<String>, arg: String): List<String> {
        return completions.filter { it.lowercase().startsWith(arg.lowercase()) }
    }
    
    /**
     * 记录调试日志
     */
    protected fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] $message")
        }
    }
}
