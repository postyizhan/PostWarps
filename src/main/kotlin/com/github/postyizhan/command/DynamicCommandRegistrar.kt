package com.github.postyizhan.command

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.lang.reflect.Field

/**
 * 动态命令注册器
 * 根据菜单配置文件中的open_command节点自动注册命令
 */
class DynamicCommandRegistrar(private val plugin: PostWarps) {
    
    private val registeredCommands = mutableMapOf<String, String>() // 命令名 -> 菜单名
    
    /**
     * 扫描所有菜单配置并注册命令
     */
    fun registerMenuCommands() {
        val menuNames = listOf("main", "private_warps", "public_warps", "settings", "create")

        for (menuName in menuNames) {
            try {
                registerCommandsForMenu(menuName)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to register commands for menu $menuName: ${e.message}")
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace()
                }
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] Registered ${registeredCommands.size} dynamic menu commands: ${registeredCommands.keys}")
        }
    }
    
    /**
     * 为指定菜单注册命令
     */
    private fun registerCommandsForMenu(menuName: String) {
        try {
            // 直接从文件加载菜单配置
            val menuFile = java.io.File(plugin.dataFolder, "menu/$menuName.yml")
            if (!menuFile.exists()) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] Menu file not found: $menuName.yml")
                }
                return
            }

            val config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(menuFile)
            val openCommands = config.getStringList("open_command")

            if (openCommands.isNotEmpty()) {
                for (commandName in openCommands) {
                    registerDynamicCommand(commandName, menuName)
                }

                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] Found ${openCommands.size} commands for menu $menuName: $openCommands")
                }
            } else {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] No open_command found for menu $menuName")
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load config for menu $menuName: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 注册动态命令
     */
    private fun registerDynamicCommand(commandName: String, menuName: String) {
        try {
            // 检查命令是否已经注册
            if (registeredCommands.containsKey(commandName)) {
                plugin.logger.warning("Command '$commandName' is already registered for menu '${registeredCommands[commandName]}', skipping registration for menu '$menuName'")
                return
            }
            
            // 创建命令执行器
            val executor = MenuCommandExecutor(plugin, menuName)
            
            // 获取服务器的命令映射
            val server = plugin.server
            val commandMapField = server.javaClass.getDeclaredField("commandMap")
            commandMapField.isAccessible = true
            val commandMap = commandMapField.get(server) as org.bukkit.command.CommandMap
            
            // 创建命令对象
            val command = DynamicMenuCommand(commandName, plugin, menuName, executor)
            
            // 注册命令
            commandMap.register(plugin.description.name.lowercase(), command)
            registeredCommands[commandName] = menuName
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Registered dynamic command '$commandName' for menu '$menuName'")
            }
            
        } catch (e: Exception) {
            plugin.logger.warning("Failed to register command '$commandName' for menu '$menuName': ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 注销所有动态注册的命令
     */
    fun unregisterCommands() {
        try {
            val server = plugin.server
            val commandMapField = server.javaClass.getDeclaredField("commandMap")
            commandMapField.isAccessible = true
            val commandMap = commandMapField.get(server) as org.bukkit.command.CommandMap
            
            // 获取已知命令映射
            val knownCommandsField = commandMap.javaClass.getDeclaredField("knownCommands")
            knownCommandsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val knownCommands = knownCommandsField.get(commandMap) as MutableMap<String, Command>
            
            // 移除注册的命令
            for (commandName in registeredCommands.keys) {
                knownCommands.remove(commandName)
                knownCommands.remove("${plugin.description.name.lowercase()}:$commandName")
            }
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Unregistered ${registeredCommands.size} dynamic commands")
            }
            
            registeredCommands.clear()
            
        } catch (e: Exception) {
            plugin.logger.warning("Failed to unregister dynamic commands: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 获取已注册的命令列表
     */
    fun getRegisteredCommands(): Map<String, String> {
        return registeredCommands.toMap()
    }
}

/**
 * 动态菜单命令类
 */
class DynamicMenuCommand(
    name: String,
    private val plugin: PostWarps,
    private val menuName: String,
    private val commandExecutor: MenuCommandExecutor
) : Command(name) {

    init {
        description = "Open $menuName menu"
        usage = "/$name"
        permission = "postwarps.menu.$menuName"
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        return commandExecutor.onCommand(sender, this, commandLabel, args)
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
        return commandExecutor.onTabComplete(sender, this, alias, args)
    }
}

/**
 * 菜单命令执行器
 */
class MenuCommandExecutor(
    private val plugin: PostWarps,
    private val menuName: String
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("general.player-only")))
            return true
        }
        
        // 检查权限
        val permission = "postwarps.menu.$menuName"
        if (!sender.hasPermission(permission) && !sender.hasPermission("postwarps.admin")) {
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("general.no-permission", sender)))
            return true
        }
        
        // 打开菜单
        try {
            plugin.getMenuManager().openMenu(sender, menuName)
            
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Player ${sender.name} opened menu '$menuName' via command '/$label'")
            }
            
        } catch (e: Exception) {
            sender.sendMessage(MessageUtil.color("&c打开菜单时发生错误，请联系管理员"))
            plugin.logger.warning("Failed to open menu '$menuName' for player ${sender.name}: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
        
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        // 菜单命令通常不需要参数补全
        return emptyList()
    }
}
