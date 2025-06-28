package com.github.postyizhan.constants

import com.github.postyizhan.PostWarps
import org.bukkit.configuration.file.FileConfiguration

/**
 * 可配置常量管理器 - 从配置文件中读取可配置的常量值
 * 提供类型安全的配置值访问，支持默认值和验证
 */
class ConfigurableConstants(private val plugin: PostWarps) {
    
    private val config: FileConfiguration
        get() = plugin.getConfigManager().getConfig()
    
    /**
     * 网络配置
     */
    object Network {
        fun getConnectTimeout(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("network.connect-timeout", PluginConstants.Network.DEFAULT_CONNECT_TIMEOUT)
                .coerceIn(1000, 30000) // 1-30秒
        }
        
        fun getReadTimeout(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("network.read-timeout", PluginConstants.Network.DEFAULT_READ_TIMEOUT)
                .coerceIn(1000, 60000) // 1-60秒
        }
        
        fun getUserAgent(plugin: PostWarps): String {
            return plugin.getConfigManager().getConfig()
                .getString("network.user-agent", PluginConstants.Network.USER_AGENT)
                ?: PluginConstants.Network.USER_AGENT
        }
    }
    
    /**
     * 缓存配置
     */
    object Cache {
        fun getCleanupIntervalMinutes(plugin: PostWarps): Long {
            return plugin.getConfigManager().getConfig()
                .getLong("cache.cleanup-interval-minutes", PluginConstants.Cache.DEFAULT_CLEANUP_INTERVAL_MINUTES)
                .coerceIn(1L, 60L) // 1-60分钟
        }
        
        fun getCacheExpiryMinutes(plugin: PostWarps): Long {
            return plugin.getConfigManager().getConfig()
                .getLong("cache.expiry-minutes", PluginConstants.Cache.DEFAULT_CACHE_EXPIRY_MINUTES)
                .coerceIn(5L, 1440L) // 5分钟-24小时
        }
        
        fun getMaxCacheSize(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("cache.max-size", PluginConstants.Cache.MAX_CACHE_SIZE)
                .coerceIn(100, 10000) // 100-10000条
        }
    }
    
    /**
     * 菜单配置
     */
    object Menu {
        fun getDefaultMenus(plugin: PostWarps): List<String> {
            val configMenus = plugin.getConfigManager().getConfig()
                .getStringList("menu.default-menus")
            
            return if (configMenus.isNotEmpty()) {
                configMenus.filter { it.isNotBlank() }
            } else {
                PluginConstants.Menu.DEFAULT_MENUS
            }
        }
        
        fun getMenuDirectory(plugin: PostWarps): String {
            return plugin.getConfigManager().getConfig()
                .getString("menu.directory", PluginConstants.Menu.MENU_DIRECTORY)
                ?: PluginConstants.Menu.MENU_DIRECTORY
        }
        

    }
    
    /**
     * 数据库配置
     */
    object Database {
        fun getPoolSize(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt(PluginConstants.Config.Database.MYSQL_POOL_SIZE, PluginConstants.Database.DEFAULT_POOL_SIZE)
                .coerceIn(1, 50) // 1-50个连接
        }
        
        fun getConnectionTimeout(plugin: PostWarps): Long {
            return plugin.getConfigManager().getConfig()
                .getLong("database.connection-timeout", PluginConstants.Database.CONNECTION_TIMEOUT)
                .coerceIn(5000L, 120000L) // 5-120秒
        }
        
        fun getIdleTimeout(plugin: PostWarps): Long {
            return plugin.getConfigManager().getConfig()
                .getLong("database.idle-timeout", PluginConstants.Database.IDLE_TIMEOUT)
                .coerceIn(60000L, 3600000L) // 1-60分钟
        }
        
        fun getMaxLifetime(plugin: PostWarps): Long {
            return plugin.getConfigManager().getConfig()
                .getLong("database.max-lifetime", PluginConstants.Database.MAX_LIFETIME)
                .coerceIn(300000L, 7200000L) // 5分钟-2小时
        }
        
        fun getMinIdleConnections(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("database.min-idle", PluginConstants.Database.MIN_IDLE_CONNECTIONS)
                .coerceIn(1, 10) // 1-10个连接
        }
    }
    
