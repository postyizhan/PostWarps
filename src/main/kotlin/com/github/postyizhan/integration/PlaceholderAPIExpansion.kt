package com.github.postyizhan.integration

import com.github.postyizhan.PostWarps
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

/**
 * PlaceholderAPI扩展
 * 提供PostWarps相关的占位符支持
 */
class PlaceholderAPIExpansion(private val plugin: PostWarps) : PlaceholderExpansion() {
    
    /**
     * 获取扩展标识符
     */
    override fun getIdentifier(): String = "postwarps"
    
    /**
     * 获取扩展作者
     */
    override fun getAuthor(): String = "postyizhan"
    
    /**
     * 获取扩展版本
     */
    override fun getVersion(): String = plugin.description.version
    
    /**
     * 是否持久化
     */
    override fun persist(): Boolean = true
    
    /**
     * 是否可以注册
     */
    override fun canRegister(): Boolean = true
    
    /**
     * 处理占位符请求
     * 
     * 支持的占位符：
     * %postwarps_total_warps% - 玩家总地标数量
     * %postwarps_public_warps% - 玩家公开地标数量
     * %postwarps_private_warps% - 玩家私有地标数量
     * %postwarps_vault_balance% - 玩家Vault余额
     * %postwarps_points_balance% - 玩家点券余额
     * %postwarps_group% - 玩家当前权限组
     * %postwarps_create_cost_vault% - 创建地标Vault费用
     * %postwarps_create_cost_points% - 创建地标点券费用
     * %postwarps_teleport_cost_vault% - 传送Vault费用
     * %postwarps_teleport_cost_points% - 传送点券费用
     * %postwarps_has_warp_<name>% - 是否拥有指定名称的地标
     * %postwarps_warp_public_<name>% - 指定地标是否公开
     */
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        
        return when {
            // 地标数量相关
            params == "total_warps" -> {
                val warps = plugin.getDatabaseManager().getPlayerWarps(player.uniqueId)
                warps.size.toString()
            }
            
            params == "public_warps" -> {
                val warps = plugin.getDatabaseManager().getPlayerPublicWarps(player.uniqueId)
                warps.size.toString()
            }
            
            params == "private_warps" -> {
                val warps = plugin.getDatabaseManager().getPlayerPrivateWarps(player.uniqueId)
                warps.size.toString()
            }
            
            // 经济相关
            params == "vault_balance" -> {
                if (plugin.getVaultManager().hasEconomy()) {
                    plugin.getVaultManager().format(plugin.getVaultManager().getBalance(player))
                } else {
                    "N/A"
                }
            }
            
            params == "points_balance" -> {
                if (plugin.getPlayerPointsManager().isAvailable()) {
                    plugin.getPlayerPointsManager().format(plugin.getPlayerPointsManager().getBalance(player))
                } else {
                    "N/A"
                }
            }
            
            // 权限组相关
            params == "group" -> {
                val groupConfig = plugin.getGroupConfig().getPlayerGroupConfig(player)
                groupConfig.groupName
            }
            
            // 费用相关
            params == "create_cost_vault" -> {
                val groupConfig = plugin.getGroupConfig().getPlayerGroupConfig(player)
                if (groupConfig.vaultConfig.enabled) {
                    plugin.getVaultManager().format(groupConfig.vaultConfig.createCost)
                } else {
                    "N/A"
                }
            }
            
            params == "create_cost_points" -> {
                val groupConfig = plugin.getGroupConfig().getPlayerGroupConfig(player)
                if (groupConfig.playerPointsConfig.enabled) {
                    groupConfig.playerPointsConfig.createCost.toInt().toString()
                } else {
                    "N/A"
                }
            }
            
            params == "teleport_cost_vault" -> {
                val groupConfig = plugin.getGroupConfig().getPlayerGroupConfig(player)
                if (groupConfig.vaultConfig.enabled) {
                    plugin.getVaultManager().format(groupConfig.vaultConfig.teleportCost)
                } else {
                    "N/A"
                }
            }
            
            params == "teleport_cost_points" -> {
                val groupConfig = plugin.getGroupConfig().getPlayerGroupConfig(player)
                if (groupConfig.playerPointsConfig.enabled) {
                    groupConfig.playerPointsConfig.teleportCost.toInt().toString()
                } else {
                    "N/A"
                }
            }
            
            // 地标检查相关
            params.startsWith("has_warp_") -> {
                val warpName = params.substring("has_warp_".length)
                val warp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)
                (warp != null).toString()
            }
            
            params.startsWith("warp_public_") -> {
                val warpName = params.substring("warp_public_".length)
                val warp = plugin.getDatabaseManager().getWarp(warpName, player.uniqueId)
                if (warp != null) {
                    warp.isPublic.toString()
                } else {
                    "false"
                }
            }
            
            // 服务器统计相关
            params == "server_total_warps" -> {
                try {
                    val totalWarps = plugin.getDatabaseManager().getAllWarps().size
                    totalWarps.toString()
                } catch (e: Exception) {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("DEBUG: Failed to get server total warps: ${e.message}")
                    }
                    "0"
                }
            }
            
            params == "server_public_warps" -> {
                try {
                    val publicWarps = plugin.getDatabaseManager().getAllPublicWarps().size
                    publicWarps.toString()
                } catch (e: Exception) {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("DEBUG: Failed to get server public warps: ${e.message}")
                    }
                    "0"
                }
            }
            
            // 经济系统状态
            params == "vault_enabled" -> {
                plugin.getVaultManager().hasEconomy().toString()
            }
            
            params == "points_enabled" -> {
                plugin.getPlayerPointsManager().isAvailable().toString()
            }
            
            // 玩家经济系统状态
            params == "player_vault_enabled" -> {
                val groupConfig = plugin.getGroupConfig().getPlayerGroupConfig(player)
                groupConfig.vaultConfig.enabled.toString()
            }
            
            params == "player_points_enabled" -> {
                val groupConfig = plugin.getGroupConfig().getPlayerGroupConfig(player)
                groupConfig.playerPointsConfig.enabled.toString()
            }
            
            // 最近创建的地标
            params == "latest_warp" -> {
                val warps = plugin.getDatabaseManager().getPlayerWarps(player.uniqueId)
                if (warps.isNotEmpty()) {
                    val latestWarp = warps.maxByOrNull { it.createTime }
                    latestWarp?.name ?: "None"
                } else {
                    "None"
                }
            }
            
            // 最近创建的公开地标
            params == "latest_public_warp" -> {
                val warps = plugin.getDatabaseManager().getPlayerPublicWarps(player.uniqueId)
                if (warps.isNotEmpty()) {
                    val latestWarp = warps.maxByOrNull { it.createTime }
                    latestWarp?.name ?: "None"
                } else {
                    "None"
                }
            }
            
            else -> null
        }
    }
}
