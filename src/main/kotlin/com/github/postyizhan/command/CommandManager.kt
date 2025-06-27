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

    // 语言命令处理器
    private val languageCommand = LanguageCommand(plugin)
    
    /**
     * 注册命令
     */
    fun registerCommands() {
        val pluginCommand = plugin.getCommand("postwarps")
        pluginCommand?.setExecutor(this)
        pluginCommand?.tabCompleter = this

        // 注册语言命令
        val langCommand = plugin.getCommand("lang")
        langCommand?.setExecutor(languageCommand)
        langCommand?.tabCompleter = languageCommand
    }
    
    /**
     * 处理命令执行
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // 检查命令是否是插件命令
        if (command.name.equals("postwarps", ignoreCase = true)) {
            // 调试信息
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Command executed: ${command.name}, args: ${args.joinToString(" ")}, sender: ${sender.name}")
            }

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

                    // 检查经济费用
                    if (!plugin.getEconomyService().chargeCreateCost(sender)) {
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

                        // 退还费用
                        plugin.getEconomyService().refundDeleteCost(sender)
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

                    // 检查传送费用
                    if (!plugin.getEconomyService().chargeTeleportCost(sender, warp.isPublic)) {
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

                    // 检查设置公开的费用
                    if (!plugin.getEconomyService().chargeSetPublicCost(sender)) {
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
                    if (sender is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.reload", sender)
                        ))
                    } else {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.reload")
                        ))
                    }
                }
                
                "economy", "eco" -> {
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.player-only")
                        ))
                        return true
                    }

                    // 显示经济信息
                    sender.sendMessage("${ChatColor.GREEN}===== 经济信息 =====")
                    sender.sendMessage(MessageUtil.color(plugin.getEconomyService().getBalanceInfo(sender)))

                    val costInfo = plugin.getEconomyService().getCostInfo(sender)
                    costInfo.forEach { info ->
                        sender.sendMessage(MessageUtil.color(info))
                    }
                }

                "version" -> {
                    sender.sendMessage("${ChatColor.GREEN}PostWarps 版本: ${plugin.description.version}")
                    sender.sendMessage("${ChatColor.GREEN}作者: postyizhan")

                    // 只有管理员可以检查更新
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

                "placeholders", "papi" -> {
                    if (!sender.hasPermission("postwarps.admin")) {
                        if (sender is Player) {
                            sender.sendMessage(MessageUtil.color(
                                MessageUtil.getMessage("messages.no-permission", sender)
                            ))
                        } else {
                            sender.sendMessage(MessageUtil.color(
                                MessageUtil.getMessage("messages.no-permission")
                            ))
                        }
                        return true
                    }
                    showPlaceholders(sender)
                }

                "lang", "language" -> {
                    // 语言命令处理
                    if (sender !is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("general.player-only")
                        ))
                        return true
                    }

                    // 将参数传递给语言命令处理器
                    val langArgs = if (args.size > 1) args.sliceArray(1 until args.size) else emptyArray()
                    languageCommand.onCommand(sender, command, label, langArgs)
                }

                else -> {
                    if (sender is Player) {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.invalid-command", sender)
                        ))
                    } else {
                        sender.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("messages.invalid-command")
                        ))
                    }
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
                subCommands.add("help") // help命令对所有人开放
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
                if (sender is Player) {
                    subCommands.add("economy")
                    subCommands.add("eco")
                    subCommands.add("lang")
                    subCommands.add("language")
                }
                if (sender.hasPermission("postwarps.admin")) {
                    subCommands.add("reload")
                    subCommands.add("placeholders")
                    subCommands.add("papi")
                }
                // version命令不需要特殊权限
                subCommands.add("version")
                
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
                    "lang", "language" -> {
                        // 语言命令的tab补全
                        if (sender is Player) {
                            return languageCommand.onTabComplete(sender, command, alias, args.sliceArray(1 until args.size))
                        }
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
        if (sender is Player) {
            // 玩家发送者，使用国际化消息
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.header", sender)))

            // 根据权限显示命令
            if (sender.hasPermission("postwarps.create"))
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.create", sender)))

            if (sender.hasPermission("postwarps.delete"))
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.delete", sender)))

            if (sender.hasPermission("postwarps.list"))
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.list", sender)))

            if (sender.hasPermission("postwarps.teleport"))
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.tp", sender)))

            if (sender.hasPermission("postwarps.info"))
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.info", sender)))

            if (sender.hasPermission("postwarps.public"))
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.public", sender)))

            if (sender.hasPermission("postwarps.private"))
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.private", sender)))

            if (sender.hasPermission("postwarps.menu"))
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.menu", sender)))

            // 经济命令对所有玩家开放
            val economyHelp = when (MessageUtil.getPlayerLanguage(sender)) {
                "en_US" -> "&7/pw economy &f- View economy information and costs"
                else -> "&7/pw economy &f- 查看经济信息和费用"
            }
            sender.sendMessage(MessageUtil.color(economyHelp))

            if (sender.hasPermission("postwarps.admin")) {
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.reload", sender)))
                val placeholderHelp = when (MessageUtil.getPlayerLanguage(sender)) {
                    "en_US" -> "&7/pw placeholders &f- View PlaceholderAPI placeholders"
                    else -> "&7/pw placeholders &f- 查看PlaceholderAPI占位符"
                }
                sender.sendMessage(MessageUtil.color(placeholderHelp))
            }

            // version命令对所有人开放
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.version", sender)))
        } else {
            // 控制台发送者，使用默认语言
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

            // 经济命令
            sender.sendMessage("&7/pw economy &f- 查看经济信息和费用")

            if (sender.hasPermission("postwarps.admin")) {
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.reload")))
                sender.sendMessage("&7/pw placeholders &f- 查看PlaceholderAPI占位符")
            }

            // version命令对所有人开放
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("help.version")))
        }
    }
    
    /**
     * 显示PlaceholderAPI占位符信息
     */
    private fun showPlaceholders(sender: CommandSender) {
        if (!plugin.getPlaceholderAPIManager().isAvailable()) {
            sender.sendMessage("${ChatColor.RED}PlaceholderAPI未安装或未启用")
            return
        }

        sender.sendMessage("${ChatColor.GREEN}=== PostWarps PlaceholderAPI 占位符 ===")

        val helpLines = plugin.getPlaceholderAPIManager().getPlaceholderHelp()
        helpLines.forEach { line ->
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line))
        }

        sender.sendMessage("")
        sender.sendMessage("${ChatColor.YELLOW}注意: 将 <name> 替换为实际的地标名称")
        sender.sendMessage("${ChatColor.YELLOW}例如: %postwarps_has_warp_home% 检查是否有名为 'home' 的地标")
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
 