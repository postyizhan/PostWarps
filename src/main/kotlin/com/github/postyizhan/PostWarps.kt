package com.github.postyizhan

import com.github.postyizhan.commands.WarpCommand
import com.github.postyizhan.database.DatabaseManager
import com.github.postyizhan.i18n.I18n
import com.github.postyizhan.listeners.PlayerListener
import com.github.postyizhan.manager.WarpManager
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class PostWarps : JavaPlugin() {

    lateinit var databaseManager: DatabaseManager
    lateinit var warpManager: WarpManager
    lateinit var i18n: I18n
    
    // 数据库设置
    var useMySQL: Boolean = false
    var mysqlHost: String = "localhost"
    var mysqlPort: Int = 3306
    var mysqlDatabase: String = "minecraft"
    var mysqlUsername: String = "root"
    var mysqlPassword: String = "password"
    
    // 插件设置
    var language: String = "zh_CN"
    var debugMode: Boolean = false
    
    // 跨服设置
    var enableBungeeCord: Boolean = false

    override fun onEnable() {
        // 加载配置
        saveDefaultConfig()
        loadConfig()
        
        // 初始化国际化
        i18n = I18n(this)
        
        // 初始化数据库
        databaseManager = DatabaseManager(this)
        databaseManager.init()
        
        // 初始化地标管理器
        warpManager = WarpManager(this)
        
        // 注册命令
        registerCommands()
        
        // 注册事件监听器
        registerListeners()
        
        logger.info("PostWarps plugin enabled!")
    }

    override fun onDisable() {
        // 关闭数据库连接
        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }
        
        logger.info("PostWarps plugin disabled!")
    }
    
    override fun reloadConfig() {
        super.reloadConfig()
        loadConfig()
    }
    
    private fun loadConfig() {
        // 从配置文件读取设置
        // 数据库设置
        useMySQL = config.getBoolean("database.mysql.enabled", false)
        mysqlHost = config.getString("database.mysql.host") ?: "localhost"
        mysqlPort = config.getInt("database.mysql.port", 3306)
        mysqlDatabase = config.getString("database.mysql.database") ?: "minecraft"
        mysqlUsername = config.getString("database.mysql.username") ?: "root"
        mysqlPassword = config.getString("database.mysql.password") ?: "password"
        
        // 插件设置
        language = config.getString("settings.language") ?: "zh_CN"
        debugMode = config.getBoolean("settings.debug", false)
        
        // 跨服设置
        enableBungeeCord = config.getBoolean("bungeecord.enabled", false)
        
        // 如果配置文件不存在，则创建它
        createDefaultConfig()
    }
    
    private fun createDefaultConfig() {
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists()) {
            saveResource("config.yml", false)
        }
    }
    
    private fun registerCommands() {
        getCommand("warp")?.setExecutor(WarpCommand(this))
        
        if (debugMode) {
            logger.info("Commands registered successfully")
        }
    }
    
    private fun registerListeners() {
        server.pluginManager.registerEvents(PlayerListener(this), this)
        
        if (debugMode) {
            logger.info("Event listeners registered successfully")
        }
    }
}
