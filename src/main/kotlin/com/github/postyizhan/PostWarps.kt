package com.github.postyizhan

import com.github.postyizhan.command.CommandManager
import com.github.postyizhan.config.ConfigManager
import com.github.postyizhan.database.DatabaseManager
import com.github.postyizhan.gui.MenuManager
import com.github.postyizhan.listener.MenuListener
import com.github.postyizhan.listener.PlayerListener
import com.github.postyizhan.util.MessageUtil
import com.github.postyizhan.util.UpdateChecker
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

/**
 * PostWarps主类
 */
class PostWarps : JavaPlugin() {

    private lateinit var configManager: ConfigManager
    private lateinit var databaseManager: DatabaseManager
    private lateinit var menuManager: MenuManager
    private lateinit var commandManager: CommandManager
    private lateinit var updateChecker: UpdateChecker
    private var debugEnabled: Boolean = false
    private lateinit var actionFactory: com.github.postyizhan.util.action.ActionFactory
    
    companion object {
        private lateinit var instance: PostWarps
        
        fun getInstance(): PostWarps {
            return instance
        }
    }
    
    /**
     * 插件启用时触发
     */
    override fun onEnable() {
        instance = this
        
        // 初始化配置管理器
        configManager = ConfigManager(this).apply { loadAll() }
        
        // 初始化消息工具
        MessageUtil.init(this)
        
        // 设置调试模式
        debugEnabled = configManager.getConfig().getBoolean("debug", false)
        
        // 初始化数据库管理器
        databaseManager = DatabaseManager(this).apply { init() }
        
        // 初始化菜单管理器
        menuManager = MenuManager(this).apply { loadMenus() }
        
        // 初始化动作工厂
        actionFactory = com.github.postyizhan.util.action.ActionFactory(this)
        
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(MenuListener(this), this)
        Bukkit.getPluginManager().registerEvents(PlayerListener(this), this)
        
        // 注册命令
        commandManager = CommandManager(this)
        commandManager.registerCommands()
        
        // 初始化更新检查器
        updateChecker = UpdateChecker(this, "postyizhan/PostWarps")
        if (configManager.getConfig().getBoolean("update-checker.enabled", true)) {
            updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
                if (isUpdateAvailable) {
                    logger.info("发现新版本：$newVersion，当前版本：${description.version}")
                } else {
                    logger.info("插件已是最新版本 ${description.version}")
                }
            }
        }
        
        // 注册命令处理器
        getCommand("postwarps")?.setExecutor { sender, _, _, args ->
            if (args.isEmpty()) {
                // 显示帮助信息
                return@setExecutor true
            }
            
            when (args[0].lowercase()) {
                "reload" -> {
                    if (sender.hasPermission("postwarps.admin")) {
                        reload()
                        sender.sendMessage("§a插件已重新载入。")
                    } else {
                        sender.sendMessage("§c你没有权限执行此命令。")
                    }
                    return@setExecutor true
                }
                "menu" -> {
                    if (sender is org.bukkit.entity.Player && sender.hasPermission("postwarps.menu")) {
                        menuManager.openMenu(sender, if (args.size > 1) args[1] else "main")
                    } else {
                        sender.sendMessage("§c只有玩家可以执行此命令。")
                    }
                    return@setExecutor true
                }
            }
            
            true
        }
        
        // 输出启动消息
        logger.info("PostWarps 插件已启用 - 版本 ${description.version}")
    }
    
    /**
     * 插件禁用时触发
     */
    override fun onDisable() {
        HandlerList.unregisterAll(this)
        
        // 关闭所有菜单
        menuManager.closeAllMenus()
        
        // 关闭数据库连接
        if (this::databaseManager.isInitialized) {
            databaseManager.close()
        }
        
        // 输出关闭消息
        logger.info("PostWarps 插件已禁用")
    }
    
    /**
     * 重新加载插件配置
     */
    fun reload() {
        configManager.loadAll()
        MessageUtil.init(this)
        menuManager.loadMenus()
        debugEnabled = configManager.getConfig().getBoolean("debug", false)
    }
    
    /**
     * 发送更新信息给指定发送者
     */
    fun sendUpdateInfo(sender: CommandSender) {
        updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
            if (isUpdateAvailable) {
                sender.sendMessage("§a发现新版本：$newVersion，当前版本：${description.version}")
            }
        }
    }
    
    fun getConfigManager(): ConfigManager = configManager
    fun getDatabaseManager(): DatabaseManager = databaseManager
    fun getMenuManager(): MenuManager = menuManager
    fun getUpdateChecker(): UpdateChecker = updateChecker
    
    /**
     * 获取动作工厂
     * @return 动作工厂实例
     */
    fun getActionFactory(): com.github.postyizhan.util.action.ActionFactory = actionFactory
    
    /**
     * 检查调试模式是否启用
     * @return 如果调试模式启用则返回true，否则返回false
     */
    fun isDebugEnabled(): Boolean = debugEnabled
}
