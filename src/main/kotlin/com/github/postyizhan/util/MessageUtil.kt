package com.github.postyizhan.util

import com.github.postyizhan.PostWarps
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

/**
 * 消息工具类，负责处理消息发送和颜色处理
 */
object MessageUtil {
    
    private lateinit var plugin: PostWarps
    private lateinit var messages: YamlConfiguration
    private var prefix: String = ""
    
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
     * 获取消息
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
     * 处理颜色代码
     */
    fun color(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', message)
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
