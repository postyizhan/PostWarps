package com.github.postyizhan

import com.github.postyizhan.command.CommandManager
import com.github.postyizhan.command.DynamicCommandRegistrar
import com.github.postyizhan.config.ConfigManager
import com.github.postyizhan.config.GroupConfig
import com.github.postyizhan.database.DatabaseManager
import com.github.postyizhan.gui.MenuManager
import com.github.postyizhan.integration.VaultManager
import com.github.postyizhan.integration.PlayerPointsManager
import com.github.postyizhan.integration.PlaceholderAPIManager
import com.github.postyizhan.listener.MenuListener
import com.github.postyizhan.listener.PlayerListener
import com.github.postyizhan.service.EconomyService
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
    private lateinit var dynamicCommandRegistrar: DynamicCommandRegistrar
    private lateinit var updateChecker: UpdateChecker
    private lateinit var vaultManager: VaultManager
    private lateinit var playerPointsManager: PlayerPointsManager
    private lateinit var placeholderAPIManager: PlaceholderAPIManager
    private lateinit var groupConfig: GroupConfig
    private lateinit var economyService: EconomyService
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

        // 初始化Vault集成
        vaultManager = VaultManager(this)
        vaultManager.initialize()

        // 初始化PlayerPoints集成
        playerPointsManager = PlayerPointsManager(this)
        playerPointsManager.initialize()

        // 初始化PlaceholderAPI集成
        placeholderAPIManager = PlaceholderAPIManager(this)
        placeholderAPIManager.initialize()

        // 初始化权限组配置
        groupConfig = GroupConfig(this)
        groupConfig.initialize()

        // 初始化经济服务
        economyService = EconomyService(this, vaultManager, playerPointsManager, groupConfig)

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

        // 初始化并注册动态菜单命令
        dynamicCommandRegistrar = DynamicCommandRegistrar(this)
        dynamicCommandRegistrar.registerMenuCommands()

        // 初始化更新检查器
        updateChecker = UpdateChecker(this, "postyizhan/PostWarps")

        // 检查更新
        if (configManager.getConfig().getBoolean("update-checker.enabled", true)) {
            updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
                if (isUpdateAvailable) {
                    server.consoleSender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.update_available")
                            .replace("{current_version}", description.version)
                            .replace("{latest_version}", newVersion)
                    ))
                    server.consoleSender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.update_url")
                            .replace("{current_version}", description.version)
                            .replace("{latest_version}", newVersion)
                    ))
                } else {
                    server.consoleSender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.up_to_date")
                    ))
                }
            }
        }
        
        // 输出启动消息
        logger.info("PostWarps 插件已启用 - 版本 ${description.version}")
    }
    
    /**
     * 插件禁用时触发
     */
    override fun onDisable() {
        HandlerList.unregisterAll(this)

        // 注销动态命令
        if (this::dynamicCommandRegistrar.isInitialized) {
            dynamicCommandRegistrar.unregisterCommands()
        }

        // 关闭所有菜单
        if (this::menuManager.isInitialized) {
            menuManager.closeAllMenus()
            menuManager.shutdown()
        }

        // 关闭Vault集成
        if (this::vaultManager.isInitialized) {
            vaultManager.shutdown()
        }

        // 关闭PlayerPoints集成
        if (this::playerPointsManager.isInitialized) {
            playerPointsManager.shutdown()
        }

        // 关闭PlaceholderAPI集成
        if (this::placeholderAPIManager.isInitialized) {
            placeholderAPIManager.shutdown()
        }

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
        groupConfig.reload()
        menuManager.reloadMenus()  // 使用reloadMenus而不是loadMenus，这样会重新注册动态命令
        debugEnabled = configManager.getConfig().getBoolean("debug", false)

        if (debugEnabled) {
            logger.info("[DEBUG] Plugin reloaded, dynamic commands reregistered")
        }
    }
    
    /**
     * 向玩家发送更新检查信息
     */
    fun sendUpdateInfo(player: Player) {
        updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
            if (isUpdateAvailable) {
                val updateAvailableMsg = MessageUtil.getMessage("system.updater.update_available")
                    .replace("{current_version}", description.version)
                    .replace("{latest_version}", newVersion)

                val updateUrlMsg = MessageUtil.getMessage("system.updater.update_url")
                    .replace("{current_version}", description.version)
                    .replace("{latest_version}", newVersion)

                MessageUtil.sendMessage(player, updateAvailableMsg)
                MessageUtil.sendMessage(player, updateUrlMsg)
            } else {
                val upToDateMsg = MessageUtil.getMessage("system.updater.up_to_date")
                MessageUtil.sendMessage(player, upToDateMsg)
            }
        }
    }

    /**
     * 向命令发送者发送更新检查信息（兼容性方法）
     */
    fun sendUpdateInfo(sender: CommandSender) {
        if (sender is Player) {
            sendUpdateInfo(sender)
        } else {
            updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
                if (isUpdateAvailable) {
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.update_available")
                            .replace("{current_version}", description.version)
                            .replace("{latest_version}", newVersion)
                    ))
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.update_url")
                            .replace("{current_version}", description.version)
                            .replace("{latest_version}", newVersion)
                    ))
                } else {
                    sender.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("system.updater.up_to_date")
                    ))
                }
            }
        }
    }
    
    fun getConfigManager(): ConfigManager = configManager
    fun getDatabaseManager(): DatabaseManager = databaseManager
    fun getMenuManager(): MenuManager = menuManager
    fun getDynamicCommandRegistrar(): DynamicCommandRegistrar = dynamicCommandRegistrar
    fun getUpdateChecker(): UpdateChecker = updateChecker
    fun getVaultManager(): VaultManager = vaultManager
    fun getPlayerPointsManager(): PlayerPointsManager = playerPointsManager
    fun getPlaceholderAPIManager(): PlaceholderAPIManager = placeholderAPIManager
    fun getGroupConfig(): GroupConfig = groupConfig
    fun getEconomyService(): EconomyService = economyService
    
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
