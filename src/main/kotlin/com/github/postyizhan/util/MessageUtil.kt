package com.github.postyizhan.util

import com.github.postyizhan.PostWarps
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * 消息工具类，负责处理消息发送和颜色处理
 */
object MessageUtil {

    private lateinit var plugin: PostWarps
    private lateinit var messages: YamlConfiguration
    private lateinit var prefix: String

    // 缓存不同语言的消息配置
    private val languageMessages = ConcurrentHashMap<String, YamlConfiguration>()

    // 玩家语言偏好设置（覆盖客户端检测）
    private val playerLanguagePreferences = ConcurrentHashMap<String, String>()
    
    /**
     * 初始化消息工具
     */
    fun init(plugin: PostWarps) {
        this.plugin = plugin
        loadMessages()
    }
    
    /**
     * 加载语言文件
     */
    private fun loadMessages() {
        val language = plugin.getConfigManager().getConfig().getString("language", "zh_CN")
        val langFile = File(plugin.dataFolder, "lang/$language.yml")
        
        // 如果文件不存在，则创建
        if (!langFile.exists()) {
            plugin.saveResource("lang/$language.yml", false)
        }
        
        // 尝试从文件加载，如果失败则从内置资源加载
        messages = try {
            YamlConfiguration.loadConfiguration(langFile)
        } catch (e: Exception) {
            plugin.logger.warning("无法从文件加载语言文件，使用内置资源: ${e.message}")
            val resource = plugin.getResource("lang/$language.yml") ?: plugin.getResource("lang/zh_CN.yml")
            if (resource != null) {
                YamlConfiguration.loadConfiguration(InputStreamReader(resource))
            } else {
                YamlConfiguration()
            }
        }
        
        // 获取前缀
        prefix = messages.getString("prefix", "&8[&3Post&bWarps&8] ")
    }
    
    /**
     * 获取消息（使用默认语言）
     */
    fun getMessage(path: String): String {
        var message = messages.getString(path)
        if (message == null) {
            plugin.logger.warning("找不到消息路径: $path")
            message = "&c找不到消息: $path"
        }
        return message.replace("{prefix}", prefix)
    }

    /**
     * 获取玩家语言的消息
     */
    fun getMessage(path: String, player: Player): String {
        val language = getPlayerLanguage(player)
        val langMessages = getLanguageMessages(language)

        var message = langMessages.getString(path)
        if (message == null) {
            // 回退到默认语言
            message = messages.getString(path)
            if (message == null) {
                plugin.logger.warning("找不到消息路径: $path")
                message = "&c找不到消息: $path"
            }
        }

        val langPrefix = langMessages.getString("prefix") ?: prefix
        return message.replace("{prefix}", langPrefix)
    }
    
    /**
     * 处理颜色代码
     */
    fun color(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', message)
    }

    /**
     * 获取玩家的语言设置
     */
    fun getPlayerLanguage(player: Player): String {
        // 第一优先级：玩家手动设置的语言偏好
        val preference = playerLanguagePreferences[player.uniqueId.toString()]
        if (preference != null && preference != "auto") {
            return preference
        }

        // 第二优先级：客户端语言检测
        val clientLanguage = try {
            player.locale
        } catch (e: Exception) {
            null
        }

        // 转换客户端语言到支持的格式
        val supportedLanguage = when (clientLanguage) {
            "zh_cn", "zh_CN" -> "zh_CN"
            "en_us", "en_US" -> "en_US"
            else -> null
        }

        if (supportedLanguage != null) {
            return supportedLanguage
        }

        // 第三优先级：服务器默认语言
        return plugin.getConfigManager().getConfig().getString("language", "zh_CN") ?: "zh_CN"
    }

    /**
     * 设置玩家的语言偏好
     */
    fun setPlayerLanguage(player: Player, language: String) {
        if (isLanguageSupported(language)) {
            playerLanguagePreferences[player.uniqueId.toString()] = language
            plugin.logger.info("Player ${player.name} language preference set to: $language")
        } else {
            plugin.logger.warning("Unsupported language: $language")
        }
    }

    /**
     * 清除玩家的语言偏好（使用客户端检测）
     */
    fun clearPlayerLanguage(player: Player) {
        playerLanguagePreferences.remove(player.uniqueId.toString())
        plugin.logger.info("Player ${player.name} language preference cleared")
    }

    /**
     * 检查语言是否支持
     */
    fun isLanguageSupported(language: String): Boolean {
        return language in listOf("zh_CN", "en_US")
    }

    /**
     * 获取支持的语言列表
     */
    fun getSupportedLanguages(): List<String> {
        return listOf("zh_CN", "en_US")
    }

    /**
     * 获取指定语言的消息配置
     */
    private fun getLanguageMessages(language: String): YamlConfiguration {
        return languageMessages.getOrPut(language) {
            loadLanguageMessages(language)
        }
    }

    /**
     * 加载指定语言的消息文件
     */
    private fun loadLanguageMessages(language: String): YamlConfiguration {
        val langFile = File(plugin.dataFolder, "lang/$language.yml")

        if (langFile.exists()) {
            try {
                val config = YamlConfiguration.loadConfiguration(langFile)
                plugin.logger.info("Successfully loaded language file: $language.yml from data folder")
                return config
            } catch (e: Exception) {
                plugin.logger.warning("无法加载语言文件 $language.yml: ${e.message}")
            }
        } else {
            // 尝试从资源文件加载
            try {
                plugin.getResource("lang/$language.yml")?.use { inputStream ->
                    InputStreamReader(inputStream, "UTF-8").use { reader ->
                        val config = YamlConfiguration.loadConfiguration(reader)
                        plugin.logger.info("Successfully loaded language file: $language.yml from resources")
                        return config
                    }
                }
            } catch (e: Exception) {
                plugin.logger.warning("无法从资源加载语言文件 $language.yml: ${e.message}")
            }
        }

        // 如果加载失败，返回默认消息配置
        plugin.logger.warning("语言文件 $language.yml 不存在，使用默认语言")
        return messages
    }

    /**
     * 重新加载所有语言文件
     */
    fun reloadLanguages() {
        languageMessages.clear()
        plugin.logger.info("已清除语言缓存，将重新加载语言文件")
    }
    
    /**
     * 发送消息
     */
    fun sendMessage(sender: CommandSender, message: String) {
        sender.sendMessage(color(message))
    }
    
    /**
     * 处理占位符
     */
    fun process(message: String, vararg args: Pair<String, String>): String {
        var result = message
        args.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
}
