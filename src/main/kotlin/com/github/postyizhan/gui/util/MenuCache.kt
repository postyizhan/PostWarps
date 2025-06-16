package com.github.postyizhan.gui.util

import com.github.postyizhan.gui.core.MenuData
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 菜单缓存管理器
 * 提供多层缓存机制以提高性能
 */
class MenuCache {
    
    // 菜单数据缓存
    private val dataCache = ConcurrentHashMap<String, CacheEntry<MenuData>>()
    
    // 玩家特定数据缓存
    private val playerDataCache = ConcurrentHashMap<String, CacheEntry<MenuData>>()
    
    // 缓存过期时间（毫秒）
    private val cacheExpireTime = TimeUnit.MINUTES.toMillis(5) // 5分钟
    
    /**
     * 获取菜单数据缓存
     * @param key 缓存键
     * @return 缓存的数据，如果不存在或已过期返回null
     */
    fun getMenuData(key: String): MenuData? {
        val entry = dataCache[key]
        return if (entry != null && !entry.isExpired()) {
            entry.data
        } else {
            dataCache.remove(key)
            null
        }
    }
    
    /**
     * 缓存菜单数据
     * @param key 缓存键
     * @param data 要缓存的数据
     */
    fun cacheMenuData(key: String, data: MenuData) {
        dataCache[key] = CacheEntry(data, System.currentTimeMillis() + cacheExpireTime)
    }
    
    /**
     * 获取玩家特定的菜单数据缓存
     * @param player 玩家
     * @param menuName 菜单名称
     * @return 缓存的数据，如果不存在或已过期返回null
     */
    fun getPlayerMenuData(player: Player, menuName: String): MenuData? {
        val key = "${player.uniqueId}_$menuName"
        val entry = playerDataCache[key]
        return if (entry != null && !entry.isExpired()) {
            entry.data
        } else {
            playerDataCache.remove(key)
            null
        }
    }
    
    /**
     * 缓存玩家特定的菜单数据
     * @param player 玩家
     * @param menuName 菜单名称
     * @param data 要缓存的数据
     */
    fun cachePlayerMenuData(player: Player, menuName: String, data: MenuData) {
        val key = "${player.uniqueId}_$menuName"
        playerDataCache[key] = CacheEntry(data, System.currentTimeMillis() + cacheExpireTime)
    }
    
    /**
     * 清除玩家的所有缓存
     * @param player 玩家
     */
    fun clearPlayerCache(player: Player) {
        val playerUuid = player.uniqueId.toString()
        playerDataCache.keys.removeIf { it.startsWith(playerUuid) }
    }
    
    /**
     * 清除指定菜单的所有缓存
     * @param menuName 菜单名称
     */
    fun clearMenuCache(menuName: String) {
        dataCache.keys.removeIf { it.contains(menuName) }
        playerDataCache.keys.removeIf { it.endsWith("_$menuName") }
    }
    
    /**
     * 清除所有过期缓存
     */
    fun cleanupExpiredCache() {
        val currentTime = System.currentTimeMillis()
        
        dataCache.entries.removeIf { it.value.expireTime < currentTime }
        playerDataCache.entries.removeIf { it.value.expireTime < currentTime }
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        dataCache.clear()
        playerDataCache.clear()
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            menuDataCacheSize = dataCache.size,
            playerDataCacheSize = playerDataCache.size,
            totalCacheSize = dataCache.size + playerDataCache.size
        )
    }
    
    /**
     * 缓存条目
     */
    private data class CacheEntry<T>(
        val data: T,
        val expireTime: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
    }
    
    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val menuDataCacheSize: Int,
        val playerDataCacheSize: Int,
        val totalCacheSize: Int
    )
}
