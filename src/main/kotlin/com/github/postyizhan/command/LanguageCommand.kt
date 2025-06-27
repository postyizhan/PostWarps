package com.github.postyizhan.command

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * 语言切换命令处理器
 * 允许玩家切换界面语言
 */
class LanguageCommand(private val plugin: PostWarps) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("general.player-only")))
            return true
        }
        
        when (args.size) {
            0 -> {
                // 显示当前语言和可用语言
                showLanguageInfo(sender)
            }
            1 -> {
                when (args[0].lowercase()) {
                    "list", "列表" -> {
                        // 显示支持的语言列表
                        showSupportedLanguages(sender)
                    }
                    "auto", "自动" -> {
                        // 设置为自动检测（清除偏好设置）
                        setAutoLanguage(sender)
                    }
                    "reload", "重载" -> {
                        // 重新加载语言文件（管理员权限）
                        reloadLanguages(sender)
                    }
                    else -> {
                        // 设置指定语言
                        setLanguage(sender, args[0])
                    }
                }
            }
            else -> {
                // 参数过多
                sender.sendMessage(MessageUtil.color(MessageUtil.getMessage("general.invalid-args", sender)))
                showUsage(sender)
            }
        }
        
        return true
    }
    
    /**
     * 显示当前语言信息
     */
    private fun showLanguageInfo(player: Player) {
        val currentLang = MessageUtil.getPlayerLanguage(player)
        val clientLang = try {
            player.locale
        } catch (e: Exception) {
            "unknown"
        }
        
        player.sendMessage(MessageUtil.color("&8========== &3语言信息 &8=========="))
        player.sendMessage(MessageUtil.color("&7当前语言: &f$currentLang"))
        player.sendMessage(MessageUtil.color("&7客户端语言: &f$clientLang"))
        player.sendMessage(MessageUtil.color("&7支持的语言: &f${MessageUtil.getSupportedLanguages().joinToString(", ")}"))
        player.sendMessage(MessageUtil.color(""))
        player.sendMessage(MessageUtil.color("&e使用 &f/lang <语言> &e切换语言"))
        player.sendMessage(MessageUtil.color("&e使用 &f/lang auto &e启用自动检测"))
        player.sendMessage(MessageUtil.color("&e使用 &f/lang list &e查看支持的语言"))
    }
    
    /**
     * 显示支持的语言列表
     */
    private fun showSupportedLanguages(player: Player) {
        player.sendMessage(MessageUtil.color("&8========== &3支持的语言 &8=========="))
        
        val supportedLanguages = mapOf(
            "zh_CN" to "中文 (简体)",
            "en_US" to "English (US)"
        )
        
        supportedLanguages.forEach { (code, name) ->
            val current = if (MessageUtil.getPlayerLanguage(player) == code) " &a(当前)" else ""
            player.sendMessage(MessageUtil.color("&e$code &7- &f$name$current"))
        }
        
        player.sendMessage(MessageUtil.color(""))
        player.sendMessage(MessageUtil.color("&e使用 &f/lang <语言代码> &e切换语言"))
        player.sendMessage(MessageUtil.color("&7例如: &f/lang en_US"))
    }
    
    /**
     * 设置指定语言
     */
    private fun setLanguage(player: Player, language: String) {
        val normalizedLang = language
        
        if (MessageUtil.isLanguageSupported(normalizedLang)) {
            MessageUtil.setPlayerLanguage(player, normalizedLang)
            
            // 使用新语言发送确认消息
            val confirmMessage = when (normalizedLang) {
                "zh_CN" -> "&a语言已切换为中文！"
                "en_US" -> "&aLanguage switched to English!"
                else -> "&aLanguage switched to $normalizedLang!"
            }
            
            player.sendMessage(MessageUtil.color(confirmMessage))

            // 自动刷新当前打开的菜单
            refreshCurrentMenu(player, normalizedLang)
            
        } else {
            player.sendMessage(MessageUtil.color(MessageUtil.getMessage("language.unsupported", player)
                .replace("{language}", language)
                .replace("{supported}", MessageUtil.getSupportedLanguages().joinToString(", "))
            ))
        }
    }

    /**
     * 刷新当前打开的菜单
     */
    private fun refreshCurrentMenu(player: Player, language: String) {
        try {
            // 检查玩家是否有打开的菜单
            val currentMenu = plugin.getMenuManager().getOpenMenu(player)
            if (currentMenu != null) {
                // 延迟刷新菜单，确保语言设置已生效
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    // 使用强制刷新方法，清除缓存并重新打开
                    plugin.getMenuManager().forceRefreshPlayerMenu(player)

                    // 发送刷新提示
                    val refreshMessage = when (language) {
                        "zh_CN" -> "&7菜单已自动刷新为中文"
                        "en_US" -> "&7Menu automatically refreshed to English"
                        else -> "&7Menu automatically refreshed to $language"
                    }
                    player.sendMessage(MessageUtil.color(refreshMessage))
                }, 2L)
            } else {
                // 没有打开的菜单，发送提示
                val tipMessage = when (language) {
                    "zh_CN" -> "&7提示: 打开菜单以查看语言变化"
                    "en_US" -> "&7Tip: Open menus to see language changes"
                    else -> "&7Tip: Open menus to see language changes"
                }
                player.sendMessage(MessageUtil.color(tipMessage))
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to refresh menu for player ${player.name}: ${e.message}")

            // 发送手动刷新提示
            val tipMessage = when (language) {
                "zh_CN" -> "&7提示: 重新打开菜单以查看语言变化"
                "en_US" -> "&7Tip: Reopen menus to see language changes"
                else -> "&7Tip: Reopen menus to see language changes"
            }
            player.sendMessage(MessageUtil.color(tipMessage))
        }
    }

    /**
     * 设置为自动检测语言
     */
    private fun setAutoLanguage(player: Player) {
        MessageUtil.clearPlayerLanguage(player)
        
        val newLang = MessageUtil.getPlayerLanguage(player)
        val confirmMessage = when (newLang) {
            "zh_CN" -> "&a已启用自动语言检测！当前语言: 中文"
            "en_US" -> "&aAuto language detection enabled! Current language: English"
            else -> "&aAuto language detection enabled! Current language: $newLang"
        }
        
        player.sendMessage(MessageUtil.color(confirmMessage))

        // 自动刷新当前打开的菜单
        refreshCurrentMenu(player, newLang)
    }
    
    /**
     * 重新加载语言文件
     */
    private fun reloadLanguages(player: Player) {
        if (!player.hasPermission("postwarps.admin")) {
            player.sendMessage(MessageUtil.color(MessageUtil.getMessage("general.no-permission", player)))
            return
        }
        
        MessageUtil.reloadLanguages()
        
        val confirmMessage = when (MessageUtil.getPlayerLanguage(player)) {
            "zh_CN" -> "&a语言文件已重新加载！"
            "en_US" -> "&aLanguage files reloaded!"
            else -> "&aLanguage files reloaded!"
        }
        
        player.sendMessage(MessageUtil.color(confirmMessage))
    }
    
    /**
     * 显示命令用法
     */
    private fun showUsage(player: Player) {
        val currentLang = MessageUtil.getPlayerLanguage(player)
        
        when (currentLang) {
            "zh_CN" -> {
                player.sendMessage(MessageUtil.color("&e语言命令用法:"))
                player.sendMessage(MessageUtil.color("&f/lang &7- 显示当前语言信息"))
                player.sendMessage(MessageUtil.color("&f/lang <语言> &7- 切换到指定语言"))
                player.sendMessage(MessageUtil.color("&f/lang auto &7- 启用自动语言检测"))
                player.sendMessage(MessageUtil.color("&f/lang list &7- 显示支持的语言"))
                player.sendMessage(MessageUtil.color("&f/lang reload &7- 重新加载语言文件 (管理员)"))
            }
            "en_US" -> {
                player.sendMessage(MessageUtil.color("&eLanguage command usage:"))
                player.sendMessage(MessageUtil.color("&f/lang &7- Show current language info"))
                player.sendMessage(MessageUtil.color("&f/lang <language> &7- Switch to specified language"))
                player.sendMessage(MessageUtil.color("&f/lang auto &7- Enable auto language detection"))
                player.sendMessage(MessageUtil.color("&f/lang list &7- Show supported languages"))
                player.sendMessage(MessageUtil.color("&f/lang reload &7- Reload language files (admin)"))
            }
        }
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()
        
        return when (args.size) {
            1 -> {
                val input = args[0].lowercase()
                val suggestions = mutableListOf<String>()
                
                // 添加语言代码
                suggestions.addAll(MessageUtil.getSupportedLanguages())

                // 添加特殊命令
                suggestions.addAll(listOf("auto", "list", "reload"))
                
                // 过滤匹配的建议
                suggestions.filter { it.lowercase().startsWith(input) }
            }
            else -> emptyList()
        }
    }
}
