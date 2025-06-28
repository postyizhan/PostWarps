package com.github.postyizhan.gui.cache

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.Menu
import com.github.postyizhan.gui.ModularMenu
import com.github.postyizhan.gui.util.MenuCache
import org.bukkit.configuration.file.YamlConfiguration
import java.util.concurrent.ConcurrentHashMap

/**
 * 类型安全的菜单缓存管理器
 * 职责：管理Menu和ModularMenu实例的缓存，提供类型安全的访问
 */
class TypeSafeMenuCache(private val plugin: PostWarps) {
    
    // 传统菜单缓存
    private val legacyMenuCache = ConcurrentHashMap<String, Menu>()
    
    // 模块化菜单缓存
    private val modularMenuCache = ConcurrentHashMap<String, ModularMenu>()
    
    // 菜单工具缓存
    private val utilityCache = MenuCache()
    
    /**
     * 获取传统菜单实例
     * @param menuName 菜单名称
     * @param config 菜单配置
     * @return Menu实例，如果不存在则创建新实例
     */
    fun getLegacyMenu(menuName: String, config: YamlConfiguration): Menu {
        return legacyMenuCache.computeIfAbsent(menuName) {
            logDebug("创建传统菜单实例: $menuName")
            Menu(plugin, menuName, config)
        }
    }
    
    /**
     * 获取模块化菜单实例
     * @param menuName 菜单名称
     * @param config 菜单配置
     * @return ModularMenu实例，如果不存在则创建新实例
     */
    fun getModularMenu(menuName: String, config: YamlConfiguration): ModularMenu {
        return modularMenuCache.computeIfAbsent(menuName) {
            logDebug("创建模块化菜单实例: $menuName")
            ModularMenu(plugin, menuName, config, utilityCache)
        }
    }
    
    /**
     * 检查传统菜单是否已缓存
     * @param menuName 菜单名称
     * @return 如果已缓存则返回true
     */
    fun hasLegacyMenu(menuName: String): Boolean {
        return legacyMenuCache.containsKey(menuName)
    }
    
    /**
     * 检查模块化菜单是否已缓存
     * @param menuName 菜单名称
     * @return 如果已缓存则返回true
     */
    fun hasModularMenu(menuName: String): Boolean {
        return modularMenuCache.containsKey(menuName)
    }
    
    /**
     * 移除指定的传统菜单缓存
     * @param menuName 菜单名称
     * @return 被移除的Menu实例，如果不存在则返回null
     */
    fun removeLegacyMenu(menuName: String): Menu? {
        val removed = legacyMenuCache.remove(menuName)
        if (removed != null) {
            logDebug("移除传统菜单缓存: $menuName")
        }
        return removed
    }
    
    /**
     * 移除指定的模块化菜单缓存
     * @param menuName 菜单名称
     * @return 被移除的ModularMenu实例，如果不存在则返回null
     */
    fun removeModularMenu(menuName: String): ModularMenu? {
        val removed = modularMenuCache.remove(menuName)
        if (removed != null) {
            logDebug("移除模块化菜单缓存: $menuName")
        }
        return removed
    }
    
    /**
     * 清除所有传统菜单缓存
     */
    fun clearLegacyMenus() {
        val count = legacyMenuCache.size
        legacyMenuCache.clear()
        logDebug("清除 $count 个传统菜单缓存")
    }
    
    /**
     * 清除所有模块化菜单缓存
     */
    fun clearModularMenus() {
        val count = modularMenuCache.size
        modularMenuCache.clear()
        logDebug("清除 $count 个模块化菜单缓存")
    }
    
    /**
     * 清除所有菜单缓存
     */
    fun clearAllMenus() {
        clearLegacyMenus()
        clearModularMenus()
        utilityCache.clearAllCache()
        logDebug("清除所有菜单缓存")
    }
    
    /**
     * 获取传统菜单缓存统计
     * @return 缓存统计信息
     */
    fun getLegacyMenuStats(): CacheStats {
        return CacheStats(
            type = "Legacy Menu",
            count = legacyMenuCache.size,
            keys = legacyMenuCache.keys.toList()
        )
    }
    
    /**
     * 获取模块化菜单缓存统计
     * @return 缓存统计信息
     */
    fun getModularMenuStats(): CacheStats {
        return CacheStats(
            type = "Modular Menu",
            count = modularMenuCache.size,
            keys = modularMenuCache.keys.toList()
        )
    }
    
    /**
     * 获取工具缓存统计
     * @return 缓存统计信息
     */
    fun getUtilityCacheStats(): MenuCache.CacheStats {
        return utilityCache.getCacheStats()
    }
    
    /**
     * 获取所有缓存统计信息
     * @return 完整的缓存统计
     */
    fun getAllCacheStats(): AllCacheStats {
        return AllCacheStats(
            legacyMenus = getLegacyMenuStats(),
            modularMenus = getModularMenuStats(),
            utilityCache = getUtilityCacheStats()
        )
    }
    
    /**
     * 清理过期的工具缓存
     */
    fun cleanupExpiredCache() {
        try {
            utilityCache.cleanupExpiredCache()
            logDebug("清理过期工具缓存完成")
        } catch (e: Exception) {
            plugin.logger.warning("清理过期缓存时发生错误: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 清理指定玩家的缓存
     * @param player 玩家对象
     */
    fun clearPlayerCache(player: org.bukkit.entity.Player) {
        utilityCache.clearPlayerCache(player)
        logDebug("清理玩家 ${player.name} 的缓存")
    }
    
    /**
     * 记录调试信息
     * @param message 调试消息
     */
    private fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] TypeSafeMenuCache: $message")
        }
    }
    
    /**
     * 缓存统计数据类
     */
    data class CacheStats(
        val type: String,
        val count: Int,
        val keys: List<String>
    )
    
    /**
     * 完整缓存统计数据类
     */
    data class AllCacheStats(
        val legacyMenus: CacheStats,
        val modularMenus: CacheStats,
        val utilityCache: MenuCache.CacheStats
    ) {
        val totalMenuCount: Int
            get() = legacyMenus.count + modularMenus.count
    }
}
