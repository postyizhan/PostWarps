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
        // 配置项和默认值的映射
        val defaults = mapOf(
            "update-checker.enabled" to true,
            "update-checker.check-interval-days" to 1,
            "language" to "zh_CN",
            "database.type" to "SQLite",
            "database.mysql.host" to "localhost",
            "database.mysql.port" to 3306,
            "database.mysql.database" to "postwarps",
            "database.mysql.username" to "root",
            "database.mysql.password" to "password",
            "database.mysql.use-ssl" to false,
            "database.mysql.pool-size" to 10,
            "database.debug" to false
        )
        
        // 批量设置默认值
        defaults.forEach { (key, value) ->
            if (!config.contains(key)) {
                config.set(key, value)
            }
        }
    }
    
    /**
     * 保存配置文件
     */
    fun saveConfig() {
        try {
            config.save(configFile)
        } catch (e: IOException) {
            plugin.logger.severe("Failed to save configuration file: ${e.message}")
        }
    }
    
    /**
     * 获取配置
     */
    fun getConfig(): FileConfiguration {
        return config
    }
}
