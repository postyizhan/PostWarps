package com.github.postyizhan

import com.github.postyizhan.core.DependencyContainer
import com.github.postyizhan.core.PluginInitializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * PostWarps主类 - 重构后的简化版本
 * 职责：作为插件入口点，委托具体工作给专门的管理器
 */
class PostWarps : JavaPlugin() {

    private lateinit var initializer: PluginInitializer
    private lateinit var container: DependencyContainer

    companion object {
        @Volatile
        private var instance: PostWarps? = null

        /**
         * 获取插件实例
         */
        fun getInstance(): PostWarps {
            return instance ?: throw IllegalStateException("Plugin not initialized")
        }
    }

    /**
     * 插件启用时触发
     */
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

    /**
     * 插件禁用时触发
     */
    override fun onDisable() {
        if (this::initializer.isInitialized) {
            initializer.shutdown()
        }
        instance = null
    }

    /**
     * 重新加载插件配置
     */
    fun reload() {
        if (this::initializer.isInitialized) {
            initializer.reload()
        }
    }

    /**
     * 向玩家发送更新检查信息
     */
    fun sendUpdateInfo(player: Player) {
        if (this::container.isInitialized) {
            container.updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
                if (isUpdateAvailable) {
                    val updateAvailableMsg = com.github.postyizhan.util.MessageUtil.getMessage("system.updater.update_available")
                        .replace("{current_version}", description.version)
                        .replace("{latest_version}", newVersion)

                    val updateUrlMsg = com.github.postyizhan.util.MessageUtil.getMessage("system.updater.update_url")
                        .replace("{current_version}", description.version)
                        .replace("{latest_version}", newVersion)

                    com.github.postyizhan.util.MessageUtil.sendMessage(player, updateAvailableMsg)
                    com.github.postyizhan.util.MessageUtil.sendMessage(player, updateUrlMsg)
                } else {
                    val upToDateMsg = com.github.postyizhan.util.MessageUtil.getMessage("system.updater.up_to_date")
                    com.github.postyizhan.util.MessageUtil.sendMessage(player, upToDateMsg)
                }
            }
        }
    }

    /**
     * 向命令发送者发送更新检查信息（兼容性方法）
     */
    fun sendUpdateInfo(sender: CommandSender) {
        if (sender is Player) {
            sendUpdateInfo(sender)
        } else if (this::container.isInitialized) {
            container.updateChecker.checkForUpdates { isUpdateAvailable, newVersion ->
                if (isUpdateAvailable) {
                    sender.sendMessage(com.github.postyizhan.util.MessageUtil.color(
                        com.github.postyizhan.util.MessageUtil.getMessage("system.updater.update_available")
                            .replace("{current_version}", description.version)
                            .replace("{latest_version}", newVersion)
                    ))
                    sender.sendMessage(com.github.postyizhan.util.MessageUtil.color(
                        com.github.postyizhan.util.MessageUtil.getMessage("system.updater.update_url")
                            .replace("{current_version}", description.version)
                            .replace("{latest_version}", newVersion)
                    ))
                } else {
                    sender.sendMessage(com.github.postyizhan.util.MessageUtil.color(
                        com.github.postyizhan.util.MessageUtil.getMessage("system.updater.up_to_date")
                    ))
                }
            }
        }
    }

    // 委托给依赖容器的访问器方法
    fun getConfigManager() = container.configManager
    fun getDatabaseManager() = container.databaseManager
    fun getMenuManager() = container.menuManager
    fun getDynamicCommandRegistrar() = container.dynamicCommandRegistrar
    fun getUpdateChecker() = container.updateChecker
    fun getVaultManager() = container.vaultManager
    fun getPlayerPointsManager() = container.playerPointsManager
    fun getPlaceholderAPIManager() = container.placeholderAPIManager
    fun getGroupConfig() = container.groupConfig
    fun getEconomyService() = container.economyService
    fun getTeleportManager() = container.teleportManager

    /**
     * 获取动作工厂
     * @return 动作工厂实例
     */
    fun getActionFactory() = container.actionFactory

    /**
     * 检查调试模式是否启用
     * @return 如果调试模式启用则返回true，否则返回false
     */
    fun isDebugEnabled(): Boolean =
        if (this::container.isInitialized)
            container.configManager.getConfig().getBoolean("debug", false)
        else false
}
