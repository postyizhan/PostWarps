package com.github.postyizhan.commands

import com.github.postyizhan.PostWarps
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.*

class WarpCommand(private val plugin: PostWarps) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // 只有玩家可以使用此命令
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players")
            return true
        }
        
        // 如果没有参数，显示帮助
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        // 根据子命令执行相应操作
        when (args[0].lowercase()) {
            "create", "add", "set" -> {
                // 检查权限
                if (!sender.hasPermission("postwarps.create")) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("no.permission"))
                    return true
                }
                
                // 检查参数
                if (args.size < 2) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.create.usage"))
                    return true
                }
                
                val name = args[1]
                
                // 解析描述
                var description = ""
                
                if (args.size > 2) {
                    description = args.slice(2 until args.size).joinToString(" ")
                }
                
                // 执行创建
                if (plugin.warpManager.warpExists(name)) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.create.already_exists", name))
                } else {
                    val maxWarps = plugin.config.getInt("settings.maxWarpsPerPlayer", 5)
                    val currentWarps = plugin.databaseManager.getWarpCount(sender.uniqueId)
                    
                    if (maxWarps > 0 && currentWarps >= maxWarps) {
                        sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.create.limit_reached", maxWarps))
                    } else {
                        val success = plugin.warpManager.createWarp(sender, name, description)
                        
                        if (success) {
                            sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.create.success", name))
                        } else {
                            sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("error.occurred"))
                        }
                    }
                }
            }
            
            "delete", "remove", "del" -> {
                // 检查权限
                if (!sender.hasPermission("postwarps.delete")) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("no.permission"))
                    return true
                }
                
                // 检查参数
                if (args.size < 2) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.delete.usage"))
                    return true
                }
                
                val name = args[1]
                
                // 执行删除
                val warp = plugin.warpManager.getWarp(name)
                
                if (warp == null) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.delete.not_found", name))
                } else if (warp.ownerUUID != sender.uniqueId && !sender.hasPermission("postwarps.admin")) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.delete.no_permission"))
                } else {
                    val success = plugin.warpManager.deleteWarp(sender, name)
                    
                    if (success) {
                        sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.delete.success", name))
                    } else {
                        sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("error.occurred"))
                    }
                }
            }
            
            "list", "ls" -> {
                // 检查权限
                if (!sender.hasPermission("postwarps.list")) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("no.permission"))
                    return true
                }
                
                // 解析参数
                var page = 1
                
                if (args.size > 1) {
                    // 尝试将参数解析为页码
                    try {
                        page = args[1].toInt()
                    } catch (e: NumberFormatException) {
                        page = 1
                    }
                }
                
                // 显示地标列表
                val warps = plugin.warpManager.getWarpsByOwner(sender.uniqueId)
                
                if (warps.isEmpty()) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.list.empty"))
                } else {
                    val warpsPerPage = plugin.config.getInt("settings.warpsPerPage", 5)
                    val totalPages = (warps.size + warpsPerPage - 1) / warpsPerPage
                    val currentPage = page.coerceIn(1, totalPages)
                    
                    val start = (currentPage - 1) * warpsPerPage
                    val end = (start + warpsPerPage).coerceAtMost(warps.size)
                    
                    val title = plugin.i18n.getMessage("warp.list.title", currentPage, totalPages)
                    
                    sender.sendMessage(title)
                    
                    for (i in start until end) {
                        val warp = warps[i]
                        sender.sendMessage(plugin.i18n.getMessage("warp.list.entry", i + 1, warp.name, warp.description))
                    }
                    
                    if (currentPage < totalPages) {
                        val nextPageCommand = "/warp list ${currentPage + 1}"
                        sender.sendMessage(plugin.i18n.getMessage("warp.list.footer", nextPageCommand))
                    }
                }
            }
            
            "teleport", "tp", "warp" -> {
                // 检查权限
                if (!sender.hasPermission("postwarps.teleport")) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("no.permission"))
                    return true
                }
                
                // 检查参数
                if (args.size < 2) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.teleport.usage"))
                    return true
                }
                
                val name = args[1]
                
                // 执行传送
                val warp = plugin.warpManager.getWarp(name)
                
                if (warp == null) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.teleport.not_found", name))
                } else {
                    val success = plugin.warpManager.teleportToWarp(sender, name)
                    
                    if (!success) {
                        sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("error.occurred"))
                    }
                }
            }
            
            "info", "show" -> {
                // 检查参数
                if (args.size < 2) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.teleport.usage"))
                    return true
                }
                
                val name = args[1]
                
                // 获取地标信息
                val warp = plugin.warpManager.getWarp(name)
                
                if (warp == null) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.teleport.not_found", name))
                } else {
                    // 显示地标信息
                    sender.sendMessage(plugin.i18n.getMessage("warp.info.title", warp.name))
                    
                    val ownerName = Bukkit.getOfflinePlayer(warp.ownerUUID).name ?: warp.ownerUUID.toString()
                    sender.sendMessage(plugin.i18n.getMessage("warp.info.owner", ownerName))
                    
                    val location = warp.toLocation()
                    if (location != null) {
                        sender.sendMessage(plugin.i18n.getMessage("warp.info.location", 
                            location.blockX, location.blockY, location.blockZ, location.world?.name ?: "unknown"))
                    }
                    
                    if (warp.server.isNotEmpty()) {
                        sender.sendMessage(plugin.i18n.getMessage("warp.info.server", warp.server))
                    }
                    
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    sender.sendMessage(plugin.i18n.getMessage("warp.info.created", dateFormat.format(Date(warp.createdAt))))
                    
                    sender.sendMessage(plugin.i18n.getMessage("warp.info.visits", warp.visits))
                    
                    if (warp.description.isNotEmpty()) {
                        sender.sendMessage(plugin.i18n.getMessage("warp.info.description", warp.description))
                    }
                }
            }
            
            "reload" -> {
                // 检查权限
                if (!sender.hasPermission("postwarps.admin")) {
                    sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("no.permission"))
                    return true
                }
                
                // 重载插件
                plugin.reloadConfig()
                
                sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("reload.success"))
            }
            
            "help" -> {
                showHelp(sender)
            }
            
            else -> {
                sender.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("command.unknown"))
            }
        }
        
        return true
    }
    
    private fun showHelp(player: Player) {
        player.sendMessage(plugin.i18n.getMessage("warp.help.title"))
        player.sendMessage(plugin.i18n.getMessage("warp.help.create"))
        player.sendMessage(plugin.i18n.getMessage("warp.help.delete"))
        player.sendMessage(plugin.i18n.getMessage("warp.help.list"))
        player.sendMessage(plugin.i18n.getMessage("warp.help.teleport"))
        player.sendMessage(plugin.i18n.getMessage("warp.help.info"))
        
        if (player.hasPermission("postwarps.admin")) {
            player.sendMessage(plugin.i18n.getMessage("warp.help.reload"))
        }
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (sender !is Player) return null
        
        val completions = mutableListOf<String>()
        
        // 第一个参数的补全
        if (args.size == 1) {
            val subCommands = mutableListOf<String>()
            
            if (sender.hasPermission("postwarps.create")) {
                subCommands.add("create")
            }
            
            if (sender.hasPermission("postwarps.delete")) {
                subCommands.add("delete")
            }
            
            if (sender.hasPermission("postwarps.list")) {
                subCommands.add("list")
            }
            
            if (sender.hasPermission("postwarps.teleport")) {
                subCommands.add("teleport")
                subCommands.add("tp")
            }
            
            subCommands.add("info")
            subCommands.add("help")
            
            if (sender.hasPermission("postwarps.admin")) {
                subCommands.add("reload")
            }
            
            return subCommands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        // 第二个参数的补全
        if (args.size == 2) {
            when (args[0].lowercase()) {
                "delete", "teleport", "tp", "info" -> {
                    // 获取玩家可以使用的地标列表
                    val warps = if (sender.hasPermission("postwarps.admin")) {
                        plugin.warpManager.getWarpsByOwner(sender.uniqueId).map { it.name }
                    } else {
                        plugin.warpManager.getWarpsByOwner(sender.uniqueId).map { it.name }
                    }
                    
                    return warps.filter { it.lowercase().startsWith(args[1].lowercase()) }
                }
            }
        }
        
        return completions
    }
}