    /**
     * 地标配置
     */
    object Warp {
        fun getMaxNameLength(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("warp.max-name-length", PluginConstants.Warp.MAX_NAME_LENGTH)
                .coerceIn(3, 64) // 3-64字符
        }
        
        fun getMaxDescriptionLength(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("warp.max-description-length", PluginConstants.Warp.MAX_DESCRIPTION_LENGTH)
                .coerceIn(10, 500) // 10-500字符
        }
        
        fun getDefaultMaterial(plugin: PostWarps): String {
            return plugin.getConfigManager().getConfig()
                .getString("warp.default-material", PluginConstants.Database.DEFAULT_MATERIAL)
                ?: PluginConstants.Database.DEFAULT_MATERIAL
        }
        
        fun isNameValidationEnabled(plugin: PostWarps): Boolean {
            return plugin.getConfigManager().getConfig()
                .getBoolean("warp.validation.name-enabled", true)
        }
        
        fun getAllowedNamePattern(plugin: PostWarps): String {
            return plugin.getConfigManager().getConfig()
                .getString("warp.validation.name-pattern", "[a-zA-Z0-9_\\-]+")
                ?: "[a-zA-Z0-9_\\-]+"
        }
    }
    
    /**
     * 更新检查配置
     */
    object UpdateChecker {
        fun isEnabled(plugin: PostWarps): Boolean {
            return plugin.getConfigManager().getConfig()
                .getBoolean(PluginConstants.Config.KEY_UPDATE_CHECKER_ENABLED, true)
        }
        
        fun getCheckIntervalDays(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt(PluginConstants.Config.KEY_UPDATE_CHECKER_INTERVAL, 1)
                .coerceIn(1, 30) // 1-30天
        }
        
        fun getRepository(plugin: PostWarps): String {
            return plugin.getConfigManager().getConfig()
                .getString("update-checker.repository", "postyizhan/PostWarps")
                ?: "postyizhan/PostWarps"
        }
    }
    
    /**
     * 调试配置
     */
    object Debug {
        fun isEnabled(plugin: PostWarps): Boolean {
            return plugin.getConfigManager().getConfig()
                .getBoolean(PluginConstants.Config.KEY_DEBUG, PluginConstants.Config.DEFAULT_DEBUG)
        }
        
        fun getMaxLogLength(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("debug.max-log-length", PluginConstants.Debug.MAX_LOG_LENGTH)
                .coerceIn(100, 5000) // 100-5000字符
        }
        
        fun isVerboseEnabled(plugin: PostWarps): Boolean {
            return plugin.getConfigManager().getConfig()
                .getBoolean("debug.verbose", false)
        }
    }
    
    /**
     * 玩家菜单配置
     */
    object PlayerMenu {
        fun getMaxHistorySize(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("player-menu.max-history-size", PluginConstants.PlayerMenu.MAX_HISTORY_SIZE)
                .coerceIn(3, 50) // 3-50条历史记录
        }
        
        fun getDefaultMenuName(plugin: PostWarps): String {
            return plugin.getConfigManager().getConfig()
                .getString("player-menu.default-menu", PluginConstants.PlayerMenu.DEFAULT_MENU_NAME)
                ?: PluginConstants.PlayerMenu.DEFAULT_MENU_NAME
        }
        
        fun isHistoryEnabled(plugin: PostWarps): Boolean {
            return plugin.getConfigManager().getConfig()
                .getBoolean("player-menu.history-enabled", true)
        }
    }
    
    /**
     * 传送配置
     */
    object Teleport {
        fun getDefaultDelay(plugin: PostWarps): Int {
            return plugin.getConfigManager().getConfig()
                .getInt("teleport.default-delay", PluginConstants.Teleport.DEFAULT_DELAY_SECONDS)
                .coerceIn(0, PluginConstants.Teleport.MAX_DELAY_SECONDS)
        }
        
        fun isWarmupEnabled(plugin: PostWarps): Boolean {
            return plugin.getConfigManager().getConfig()
                .getBoolean("teleport.warmup-enabled", true)
        }
        
        fun isCancelOnMove(plugin: PostWarps): Boolean {
            return plugin.getConfigManager().getConfig()
                .getBoolean("teleport.cancel-on-move", true)
        }
        
        fun isCancelOnDamage(plugin: PostWarps): Boolean {
            return plugin.getConfigManager().getConfig()
                .getBoolean("teleport.cancel-on-damage", true)
        }
    }
}
