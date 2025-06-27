package com.github.postyizhan.gui

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.util.MenuCache
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 菜单管理器，负责加载和管理GUI菜单
 */
class MenuManager(private val plugin: PostWarps) {

    // 存储菜单配置
    private val menuConfigs = ConcurrentHashMap<String, YamlConfiguration>()

    // 存储菜单缓存（支持新旧两种菜单类型）
    private val menuCache = ConcurrentHashMap<String, Any>() // Menu 或 ModularMenu

    // 玩家当前打开的菜单
    private val playerMenus = ConcurrentHashMap<Player, String>()

    // 玩家菜单数据
    private val playerData = ConcurrentHashMap<Player, MutableMap<String, Any>>()

    // 菜单缓存管理器
    private val cache = MenuCache()

    // 定时任务执行器，用于清理过期缓存
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "MenuManager-Cache-Cleaner").apply {
            isDaemon = true
        }
    }
    
    init {
        // 启动缓存清理定时任务
        scheduler.scheduleAtFixedRate({
            try {
                cache.cleanupExpiredCache()
            } catch (e: Exception) {
                plugin.logger.warning("清理菜单缓存时发生错误: ${e.message}")
            }
        }, 5, 5, TimeUnit.MINUTES)
    }

    /**
     * 加载所有菜单
     */
    fun loadMenus() {
        // 清除旧数据
        menuConfigs.clear()
        menuCache.clear()
        cache.clearAllCache()

        // 确保菜单目录存在
        val menuDir = File(plugin.dataFolder, "menu")
        if (!menuDir.exists()) {
            menuDir.mkdirs()

            // 保存默认菜单
            saveDefaultMenus()
        }

        // 加载所有菜单文件
        val menuFiles = menuDir.listFiles { file -> file.isFile && file.name.endsWith(".yml") }
        if (menuFiles != null) {
            var count = 0
            for (file in menuFiles) {
                try {
                    val menuName = file.nameWithoutExtension
                    val config = YamlConfiguration.loadConfiguration(file)
                    menuConfigs[menuName] = config
                    count++
                } catch (e: Exception) {
                    plugin.logger.warning("无法加载菜单 ${file.name}: ${e.message}")
                }
            }

            plugin.server.consoleSender.sendMessage(ChatColor.translateAlternateColorCodes(
                '&',
                plugin.getConfigManager().getConfig().getString("prefix", "&8[&3Post&bWarps&8] ") +
                    "&7成功加载 &a$count &7个菜单"
            ))
        }
    }
    
    /**
     * 保存默认菜单
     */
    private fun saveDefaultMenus() {
        val menuNames = listOf("main", "create", "private_warps", "public_warps", "settings")
        
        for (menuName in menuNames) {
            plugin.saveResource("menu/$menuName.yml", false)
        }
    }
    
    /**
     * 打开菜单
     */
    fun openMenu(player: Player, menuName: String, data: Map<String, Any> = emptyMap()) {
        val menu = getMenu(menuName) ?: run {
            player.sendMessage("${ChatColor.RED}无法打开菜单：找不到 $menuName")
            return
        }
        
        // 存储玩家数据
        playerData.computeIfAbsent(player) { mutableMapOf() }.apply {
            putAll(data)
        }
        
        // 创建菜单物品
        menu.createInventory(player, playerData[player] ?: mutableMapOf())?.let { inventory ->
            player.openInventory(inventory)
            playerMenus[player] = menuName
        } ?: player.sendMessage("${ChatColor.RED}无法打开菜单：创建失败")
    }
    
    /**
     * 获取玩家当前打开的菜单名称
     */
    fun getOpenMenu(player: Player): String? {
        return playerMenus[player]
    }
    
    /**
     * 获取菜单
     */
    fun getMenu(name: String): Menu? {
        // 尝试从缓存获取
        val cachedMenu = menuCache[name]
        if (cachedMenu is Menu) {
            return cachedMenu
        }

        // 从配置文件加载
        val config = menuConfigs[name] ?: return null
        val menu = Menu(plugin, name, config)

        menuCache[name] = menu
        return menu
    }

    /**
     * 获取模块化菜单（新接口）
     */
    fun getModularMenu(name: String): ModularMenu? {
        // 尝试从缓存获取
        val cachedMenu = menuCache[name]
        if (cachedMenu is ModularMenu) {
            return cachedMenu
        }

        // 从配置文件加载
        val config = menuConfigs[name] ?: return null
        val menu = ModularMenu(plugin, name, config, cache)
        menuCache[name] = menu
        return menu
    }

    /**
     * 打开模块化菜单
     */
    fun openModularMenu(player: Player, menuName: String, data: Map<String, Any> = emptyMap()) {
        val menu = getModularMenu(menuName) ?: run {
            player.sendMessage("${ChatColor.RED}无法打开菜单：找不到 $menuName")
            return
        }

        // 存储玩家数据
        playerData.computeIfAbsent(player) { mutableMapOf() }.apply {
            putAll(data)
        }

        // 创建菜单物品
        menu.createInventory(player, playerData[player] ?: mutableMapOf())?.let { inventory ->
            player.openInventory(inventory)
            playerMenus[player] = menuName
        } ?: player.sendMessage("${ChatColor.RED}无法打开菜单：创建失败")
    }
    
    /**
     * 关闭所有菜单
     */
    fun closeAllMenus() {
        for (player in playerMenus.keys) {
            if (player.isOnline) {
                player.closeInventory()
            }
        }
        playerMenus.clear()
        playerData.clear()
    }
    
    /**
     * 关闭玩家的菜单
     */
    fun closeMenu(player: Player) {
        playerMenus.remove(player)
    }
    
    /**
     * 获取玩家数据
     */
    fun getPlayerData(player: Player): MutableMap<String, Any> {
        return playerData.computeIfAbsent(player) { mutableMapOf() }
    }
    
    /**
     * 设置玩家数据
     */
    fun setPlayerData(player: Player, key: String, value: Any) {
        val data = playerData.computeIfAbsent(player) { mutableMapOf() }
        data[key] = value
    }
    
    /**
     * 重新加载菜单
     */
    fun reloadMenus() {
        loadMenus()

        // 重新注册动态命令
        try {
            plugin.getDynamicCommandRegistrar().unregisterCommands()
            plugin.getDynamicCommandRegistrar().registerMenuCommands()

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Reregistered dynamic menu commands after menu reload")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to reregister dynamic commands after menu reload: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 处理玩家退出
     */
    fun handleQuit(player: Player) {
        playerMenus.remove(player)
        playerData.remove(player)
        cache.clearPlayerCache(player)
    }

    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): MenuCache.CacheStats {
        return cache.getCacheStats()
    }

    /**
     * 清除玩家的菜单缓存（用于语言切换后刷新）
     */
    fun clearPlayerMenuCache(player: Player) {
        // 清除玩家特定的缓存
        cache.clearPlayerCache(player)

        // 注意：不清除menuCache，因为那是全局的Menu对象缓存
        // Menu对象本身是无状态的，问题在于国际化处理需要在每次创建inventory时进行
    }

    /**
     * 强制刷新玩家的菜单（清除缓存并重新打开）
     */
    fun forceRefreshPlayerMenu(player: Player) {
        val currentMenu = getOpenMenu(player)
        if (currentMenu != null) {
            // 清除玩家缓存
            clearPlayerMenuCache(player)

            // 重新打开菜单
            val currentData = getPlayerData(player).toMap()
            openMenu(player, currentMenu, currentData)
        }
    }

    /**
     * 清理资源
     */
    fun shutdown() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
        cache.clearAllCache()
    }
}
