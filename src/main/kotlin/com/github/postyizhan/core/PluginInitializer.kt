package com.github.postyizhan.core

import com.github.postyizhan.PostWarps
import com.github.postyizhan.listener.MenuListener
import com.github.postyizhan.listener.PlayerListener
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit

/**
 * 插件初始化器 - 负责管理插件的启动和关闭流程
 * 采用阶段性初始化，确保依赖关系正确
 */
class PluginInitializer(private val plugin: PostWarps) {
    
    private val container = DependencyContainer.getInstance(plugin)
    private var debugEnabled = false
    
    /**
     * 执行插件启动初始化
     * 按照依赖关系分阶段初始化各个组件
     */
    fun initialize(): Boolean {
        return try {
            logInfo("Starting PostWarps plugin initialization...")

            // Phase 1: Initialize core configuration
            initializeCore()

            // Phase 2: Initialize database
            initializeDatabase()

            // Phase 3: Initialize third-party integrations
            initializeIntegrations()

            // Phase 4: Initialize business services
            initializeServices()

            // Phase 5: Initialize UI and commands
            initializeUIAndCommands()

            // Phase 6: Register event listeners
            registerEventListeners()

            // Phase 7: Start update checker
            startUpdateChecker()

            logInfo("PostWarps plugin initialization completed - version ${plugin.description.version}")
            true
        } catch (e: Exception) {
            plugin.logger.severe("Plugin initialization failed: ${e.message}")
            if (debugEnabled) {
                e.printStackTrace()
            }
            false
        }
    }
    
    /**
     * 阶段1: 初始化核心配置
     */
    private fun initializeCore() {
        logDebug("Initializing core configuration...")

        // Initialize configuration manager
        container.initConfigManager()

        // Initialize message utility
        MessageUtil.init(plugin)

        // Set debug mode
        debugEnabled = container.configManager.getConfig().getBoolean("debug", false)

        logDebug("Core configuration initialization completed")
    }
    
    /**
     * 阶段2: 初始化数据库
     */
    private fun initializeDatabase() {
        logDebug("Initializing database...")

        container.initDatabaseManager()

        logDebug("Database initialization completed")
    }

    /**
     * Phase 3: Initialize third-party integrations
     */
    private fun initializeIntegrations() {
        logDebug("Initializing third-party integrations...")

        container.initIntegrations()

        logDebug("Third-party integrations initialization completed")
    }

    /**
     * Phase 4: Initialize business services
     */
    private fun initializeServices() {
        logDebug("Initializing business services...")

        container.initServices()

        logDebug("Business services initialization completed")
    }
    
    /**
     * Phase 5: Initialize UI and commands
     */
    private fun initializeUIAndCommands() {
        logDebug("Initializing UI and command system...")

        container.initMenuAndCommands()

        logDebug("UI and command system initialization completed")
    }

    /**
     * Phase 6: Register event listeners
     */
    private fun registerEventListeners() {
        logDebug("Registering event listeners...")

        Bukkit.getPluginManager().registerEvents(MenuListener(plugin), plugin)
        Bukkit.getPluginManager().registerEvents(PlayerListener(plugin), plugin)

        logDebug("Event listeners registration completed")
    }
    
    /**
     * Phase 7: Start update checker
     */
    private fun startUpdateChecker() {
        if (!container.configManager.getConfig().getBoolean("update-checker.enabled", true)) {
            logDebug("Update checker is disabled")
            return
        }

        logDebug("Starting update checker...")

        container.updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
            if (isUpdateAvailable) {
                plugin.server.consoleSender.sendMessage(MessageUtil.color(
                    MessageUtil.getMessage("system.updater.update_available")
                        .replace("{current_version}", plugin.description.version)
                        .replace("{latest_version}", newVersion)
                ))
                plugin.server.consoleSender.sendMessage(MessageUtil.color(
                    MessageUtil.getMessage("system.updater.update_url")
                        .replace("{current_version}", plugin.description.version)
                        .replace("{latest_version}", newVersion)
                ))
            } else {
                plugin.server.consoleSender.sendMessage(MessageUtil.color(
                    MessageUtil.getMessage("system.updater.up_to_date")
                ))
            }
        }

        logDebug("Update checker startup completed")
    }
    
    /**
     * Execute plugin shutdown cleanup
     */
    fun shutdown() {
        logInfo("Starting PostWarps plugin shutdown...")

        try {
            // Unregister all event listeners
            org.bukkit.event.HandlerList.unregisterAll(plugin)

            // Shutdown all components
            container.shutdown()

            // Cleanup dependency container
            DependencyContainer.cleanup()

            logInfo("PostWarps plugin shutdown completed")
        } catch (e: Exception) {
            plugin.logger.warning("Error occurred during plugin shutdown: ${e.message}")
            if (debugEnabled) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Reload plugin configuration
     */
    fun reload() {
        logInfo("Reloading plugin configuration...")

        try {
            // Reload configuration
            container.configManager.loadAll()
            MessageUtil.init(plugin)
            container.groupConfig.reload()
            container.menuManager.reloadMenus()

            // Update debug mode
            debugEnabled = container.configManager.getConfig().getBoolean("debug", false)

            logInfo("Plugin configuration reload completed")
            logDebug("Dynamic commands have been re-registered")
        } catch (e: Exception) {
            plugin.logger.warning("Error occurred while reloading configuration: ${e.message}")
            if (debugEnabled) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 检查插件是否完全初始化
     */
    fun isInitialized(): Boolean {
        return container.isFullyInitialized()
    }
    
    /**
     * 获取依赖容器
     */
    fun getContainer(): DependencyContainer {
        return container
    }
    
    /**
     * 记录信息日志
     */
    private fun logInfo(message: String) {
        plugin.logger.info(message)
    }
    
    /**
     * 记录调试日志
     */
    private fun logDebug(message: String) {
        if (debugEnabled) {
            plugin.logger.info("[DEBUG] $message")
        }
    }
}
