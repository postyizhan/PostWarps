package com.github.postyizhan.config

import com.github.postyizhan.PostWarps
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

/**
 * 权限组配置管理器
 * 支持按权限组配置不同的经济系统和费用
 */
class GroupConfig(private val plugin: PostWarps) {
    
    private lateinit var config: FileConfiguration
    private lateinit var configFile: File
    
    /**
     * 初始化配置
     */
    fun initialize() {
        configFile = File(plugin.dataFolder, "groups.yml")
        
        // 如果配置文件不存在，从资源中复制
        if (!configFile.exists()) {
            plugin.saveResource("groups.yml", false)
        }
        
        config = YamlConfiguration.loadConfiguration(configFile)
        plugin.logger.info("Group configuration loaded")
    }
    
    /**
     * 重新加载配置
     */
    fun reload() {
        config = YamlConfiguration.loadConfiguration(configFile)
        plugin.logger.info("Group configuration reloaded")
    }
    
    /**
     * 获取玩家的权限组配置
     * 返回优先级最高的组配置
     */
    fun getPlayerGroupConfig(player: Player): GroupEconomyConfig {
        val vaultManager = plugin.getVaultManager()
        
        if (!vaultManager.hasPermission()) {
            // 如果没有权限系统，使用默认配置
            return getGroupConfig("default")
        }
        
        // 获取玩家的所有组
        val playerGroups = vaultManager.getPlayerGroups(player)
        
        // 找到优先级最高的组
        var highestPriority = -1
        var selectedGroup = "default"
        
        // 检查默认组
        val defaultPriority = config.getInt("default.priority", 0)
        if (defaultPriority > highestPriority) {
            highestPriority = defaultPriority
            selectedGroup = "default"
        }
        
        // 检查玩家的所有组
        for (group in playerGroups) {
            if (config.contains(group)) {
                val priority = config.getInt("$group.priority", 0)
                if (priority > highestPriority) {
                    highestPriority = priority
                    selectedGroup = group
                }
            }
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("DEBUG: Player ${player.name} using group config: $selectedGroup (priority: $highestPriority)")
        }
        return getGroupConfig(selectedGroup)
    }
    
    /**
     * 获取指定组的配置
     */
    private fun getGroupConfig(groupName: String): GroupEconomyConfig {
        val groupSection = config.getConfigurationSection(groupName)
            ?: config.getConfigurationSection("default")!!
        
        return GroupEconomyConfig(
            groupName = groupName,
            priority = groupSection.getInt("priority", 0),
            vaultConfig = EconomySystemConfig(
                enabled = groupSection.getBoolean("vault.enabled", false),
                createCost = groupSection.getDouble("vault.costs.create", 0.0),
                teleportCost = groupSection.getDouble("vault.costs.teleport", 0.0),
                publicTeleportCost = groupSection.getDouble("vault.costs.public_teleport", 0.0),
                privateTeleportCost = groupSection.getDouble("vault.costs.private_teleport", 0.0),
                setPublicCost = groupSection.getDouble("vault.costs.set_public", 0.0),
                refundOnDelete = groupSection.getBoolean("vault.refund_on_delete", false),
                refundRatio = groupSection.getDouble("vault.refund_ratio", 0.0)
            ),
            playerPointsConfig = EconomySystemConfig(
                enabled = groupSection.getBoolean("playerpoints.enabled", false),
                createCost = groupSection.getDouble("playerpoints.costs.create", 0.0),
                teleportCost = groupSection.getDouble("playerpoints.costs.teleport", 0.0),
                publicTeleportCost = groupSection.getDouble("playerpoints.costs.public_teleport", 0.0),
                privateTeleportCost = groupSection.getDouble("playerpoints.costs.private_teleport", 0.0),
                setPublicCost = groupSection.getDouble("playerpoints.costs.set_public", 0.0),
                refundOnDelete = groupSection.getBoolean("playerpoints.refund_on_delete", false),
                refundRatio = groupSection.getDouble("playerpoints.refund_ratio", 0.0)
            )
        )
    }
    
    /**
     * 获取所有可用的组名
     */
    fun getAvailableGroups(): Set<String> {
        return config.getKeys(false)
    }
    
    /**
     * 检查指定组是否存在
     */
    fun hasGroup(groupName: String): Boolean {
        return config.contains(groupName)
    }
}

/**
 * 权限组经济配置数据类
 */
data class GroupEconomyConfig(
    val groupName: String,
    val priority: Int,
    val vaultConfig: EconomySystemConfig,
    val playerPointsConfig: EconomySystemConfig
) {
    /**
     * 检查是否有任何经济系统启用
     */
    fun hasAnyEconomyEnabled(): Boolean {
        return vaultConfig.enabled || playerPointsConfig.enabled
    }
    
    /**
     * 获取启用的经济系统列表
     */
    fun getEnabledSystems(): List<String> {
        val systems = mutableListOf<String>()
        if (vaultConfig.enabled) systems.add("vault")
        if (playerPointsConfig.enabled) systems.add("playerpoints")
        return systems
    }
}

/**
 * 经济系统配置数据类
 */
data class EconomySystemConfig(
    val enabled: Boolean,
    val createCost: Double,
    val teleportCost: Double,
    val publicTeleportCost: Double,
    val privateTeleportCost: Double,
    val setPublicCost: Double,
    val refundOnDelete: Boolean,
    val refundRatio: Double
) {
    /**
     * 获取实际的传送费用（优先使用具体的公开/私有费用）
     */
    fun getActualTeleportCost(isPublic: Boolean): Double {
        return if (isPublic) {
            if (publicTeleportCost > 0) publicTeleportCost else teleportCost
        } else {
            if (privateTeleportCost > 0) privateTeleportCost else teleportCost
        }
    }
    
    /**
     * 计算退款金额
     */
    fun calculateRefund(originalCost: Double): Double {
        return if (refundOnDelete) {
            originalCost * refundRatio
        } else {
            0.0
        }
    }
}
