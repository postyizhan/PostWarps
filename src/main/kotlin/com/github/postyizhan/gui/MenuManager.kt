package com.github.postyizhan.gui

import com.github.postyizhan.PostWarps
import com.github.postyizhan.constants.ConfigurableConstants
import com.github.postyizhan.gui.cache.TypeSafeMenuCache
import com.github.postyizhan.gui.loader.MenuLoader
import com.github.postyizhan.gui.tracker.PlayerMenuTracker
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 菜单管理器 - 重构后的简化版本
 * 职责：协调各个菜单组件，提供统一的菜单管理接口
 */
class MenuManager(private val plugin: PostWarps) {

    // 菜单加载器
    private val menuLoader = MenuLoader(plugin)

    // 类型安全的菜单缓存
    private val menuCache = TypeSafeMenuCache(plugin)

    // 玩家菜单跟踪器
    private val playerTracker = PlayerMenuTracker(plugin)

    // 定时任务执行器，用于清理过期缓存
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "MenuManager-Cache-Cleaner").apply {
            isDaemon = true
        }
    }

    init {
        // 启动缓存清理定时任务
        val cleanupInterval = ConfigurableConstants.Cache.getCleanupIntervalMinutes(plugin)
        scheduler.scheduleAtFixedRate({
            try {
                menuCache.cleanupExpiredCache()
            } catch (e: Exception) {
                plugin.logger.warning("Error occurred while cleaning menu cache: ${e.message}")
            }
        }, cleanupInterval, cleanupInterval, TimeUnit.MINUTES)
    }

    /**
     * 加载所有菜单
     */
    fun loadMenus() {
        // 清除旧缓存
        menuCache.clearAllMenus()

        // 使用菜单加载器加载所有菜单
        menuLoader.loadAllMenus()
    }

    /**
     * 打开传统菜单
     */
    fun openMenu(player: Player, menuName: String, data: Map<String, Any> = emptyMap()) {
        val menu = getMenu(menuName) ?: run {
            player.sendMessage("${ChatColor.RED}无法打开菜单：找不到 $menuName")
            return
        }

        // 设置玩家数据
        if (data.isNotEmpty()) {
            playerTracker.setPlayerData(player, data)
        }

        // 创建菜单物品
        val playerData = playerTracker.getPlayerData(player)
        menu.createInventory(player, playerData)?.let { inventory ->
            player.openInventory(inventory)
            playerTracker.setCurrentMenu(player, menuName)
        } ?: player.sendMessage("${ChatColor.RED}无法打开菜单：创建失败")
    }

    /**
     * 获取玩家当前打开的菜单名称
     */
    fun getOpenMenu(player: Player): String? {
        return playerTracker.getCurrentMenu(player)
    }

    /**
     * 获取传统菜单
     */
    fun getMenu(name: String): Menu? {
        val config = menuLoader.getMenuConfig(name) ?: return null
        return menuCache.getLegacyMenu(name, config)
    }

    /**
     * 获取模块化菜单
     */
    fun getModularMenu(name: String): ModularMenu? {
        val config = menuLoader.getMenuConfig(name) ?: return null
        return menuCache.getModularMenu(name, config)
    }

    /**
     * 打开模块化菜单
     */
    fun openModularMenu(player: Player, menuName: String, data: Map<String, Any> = emptyMap()) {
        val menu = getModularMenu(menuName) ?: run {
            player.sendMessage("${ChatColor.RED}无法打开菜单：找不到 $menuName")
            return
        }

        // 设置玩家数据
        if (data.isNotEmpty()) {
            playerTracker.setPlayerData(player, data)
        }

        // 创建菜单物品
        val playerData = playerTracker.getPlayerData(player)
        menu.createInventory(player, playerData)?.let { inventory ->
            player.openInventory(inventory)
            playerTracker.setCurrentMenu(player, menuName)
        } ?: player.sendMessage("${ChatColor.RED}无法打开菜单：创建失败")
    }

    /**
     * 关闭所有菜单
     */
    fun closeAllMenus() {
        playerTracker.closeAllMenus()
    }

    /**
     * 关闭玩家的菜单
     */
    fun closeMenu(player: Player) {
        playerTracker.closeMenu(player)
    }

    /**
     * 获取玩家数据
     */
    fun getPlayerData(player: Player): MutableMap<String, Any> {
        return playerTracker.getPlayerData(player)
    }

    /**
     * 设置玩家数据
     */
    fun setPlayerData(player: Player, key: String, value: Any) {
        playerTracker.setPlayerData(player, key, value)
    }

    /**
     * 重新加载菜单
     */
    fun reloadMenus() {
        // 重新加载菜单配置
        menuLoader.reloadAllMenus()

        // 清除菜单缓存
        menuCache.clearAllMenus()

        // 重新注册动态命令
        try {
            plugin.getDynamicCommandRegistrar().unregisterCommands()
            plugin.getDynamicCommandRegistrar().registerMenuCommands()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to re-register dynamic commands: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 处理玩家退出
     */
    fun handleQuit(player: Player) {
        playerTracker.handlePlayerQuit(player)
        menuCache.clearPlayerCache(player)
    }

    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): TypeSafeMenuCache.AllCacheStats {
        return menuCache.getAllCacheStats()
    }

    /**
     * 获取玩家菜单统计信息
     */
    fun getPlayerMenuStats(): PlayerMenuTracker.MenuStats {
        return playerTracker.getMenuStats()
    }

    /**
     * 清除玩家的菜单缓存（用于语言切换后刷新）
     */
    fun clearPlayerMenuCache(player: Player) {
        menuCache.clearPlayerCache(player)
    }

    /**
     * 强制刷新玩家的菜单（清除缓存并重新打开）
     */
    fun forceRefreshPlayerMenu(player: Player) {
        val currentMenu = playerTracker.getCurrentMenu(player)
        if (currentMenu != null) {
            // 清除玩家缓存
            clearPlayerMenuCache(player)

            // 重新打开菜单
            val currentData = playerTracker.getPlayerData(player).toMap()
            openMenu(player, currentMenu, currentData)
        }
    }

    /**
     * 获取已加载的菜单列表
     */
    fun getLoadedMenus(): Set<String> {
        return menuLoader.getLoadedMenuNames()
    }



    /**
     * 检查菜单是否已加载
     */
    fun isMenuLoaded(menuName: String): Boolean {
        return menuLoader.isMenuLoaded(menuName)
    }

    /**
     * 清理资源
     */
    fun shutdown() {
        // 关闭所有菜单
        closeAllMenus()

        // 关闭定时任务
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }

        // 清理缓存
        menuCache.clearAllMenus()
        menuLoader.clearCache()
    }

}
