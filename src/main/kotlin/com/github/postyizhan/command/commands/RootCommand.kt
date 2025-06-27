package com.github.postyizhan.command.commands

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.LanguageCommand
import com.github.postyizhan.command.base.CommandExecutor
import com.github.postyizhan.command.commands.admin.AdminCommand
import com.github.postyizhan.command.commands.warp.WarpCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 根命令执行器 - 处理 /pw 命令
 */
class RootCommand(plugin: PostWarps) : CommandExecutor(plugin) {
    
    init {
        // 注册子命令
        registerSubCommand(WarpCommand(plugin))
        registerSubCommand(LanguageCommandAdapter(plugin))
        registerSubCommand(MenuCommand(plugin))
        registerSubCommand(VersionCommand(plugin))
        registerSubCommand(ReloadCommand(plugin))
        registerSubCommand(AdminCommand(plugin))
    }
    
    override fun showHelp(sender: CommandSender) {
        sendMessage(sender, "help.header")
        
        // 显示可用的子命令
        getAllSubCommands().values
            .filter { sender.hasPermission(it.getPermission()) }
            .filter { !it.isPlayerOnly() || sender is Player }
            .forEach { subCommand ->
                val helpKey = "help.${subCommand.getName()}"
                sendMessage(sender, helpKey)
            }
        
        sendMessage(sender, "help.footer")
    }
}



/**
 * 菜单命令
 */
class MenuCommand(plugin: PostWarps) : com.github.postyizhan.command.base.AbstractSubCommand(
    plugin, "menu", "postwarps.menu", "commands.menu.description", true
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        val player = checkPlayer(sender) ?: return true
        if (!checkPermission(sender)) return true
        
        val menuName = if (args.isNotEmpty()) args[0] else "main"
        
        try {
            plugin.getMenuManager().openMenu(player, menuName)
            logDebug("Player ${player.name} opened menu '$menuName'")
        } catch (e: Exception) {
            sendMessage(sender, "commands.menu.error")
            logDebug("Failed to open menu '$menuName' for player ${player.name}: ${e.message}")
        }
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            return filterCompletions(listOf("main", "create", "public_warps", "private_warps", "settings"), args[0])
        }
        return emptyList()
    }
}

/**
 * 版本命令
 */
class VersionCommand(plugin: PostWarps) : com.github.postyizhan.command.base.AbstractSubCommand(
    plugin, "version", "postwarps.version", "commands.version.description", false
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (!checkPermission(sender)) return true
        
        sendMessage(sender, "commands.version.info", 
            "version" to plugin.description.version,
            "name" to plugin.description.name
        )
        
        return true
    }
}

/**
 * 重载命令
 */
class ReloadCommand(plugin: PostWarps) : com.github.postyizhan.command.base.AbstractSubCommand(
    plugin, "reload", "postwarps.admin", "commands.reload.description", false
) {
    
    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        if (!checkPermission(sender)) return true
        
        try {
            plugin.reload()
            sendMessage(sender, "commands.reload.success")
            logDebug("Plugin reloaded by ${sender.name}")
        } catch (e: Exception) {
            sendMessage(sender, "commands.reload.error")
            logDebug("Failed to reload plugin: ${e.message}")
        }
        
        return true
    }
}

/**
 * LanguageCommand 适配器 - 将独立的 LanguageCommand 适配为 SubCommand
 */
class LanguageCommandAdapter(plugin: PostWarps) : com.github.postyizhan.command.base.AbstractSubCommand(
    plugin, "language", "postwarps.language", "commands.language.description", true
) {

    private val languageCommand = LanguageCommand(plugin)

    override fun execute(sender: CommandSender, args: Array<String>): Boolean {
        return languageCommand.onCommand(sender,
            object : org.bukkit.command.Command("language") {
                override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                    return false
                }
            },
            "language",
            args
        )
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        return languageCommand.onTabComplete(sender,
            object : org.bukkit.command.Command("language") {
                override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                    return false
                }
            },
            "language",
            args
        )
    }
}
