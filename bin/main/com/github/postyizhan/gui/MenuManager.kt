package com.github.postyizhan.gui

import com.github.postyizhan.PostWarps
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

/**
 * 菜单管理器，负责加载和管理GUI菜单
 */
class MenuManager(private val plugin: PostWarps) {
    
    // 存储菜单配置
    private val menuConfigs = ConcurrentHashMap<String, YamlConfiguration>()
    
    // 存储菜单缓存
    private val menuCache = ConcurrentHashMap<String, Menu>()
    
    // 玩家当前打开的菜单
    private val playerMenus = ConcurrentHashMap<Player, String>()
    
    // 玩家菜单数据
    private val playerData = ConcurrentHashMap<Player, MutableMap<String, Any>>()
    
    /**
     * 加载所有菜单
     */
    fun loadMenus() {
        // 清除旧数据
        menuConfigs.clear()
        menuCache.clear()
        
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
        var menu = menuCache[name]
        if (menu != null) {
            return menu
        }
        
        // 从配置文件加载
        val config = menuConfigs[name] ?: return null
        menu = Menu(plugin, name, config)
        menuCache[name] = menu
        return menu
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
    }
    
    /**
     * 处理玩家退出
     */
    fun handleQuit(player: Player) {
        playerMenus.remove(player)
        playerData.remove(player)
    }
}
