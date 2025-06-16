package com.github.postyizhan.config

import com.github.postyizhan.PostWarps
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

/**
 * 配置管理器，负责加载和管理插件的配置
 */
class ConfigManager(private val plugin: PostWarps) {
    
    private lateinit var config: FileConfiguration
    private val configFile = File(plugin.dataFolder, "config.yml")
    
    /**
     * 加载所有配置文件
     */
    fun loadAll() {
        loadConfig()
    }
    
    /**
     * 加载主配置文件
     */
    private fun loadConfig() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig()
        }
        config = YamlConfiguration.loadConfiguration(configFile)
        
        // 添加默认值
        populateDefaults()
        
        // 保存配置文件
        saveConfig()
    }
    
    /**
     * 添加默认配置
     */
    private fun populateDefaults() {
        // 更新检查配置
        if (!config.contains("update-checker.enabled")) {
            config.set("update-checker.enabled", true)
        }
        if (!config.contains("update-checker.check-interval-days")) {
            config.set("update-checker.check-interval-days", 1)
        }
        
        // 语言配置
        if (!config.contains("language")) {
            config.set("language", "zh_CN")
        }
        
        // 数据库配置
        if (!config.contains("database.type")) {
            config.set("database.type", "SQLite")
        }
        if (!config.contains("database.mysql.host")) {
            config.set("database.mysql.host", "localhost")
        }
        if (!config.contains("database.mysql.port")) {
            config.set("database.mysql.port", 3306)
        }
        if (!config.contains("database.mysql.database")) {
            config.set("database.mysql.database", "postwarps")
        }
        if (!config.contains("database.mysql.username")) {
            config.set("database.mysql.username", "root")
        }
        if (!config.contains("database.mysql.password")) {
            config.set("database.mysql.password", "password")
        }
        if (!config.contains("database.mysql.use-ssl")) {
            config.set("database.mysql.use-ssl", false)
        }
        if (!config.contains("database.mysql.pool-size")) {
            config.set("database.mysql.pool-size", 10)
        }
        if (!config.contains("database.debug")) {
            config.set("database.debug", false)
        }
    }
    
    /**
     * 保存配置文件
     */
    fun saveConfig() {
        try {
            config.save(configFile)
        } catch (e: IOException) {
            plugin.logger.severe("无法保存配置文件: ${e.message}")
        }
    }
    
    /**
     * 获取配置
     */
    fun getConfig(): FileConfiguration {
        return config
    }
}
