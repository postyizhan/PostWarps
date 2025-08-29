package com.github.postyizhan

import com.github.postyizhan.core.DependencyContainer
import com.github.postyizhan.core.PluginInitializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin


class PostWarps : JavaPlugin() {

    private lateinit var initializer: PluginInitializer
    private lateinit var container: DependencyContainer

    companion object {
        @Volatile
        private var instance: PostWarps? = null


        fun getInstance(): PostWarps {
            return instance ?: throw IllegalStateException("Plugin not initialized")
        }
    }


    override fun onEnable() {
        instance = this

        // 创建初始化器并执行初始化
        initializer = PluginInitializer(this)
        container = DependencyContainer.getInstance(this)

        if (!initializer.initialize()) {
            logger.severe("Plugin initialization failed, disabling plugin")
            server.pluginManager.disablePlugin(this)
            return
        }
    }


    override fun onDisable() {
        if (this::initializer.isInitialized) {
            initializer.shutdown()
        }
        instance = null
    }


    fun reload() {
        if (this::initializer.isInitialized) {
            initializer.reload()
        }
    }

    fun sendUpdateInfo(sender: CommandSender) {
        if (!this::container.isInitialized) return
        
        container.updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
            val messageUtil = com.github.postyizhan.util.MessageUtil
            val currentVersion = description.version
            
            if (isUpdateAvailable) {
                val updateMsg = messageUtil.getMessage("system.updater.update_available")
                    .replace("{current_version}", currentVersion)
                    .replace("{latest_version}", newVersion)
                val urlMsg = messageUtil.getMessage("system.updater.update_url")
                    .replace("{current_version}", currentVersion)
                    .replace("{latest_version}", newVersion)
                
                if (sender is Player) {
                    messageUtil.sendMessage(sender, updateMsg)
                    messageUtil.sendMessage(sender, urlMsg)
                } else {
                    sender.sendMessage(messageUtil.color(updateMsg))
                    sender.sendMessage(messageUtil.color(urlMsg))
                }
            } else {
                val upToDateMsg = messageUtil.getMessage("system.updater.up_to_date")
                if (sender is Player) {
                    messageUtil.sendMessage(sender, upToDateMsg)
                } else {
                    sender.sendMessage(messageUtil.color(upToDateMsg))
                }
            }
        }
    }


    fun getConfigManager() = container.configManager
    fun getDatabaseManager() = container.databaseManager
    fun getEnhancedDatabaseManager() = container.enhancedDatabaseManager
    fun getMenuManager() = container.menuManager
    fun getDynamicCommandRegistrar() = container.dynamicCommandRegistrar
    fun getUpdateChecker() = container.updateChecker
    fun getVaultManager() = container.vaultManager
    fun getPlayerPointsManager() = container.playerPointsManager
    fun getPlaceholderAPIManager() = container.placeholderAPIManager
    fun getGroupConfig() = container.groupConfig
    fun getEconomyService() = container.economyService
    fun getTeleportManager() = container.teleportManager


    fun getActionFactory() = container.actionFactory


    fun isDebugEnabled(): Boolean =
        if (this::container.isInitialized)
            container.configManager.getConfig().getBoolean("debug", false)
        else false
}
