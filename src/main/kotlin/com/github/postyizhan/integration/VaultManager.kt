package com.github.postyizhan.integration

import com.github.postyizhan.PostWarps
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.permission.Permission
import org.bukkit.entity.Player
import org.bukkit.plugin.RegisteredServiceProvider

/**
 * Vault集成管理器
 * 提供经济系统和权限系统的集成支持
 */
class VaultManager(private val plugin: PostWarps) {
    
    private var economy: Economy? = null
    private var permission: Permission? = null
    private var isVaultEnabled = false
    
    /**
     * 初始化Vault集成
     */
    fun initialize(): Boolean {
        if (!plugin.server.pluginManager.isPluginEnabled("Vault")) {
            plugin.logger.info("Vault插件未找到，经济功能将被禁用")
            return false
        }
        
        // 设置经济系统
        val economyProvider: RegisteredServiceProvider<Economy>? = 
            plugin.server.servicesManager.getRegistration(Economy::class.java)
        
        if (economyProvider != null) {
            economy = economyProvider.provider
            plugin.server.consoleSender.sendMessage(
                com.github.postyizhan.util.MessageUtil.color(
                    com.github.postyizhan.util.MessageUtil.getMessage("messages.plugin-hooked")
                        .replace("{0}", economy?.name ?: "Unknown")
                )
            )
        } else {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Economy provider not found")
            }
        }
        
        // 设置权限系统
        val permissionProvider: RegisteredServiceProvider<Permission>? = 
            plugin.server.servicesManager.getRegistration(Permission::class.java)
        
        if (permissionProvider != null) {
            permission = permissionProvider.provider
            plugin.server.consoleSender.sendMessage(
                com.github.postyizhan.util.MessageUtil.color(
                    com.github.postyizhan.util.MessageUtil.getMessage("messages.plugin-hooked")
                        .replace("{0}", permission?.name ?: "Unknown")
                )
            )
        } else {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Permission provider not found")
            }
        }
        
        isVaultEnabled = economy != null || permission != null
        
        if (isVaultEnabled) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("Vault integration enabled")
            }
        } else {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Vault integration failed, no supported service providers found")
            }
        }
        
        return isVaultEnabled
    }
    
    /**
     * 检查Vault是否可用
     */
    fun isEnabled(): Boolean = isVaultEnabled
    
    /**
     * 检查经济系统是否可用
     */
    fun hasEconomy(): Boolean = economy != null
    
    /**
     * 检查权限系统是否可用
     */
    fun hasPermission(): Boolean = permission != null
    
    /**
     * 获取玩家余额
     */
    fun getBalance(player: Player): Double {
        return economy?.getBalance(player) ?: 0.0
    }
    
    /**
     * 检查玩家是否有足够的金钱
     */
    fun hasEnough(player: Player, amount: Double): Boolean {
        return economy?.has(player, amount) ?: false
    }
    
    /**
     * 从玩家账户扣除金钱
     */
    fun withdraw(player: Player, amount: Double): Boolean {
        val economy = this.economy ?: return false
        val response = economy.withdrawPlayer(player, amount)
        return response.transactionSuccess()
    }
    
    /**
     * 向玩家账户存入金钱
     */
    fun deposit(player: Player, amount: Double): Boolean {
        val economy = this.economy ?: return false
        val response = economy.depositPlayer(player, amount)
        return response.transactionSuccess()
    }
    
    /**
     * 格式化金钱数量
     */
    fun format(amount: Double): String {
        return economy?.format(amount) ?: amount.toString()
    }
    
    /**
     * 获取货币名称（单数）
     */
    fun getCurrencyNameSingular(): String {
        return economy?.currencyNameSingular() ?: "金币"
    }
    
    /**
     * 获取货币名称（复数）
     */
    fun getCurrencyNamePlural(): String {
        return economy?.currencyNamePlural() ?: "金币"
    }
    
    /**
     * 检查玩家是否在指定组中
     */
    fun playerInGroup(player: Player, group: String): Boolean {
        return permission?.playerInGroup(player, group) ?: false
    }
    
    /**
     * 获取玩家的主要组
     */
    fun getPrimaryGroup(player: Player): String? {
        return try {
            permission?.getPrimaryGroup(player)
        } catch (e: UnsupportedOperationException) {
            // SuperPerms doesn't support group permissions, return null
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Permission system doesn't support group permissions, using default group configuration")
            }
            null
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Error getting player primary group: ${e.message}")
            }
            null
        }
    }
    
    /**
     * 获取玩家的所有组
     */
    fun getPlayerGroups(player: Player): Array<String> {
        return try {
            permission?.getPlayerGroups(player) ?: emptyArray()
        } catch (e: UnsupportedOperationException) {
            // SuperPerms doesn't support group permissions, return empty array
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Permission system doesn't support group permissions query, using default group configuration")
            }
            emptyArray()
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Error getting player groups: ${e.message}")
            }
            emptyArray()
        }
    }
    
    /**
     * 检查玩家是否有指定权限
     */
    fun hasPermission(player: Player, permission: String): Boolean {
        return this.permission?.has(player, permission) ?: player.hasPermission(permission)
    }
    
    /**
     * 关闭Vault集成
     */
    fun shutdown() {
        economy = null
        permission = null
        isVaultEnabled = false
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("Vault integration shutdown")
        }
    }
}
