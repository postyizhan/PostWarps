package com.github.postyizhan.gui.loader

import com.github.postyizhan.PostWarps
import com.github.postyizhan.constants.ConfigurableConstants
import com.github.postyizhan.constants.PluginConstants
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 菜单加载器 - 负责从文件系统加载菜单配置
 * 职责：菜单文件的读取、解析和缓存管理
 */
class MenuLoader(private val plugin: PostWarps) {
    
    // 菜单配置缓存
    private val menuConfigs = ConcurrentHashMap<String, YamlConfiguration>()

    // 默认菜单列表（从配置获取）
    private fun getDefaultMenusFromConfig(): List<String> {
        return ConfigurableConstants.Menu.getDefaultMenus(plugin)
    }
    
    /**
     * 加载所有菜单配置
     * @return 加载成功的菜单数量
     */
    fun loadAllMenus(): Int {
        // 清除旧配置
        menuConfigs.clear()

        // 确保菜单目录存在
        val menuDir = ensureMenuDirectory()

        // 保存默认菜单文件
        saveDefaultMenus(menuDir)

        // 加载所有菜单文件
        return loadMenuFiles(menuDir)
    }
    
    /**
     * 重新加载所有菜单配置
     * @return 加载成功的菜单数量
     */
    fun reloadAllMenus(): Int {
        return loadAllMenus()
    }
    
    /**
     * 获取指定菜单的配置
     * @param menuName 菜单名称
     * @return 菜单配置，如果不存在则返回null
     */
    fun getMenuConfig(menuName: String): YamlConfiguration? {
        return menuConfigs[menuName]
    }
    
    /**
     * 获取所有已加载的菜单名称
     * @return 菜单名称列表
     */
    fun getLoadedMenuNames(): Set<String> {
        return menuConfigs.keys.toSet()
    }
    
    /**
     * 检查指定菜单是否已加载
     * @param menuName 菜单名称
     * @return 如果已加载则返回true
     */
    fun isMenuLoaded(menuName: String): Boolean {
        return menuConfigs.containsKey(menuName)
    }
    
    /**
     * 获取菜单配置数量
     * @return 已加载的菜单配置数量
     */
    fun getLoadedMenuCount(): Int {
        return menuConfigs.size
    }
    
    /**
     * 确保菜单目录存在
     * @return 菜单目录文件对象
     */
    private fun ensureMenuDirectory(): File {
        val menuDirName = ConfigurableConstants.Menu.getMenuDirectory(plugin)
        val menuDir = File(plugin.dataFolder, menuDirName)
        if (!menuDir.exists()) {
            menuDir.mkdirs()
            logDebug("创建菜单目录: ${menuDir.absolutePath}")
        }
        return menuDir
    }
    
    /**
     * 保存默认菜单文件
     * @param menuDir 菜单目录
     */
    private fun saveDefaultMenus(menuDir: File) {
        for (menuName in getDefaultMenusFromConfig()) {
            val menuFile = File(menuDir, "$menuName${PluginConstants.Menu.MENU_FILE_EXTENSION}")
            if (!menuFile.exists()) {
                try {
                    plugin.saveResource("${PluginConstants.Menu.MENU_DIRECTORY}/$menuName${PluginConstants.Menu.MENU_FILE_EXTENSION}", false)
                    logDebug("Saved default menu file: $menuName${PluginConstants.Menu.MENU_FILE_EXTENSION}")
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to save default menu file $menuName${PluginConstants.Menu.MENU_FILE_EXTENSION}: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 加载菜单文件
     * @param menuDir 菜单目录
     * @return 加载成功的菜单数量
     */
    private fun loadMenuFiles(menuDir: File): Int {
        val menuFiles = menuDir.listFiles { file ->
            file.isFile && file.name.endsWith(PluginConstants.Menu.MENU_FILE_EXTENSION)
        } ?: return 0
        
        var loadedCount = 0
        
        for (file in menuFiles) {
            try {
                val menuName = file.nameWithoutExtension
                val config = YamlConfiguration.loadConfiguration(file)
                
                // 直接加载菜单配置
                menuConfigs[menuName] = config
                loadedCount++
                logDebug("Successfully loaded menu: $menuName")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load menu file ${file.name}: ${e.message}")
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace()
                }
            }
        }
        
        // 输出加载结果
        val prefix = plugin.getConfigManager().getConfig().getString("prefix", "&8[&3Post&bWarps&8] ")
        plugin.server.consoleSender.sendMessage(
            ChatColor.translateAlternateColorCodes('&', "$prefix&7Successfully loaded &a$loadedCount &7menus")
        )
        
        return loadedCount
    }
    

    
    /**
     * 清理所有缓存
     */
    fun clearCache() {
        menuConfigs.clear()
        logDebug("Cleared menu configuration cache")
    }
    
    /**
     * 获取默认菜单列表
     * @return 默认菜单名称列表
     */
    fun getDefaultMenus(): List<String> {
        return getDefaultMenusFromConfig()
    }
    
    /**
     * 记录调试信息
     * @param message 调试消息
     */
    private fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] MenuLoader: $message")
        }
    }
}
