package com.github.postyizhan.core

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.CommandManager
import com.github.postyizhan.command.DynamicCommandRegistrar
import com.github.postyizhan.config.ConfigManager
import com.github.postyizhan.config.GroupConfig
import com.github.postyizhan.database.DatabaseManager
import com.github.postyizhan.gui.MenuManager
import com.github.postyizhan.integration.PlaceholderAPIManager
import com.github.postyizhan.integration.PlayerPointsManager
import com.github.postyizhan.integration.VaultManager
import com.github.postyizhan.service.EconomyService
import com.github.postyizhan.teleport.TeleportManager
import com.github.postyizhan.util.UpdateChecker
import com.github.postyizhan.util.action.ActionFactory

/**
 * 依赖容器 - 管理所有插件组件的生命周期
 * 采用单例模式，提供统一的依赖管理
 */
class DependencyContainer private constructor(private val plugin: PostWarps) {
    
    // 核心管理器
    private var _configManager: ConfigManager? = null
    private var _databaseManager: DatabaseManager? = null
    private var _menuManager: MenuManager? = null
    private var _commandManager: CommandManager? = null
    private var _dynamicCommandRegistrar: DynamicCommandRegistrar? = null
    
    // 集成管理器
    private var _vaultManager: VaultManager? = null
    private var _playerPointsManager: PlayerPointsManager? = null
    private var _placeholderAPIManager: PlaceholderAPIManager? = null
    
    // 业务服务
    private var _groupConfig: GroupConfig? = null
    private var _economyService: EconomyService? = null
    private var _teleportManager: TeleportManager? = null
    private var _updateChecker: UpdateChecker? = null
    private var _actionFactory: ActionFactory? = null
    
    companion object {
        @Volatile
        private var instance: DependencyContainer? = null
        
        /**
         * 获取依赖容器实例
         */
        fun getInstance(plugin: PostWarps): DependencyContainer {
            return instance ?: synchronized(this) {
                instance ?: DependencyContainer(plugin).also { instance = it }
            }
        }
        
        /**
         * 获取当前实例（必须先初始化）
         */
        fun getInstance(): DependencyContainer {
            return instance ?: throw IllegalStateException("DependencyContainer not initialized")
        }
        
        /**
         * 清理实例
         */
        fun cleanup() {
            instance = null
        }
    }
    
    // 属性访问器 - 懒加载模式
    val configManager: ConfigManager
        get() = _configManager ?: throw IllegalStateException("ConfigManager not initialized")
    
    val databaseManager: DatabaseManager
        get() = _databaseManager ?: throw IllegalStateException("DatabaseManager not initialized")
    
    val menuManager: MenuManager
        get() = _menuManager ?: throw IllegalStateException("MenuManager not initialized")
    
    val commandManager: CommandManager
        get() = _commandManager ?: throw IllegalStateException("CommandManager not initialized")
    
    val dynamicCommandRegistrar: DynamicCommandRegistrar
        get() = _dynamicCommandRegistrar ?: throw IllegalStateException("DynamicCommandRegistrar not initialized")
    
    val vaultManager: VaultManager
        get() = _vaultManager ?: throw IllegalStateException("VaultManager not initialized")
    
    val playerPointsManager: PlayerPointsManager
        get() = _playerPointsManager ?: throw IllegalStateException("PlayerPointsManager not initialized")
    
    val placeholderAPIManager: PlaceholderAPIManager
        get() = _placeholderAPIManager ?: throw IllegalStateException("PlaceholderAPIManager not initialized")
    
    val groupConfig: GroupConfig
        get() = _groupConfig ?: throw IllegalStateException("GroupConfig not initialized")
    
    val economyService: EconomyService
        get() = _economyService ?: throw IllegalStateException("EconomyService not initialized")
    
    val teleportManager: TeleportManager
        get() = _teleportManager ?: throw IllegalStateException("TeleportManager not initialized")
    
    val updateChecker: UpdateChecker
        get() = _updateChecker ?: throw IllegalStateException("UpdateChecker not initialized")
    
    val actionFactory: ActionFactory
        get() = _actionFactory ?: throw IllegalStateException("ActionFactory not initialized")
    
    /**
     * 初始化配置管理器
     */
    fun initConfigManager(): ConfigManager {
        if (_configManager == null) {
            _configManager = ConfigManager(plugin).apply { loadAll() }
        }
        return _configManager!!
    }
    
    /**
     * 初始化数据库管理器
     */
    fun initDatabaseManager(): DatabaseManager {
        if (_databaseManager == null) {
            _databaseManager = DatabaseManager(plugin).apply { init() }
        }
        return _databaseManager!!
    }
    
    /**
     * 初始化集成管理器
     */
    fun initIntegrations() {
        _vaultManager = VaultManager(plugin).apply { initialize() }
        _playerPointsManager = PlayerPointsManager(plugin).apply { initialize() }
        _placeholderAPIManager = PlaceholderAPIManager(plugin).apply { initialize() }
    }
    
    /**
     * 初始化业务服务
     */
    fun initServices() {
        _groupConfig = GroupConfig(plugin).apply { initialize() }
        _economyService = EconomyService(plugin, vaultManager, playerPointsManager, groupConfig)
        _teleportManager = TeleportManager(plugin).apply { initialize() }
        _updateChecker = UpdateChecker(plugin, "postyizhan/PostWarps")
        _actionFactory = ActionFactory(plugin)
    }
    
    /**
     * 初始化菜单和命令管理器
     */
    fun initMenuAndCommands() {
        _menuManager = MenuManager(plugin).apply { loadMenus() }
        _commandManager = CommandManager(plugin).apply { registerCommands() }
        _dynamicCommandRegistrar = DynamicCommandRegistrar(plugin).apply { registerMenuCommands() }
    }
    
    /**
     * 检查所有组件是否已初始化
     */
    fun isFullyInitialized(): Boolean {
        return _configManager != null &&
                _databaseManager != null &&
                _menuManager != null &&
                _commandManager != null &&
                _dynamicCommandRegistrar != null &&
                _vaultManager != null &&
                _playerPointsManager != null &&
                _placeholderAPIManager != null &&
                _groupConfig != null &&
                _economyService != null &&
                _teleportManager != null &&
                _updateChecker != null &&
                _actionFactory != null
    }
    
    /**
     * 关闭所有组件
     */
    fun shutdown() {
        // 按依赖关系逆序关闭
        _dynamicCommandRegistrar?.unregisterCommands()
        _teleportManager?.shutdown()
        _menuManager?.let {
            it.closeAllMenus()
            it.shutdown()
        }
        _vaultManager?.shutdown()
        _playerPointsManager?.shutdown()
        _placeholderAPIManager?.shutdown()
        _databaseManager?.close()
        
        // 清理引用
        _configManager = null
        _databaseManager = null
        _menuManager = null
        _commandManager = null
        _dynamicCommandRegistrar = null
        _vaultManager = null
        _playerPointsManager = null
        _placeholderAPIManager = null
        _groupConfig = null
        _economyService = null
        _teleportManager = null
        _updateChecker = null
        _actionFactory = null
    }
}
