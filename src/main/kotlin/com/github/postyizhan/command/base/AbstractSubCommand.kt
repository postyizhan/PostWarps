package com.github.postyizhan.command.base

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 抽象子命令基类
 */
abstract class AbstractSubCommand(
    protected val plugin: PostWarps,
    private val name: String,
    private val permission: String,
    private val description: String,
    private val playerOnly: Boolean = true
) : SubCommand {
    
    override fun getName(): String = name
    
    override fun getPermission(): String = permission
    
    override fun getDescription(): String = description
    
    override fun isPlayerOnly(): Boolean = playerOnly
    
    /**
     * 检查权限
     */
    protected fun checkPermission(sender: CommandSender): Boolean {
        if (!sender.hasPermission(getPermission())) {
            sendMessage(sender, "general.no-permission")
            return false
        }
        return true
    }
    
    /**
     * 检查是否为玩家
     */
    protected fun checkPlayer(sender: CommandSender): Player? {
        if (isPlayerOnly() && sender !is Player) {
            sendMessage(sender, "general.player-only")
            return null
        }
        return sender as? Player
    }
    
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
     * 过滤Tab补全
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
