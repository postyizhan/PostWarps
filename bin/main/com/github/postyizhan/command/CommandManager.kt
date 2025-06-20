package com.github.postyizhan.command

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*

/**
 * 命令管理器，处理插件命令
 */
class CommandManager(private val plugin: PostWarps) : CommandExecutor, TabCompleter {
    
    /**
     * 注册命令
     */
    fun registerCommands() {
        val pluginCommand = plugin.getCommand("postwarps")
        pluginCommand?.setExecutor(this)
        pluginCommand?.tabCompleter = this
    }
    
    /**
     * 处理命令执行
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // 检查命令是否是插件命令
        if (command.name.equals("postwarps", ignoreCase = true)) {
            // 如果没有参数，显示帮助信息
            if (args.isEmpty()) {
                showHelp(sender)
                return true
            }
            
            // 根据子命令处理
            when (args[0].lowercase()) {
                "help" -> showHelp(sender)
                
                "create" -> {
                    if (!sender.hasPermission("postwarps.create")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.player-only")
                        ))
                        return true
                    }
                    
                    if (args.size < 2) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("create.usage")
                        ))
                        return true
                    }
                    
                    val name = args[1]
                    val description = if (args.size > 2) args.copyOfRange(2, args.size).joinToString(" ") else ""
                    val isPublic = if (args.size > 3 && args[3].equals("true", ignoreCase = true)) true else false
                    
                    // 验证名称
                    if (name.isEmpty() || name.length > 32) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("create.invalid-name")
                        ))
                        return true
                    }
                    
                    // 检查名称是否存在
                    if (plugin.getDatabaseManager().getWarp(name, sender.uniqueId) != null) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("create.name-exists")
                        ))
                        return true
                    }
                    
                    // 创建地标
                    val warp = Warp.fromLocation(
                        name = name,
                        owner = sender.uniqueId,
                        ownerName = sender.name,
                        location = sender.location,
                        isPublic = isPublic,
                        description = description
                    )
                    
                    val success = plugin.getDatabaseManager().createWarp(warp)
                    if (success) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("create.success")
                                .replace("{name}", name)
                        ))
                    } else {
                        sender.sendMessage("${ChatColor.RED}创建地标失败，请稍后再试。")
                    }
                }
                
                "delete" -> {
                    if (!sender.hasPermission("postwarps.delete")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.player-only")
                        ))
                        return true
                    }
                    
                    if (args.size < 2) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("delete.usage")
                        ))
                        return true
                    }
                    
                    val name = args[1]
                    
                    // 获取地标
                    val warp = plugin.getDatabaseManager().getWarp(name, sender.uniqueId)
                    if (warp == null) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("delete.not-found")
                                .replace("{name}", name)
                        ))
                        return true
                    }
                    
                    // 检查是否是自己的地标
                    if (warp.owner != sender.uniqueId && !sender.hasPermission("postwarps.admin")) {
                        sender.sendMessage("${ChatColor.RED}你不能删除其他玩家的地标。")
                        return true
                    }
                    
                    // 删除地标
                    val success = plugin.getDatabaseManager().deleteWarp(warp.id)
                    if (success) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("delete.success")
                                .replace("{name}", name)
                        ))
                    } else {
                        sender.sendMessage("${ChatColor.RED}删除地标失败，请稍后再试。")
                    }
                }
                
                "list" -> {
                    if (!sender.hasPermission("postwarps.list")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.player-only")
                        ))
                        return true
                    }
                    
                    // 确定是查看公开还是私有地标
                    val listType = if (args.size > 1) args[1].lowercase() else "all"
                    
                    // 获取地标列表
                    val warps = when (listType) {
                        "public" -> plugin.getDatabaseManager().getPlayerPublicWarps(sender.uniqueId)
                        "private" -> plugin.getDatabaseManager().getPlayerPrivateWarps(sender.uniqueId)
                        else -> plugin.getDatabaseManager().getPlayerWarps(sender.uniqueId)
                    }
                    
                    // 显示列表
                    sender.sendMessage("${ChatColor.GREEN}===== ${if (listType == "public") "公开" else if (listType == "private") "私有" else "所有"}地标列表 =====")
                    
                    if (warps.isEmpty()) {
                        sender.sendMessage("${ChatColor.YELLOW}没有找到地标。")
                    } else {
                        warps.forEach { warp ->
                            val status = if (warp.isPublic) "${ChatColor.GREEN}[公开]" else "${ChatColor.RED}[私有]"
                            sender.sendMessage("$status ${ChatColor.WHITE}${warp.name} ${ChatColor.GRAY}- ${warp.description}")
                        }
                    }
                }
                
                "tp", "teleport" -> {
                    if (!sender.hasPermission("postwarps.teleport")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.player-only")
                        ))
                        return true
                    }
                    
                    if (args.size < 2) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("teleport.usage")
                        ))
                        return true
                    }
                    
                    val name = args[1]
                    
                    // 获取地标
                    val warp = plugin.getDatabaseManager().getWarp(name, sender.uniqueId) 
                        ?: plugin.getDatabaseManager().getPublicWarp(name)
                    
                    if (warp == null) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("teleport.not-found")
                                .replace("{name}", name)
                        ))
                        return true
                    }
                    
                    // 检查权限
                    if (warp.owner != sender.uniqueId && !warp.isPublic && !sender.hasPermission("postwarps.admin")) {
                        sender.sendMessage("${ChatColor.RED}你没有权限传送到此地标。")
                        return true
                    }
                    
                    // 获取位置
                    val location = warp.getLocation()
                    if (location == null) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("teleport.failed")
                                .replace("{name}", name)
                        ))
                        return true
                    }
                    
                    // 传送
                    sender.teleport(location)
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("teleported")
                            .replace("{name}", name)
                    ))
                }
                
                "info" -> {
                    if (!sender.hasPermission("postwarps.info")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    if (args.size < 2) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("info.usage")
                        ))
                        return true
                    }
                    
                    val name = args[1]
                    
                    // 获取地标
                    val warp = if (sender is Player) {
                        plugin.getDatabaseManager().getWarp(name, sender.uniqueId) 
                            ?: plugin.getDatabaseManager().getPublicWarp(name)
                    } else {
                        plugin.getDatabaseManager().getPublicWarp(name)
                    }
                    
                    if (warp == null) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("info.not-found")
                                .replace("{name}", name)
                        ))
                        return true
                    }
                    
                    // 显示信息
                    sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("info.header")))
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("info.name").replace("{name}", warp.name)
                    ))
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("info.owner").replace("{owner}", warp.ownerName)
                    ))
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("info.world").replace("{world}", warp.worldName)
                    ))
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("info.coordinates").replace("{coords}", warp.getFormattedCoordinates())
                    ))
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("info.created").replace("{time}", formatTimestamp(warp.createTime))
                    ))
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("info.public").replace("{public}", if (warp.isPublic) "公开" else "私有")
                    ))
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("info.description").replace("{desc}", warp.description.ifEmpty { "无" })
                    ))
                }
                
                "public" -> {
                    if (!sender.hasPermission("postwarps.public")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.player-only")
                        ))
                        return true
                    }
                    
                    if (args.size < 2) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("public.usage")
                        ))
                        return true
                    }
                    
                    val name = args[1]
                    
                    // 获取地标
                    val warp = plugin.getDatabaseManager().getWarp(name, sender.uniqueId)
                    if (warp == null) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("public.not-found")
                                .replace("{name}", name)
                        ))
                        return true
                    }
                    
                    // 检查是否是自己的地标
                    if (warp.owner != sender.uniqueId && !sender.hasPermission("postwarps.admin")) {
                        sender.sendMessage("${ChatColor.RED}你不能修改其他玩家的地标。")
                        return true
                    }
                    
                    // 检查当前状态
                    if (warp.isPublic) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("public.already-public")
                                .replace("{name}", name)
                        ))
                        return true
                    }
                    
                    // 更新状态
                    val success = plugin.getDatabaseManager().setWarpPublic(warp.id, true)
                    if (success) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("public.success")
                                .replace("{name}", name)
                        ))
                    } else {
                        sender.sendMessage("${ChatColor.RED}更新地标状态失败，请稍后再试。")
                    }
                }
                
                "private" -> {
                    if (!sender.hasPermission("postwarps.private")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.player-only")
                        ))
                        return true
                    }
                    
                    if (args.size < 2) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("private.usage")
                        ))
                        return true
                    }
                    
                    val name = args[1]
                    
                    // 获取地标
                    val warp = plugin.getDatabaseManager().getWarp(name, sender.uniqueId)
                    if (warp == null) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("private.not-found")
                                .replace("{name}", name)
                        ))
                        return true
                    }
                    
                    // 检查是否是自己的地标
                    if (warp.owner != sender.uniqueId && !sender.hasPermission("postwarps.admin")) {
                        sender.sendMessage("${ChatColor.RED}你不能修改其他玩家的地标。")
                        return true
                    }
                    
                    // 检查当前状态
                    if (!warp.isPublic) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("private.already-private")
                                .replace("{name}", name)
                        ))
                        return true
                    }
                    
                    // 更新状态
                    val success = plugin.getDatabaseManager().setWarpPublic(warp.id, false)
                    if (success) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("private.success")
                                .replace("{name}", name)
                        ))
                    } else {
                        sender.sendMessage("${ChatColor.RED}更新地标状态失败，请稍后再试。")
                    }
                }
                
                "menu" -> {
                    if (!sender.hasPermission("postwarps.menu")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.player-only")
                        ))
                        return true
                    }
                    
                    val menuName = if (args.size > 1) args[1] else "main"
                    plugin.getMenuManager().openMenu(sender, menuName)
                }
                
                "reload" -> {
                    if (!sender.hasPermission("postwarps.admin")) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.no-permission")
                        ))
                        return true
                    }
                    
                    plugin.reload()
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("messages.reload")
                    ))
                }
                
                "version" -> {
                    sender.sendMessage("${ChatColor.GREEN}PostWarps 版本: ${plugin.description.version}")
                    sender.sendMessage("${ChatColor.GREEN}作者: postyizhan")
                    
                    if (sender.hasPermission("postwarps.admin")) {
                        plugin.getUpdateChecker().checkForUpdates { isUpdateAvailable, newVersion ->
                            if (isUpdateAvailable) {
                                sender.sendMessage(MessageUtil.color(
                                    MessageUtil.getMessage("updater.update_available")
                                        .replace("{current_version}", plugin.description.version)
                                        .replace("{latest_version}", newVersion)
                                ))
                                sender.sendMessage(MessageUtil.color(
                                    MessageUtil.getMessage("updater.update_url")
                                        .replace("{current_version}", plugin.description.version)
                                        .replace("{latest_version}", newVersion)
                                ))
                            } else {
                                sender.sendMessage(MessageUtil.color(
                                    MessageUtil.getMessage("updater.up_to_date")
                                ))
                            }
                        }
                    }
                }
                
                else -> {
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("messages.invalid-command")
                    ))
                }
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * 提供命令补全
     */
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (command.name.equals("postwarps", ignoreCase = true)) {
            // 子命令补全
            if (args.size == 1) {
                val subCommands = mutableListOf<String>()
                
                // 添加有权限的命令
                if (sender.hasPermission("postwarps.help")) subCommands.add("help")
                if (sender.hasPermission("postwarps.create")) subCommands.add("create")
                if (sender.hasPermission("postwarps.delete")) subCommands.add("delete")
                if (sender.hasPermission("postwarps.list")) subCommands.add("list")
                if (sender.hasPermission("postwarps.teleport")) {
                    subCommands.add("tp")
                    subCommands.add("teleport")
                }
                if (sender.hasPermission("postwarps.info")) subCommands.add("info")
                if (sender.hasPermission("postwarps.public")) subCommands.add("public")
                if (sender.hasPermission("postwarps.private")) subCommands.add("private")
                if (sender.hasPermission("postwarps.menu")) subCommands.add("menu")
                if (sender.hasPermission("postwarps.admin")) {
                    subCommands.add("reload")
                    subCommands.add("version")
                }
                
                return filterTabCompletions(subCommands, args[0])
            }
            
            // 特定子命令的参数补全
            if (args.size == 2) {
                when (args[0].lowercase()) {
                    "delete", "tp", "teleport", "info", "public", "private" -> {
                        if (sender is Player) {
                            // 获取玩家的地标名称
                            val warps = plugin.getDatabaseManager().getPlayerWarps(sender.uniqueId)
                            val publicWarps = plugin.getDatabaseManager().getAllPublicWarps()
                                .filter { it.owner != sender.uniqueId }
                            
                            val warpNames = mutableListOf<String>()
                            warpNames.addAll(warps.map { it.name })
                            
                            // 只有传送和查看信息命令可以使用公开地标
                            if (args[0].lowercase() == "tp" || args[0].lowercase() == "teleport" || args[0].lowercase() == "info") {
                                warpNames.addAll(publicWarps.map { it.name })
                            }
                            
                            return filterTabCompletions(warpNames, args[1])
                        }
                    }
                    
                    "list" -> {
                        return filterTabCompletions(listOf("all", "public", "private"), args[1])
                    }
                    
                    "menu" -> {
                        return filterTabCompletions(listOf("main", "create", "public_warps", "private_warps", "settings"), args[1])
                    }
                }
            }
            
            // 创建命令的布尔参数补全
            if (args.size == 3 && args[0].lowercase() == "create") {
                return filterTabCompletions(listOf("true", "false"), args[2])
            }
        }
        
        return null
    }
    
    /**
     * 过滤命令补全
     */
    private fun filterTabCompletions(completions: List<String>, arg: String): List<String> {
        return completions.filter { it.lowercase().startsWith(arg.lowercase()) }
    }
    
    /**
     * 显示帮助信息
     */
    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.header")))
        
        // 根据权限显示命令
        if (sender.hasPermission("postwarps.create"))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.create")))
        
        if (sender.hasPermission("postwarps.delete"))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.delete")))
        
        if (sender.hasPermission("postwarps.list"))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.list")))
        
        if (sender.hasPermission("postwarps.teleport"))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.tp")))
        
        if (sender.hasPermission("postwarps.info"))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.info")))
        
        if (sender.hasPermission("postwarps.public"))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.public")))
        
        if (sender.hasPermission("postwarps.private"))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.private")))
        
        if (sender.hasPermission("postwarps.menu"))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.menu")))
        
        if (sender.hasPermission("postwarps.admin")) {
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.reload")))
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.version")))
        }
    }
    
    /**
     * 格式化时间戳
     */
    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(date)
    }
}
 