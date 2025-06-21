package com.github.postyizhan.service

import com.github.postyizhan.PostWarps
import com.github.postyizhan.config.GroupConfig
import com.github.postyizhan.integration.VaultManager
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 经济服务类
 * 处理所有与经济相关的操作
 * 支持按权限组配置不同的经济系统
 */
class EconomyService(
    private val plugin: PostWarps,
    private val vaultManager: VaultManager,
    private val groupConfig: GroupConfig
) {
    
    /**
     * 检查经济功能是否可用
     */
    fun isAvailable(player: Player): Boolean {
        val playerConfig = groupConfig.getPlayerGroupConfig(player)
        return playerConfig.hasAnyEconomyEnabled() && vaultManager.hasEconomy()
    }

    /**
     * 获取玩家的权限组配置
     */
    private fun getPlayerConfig(player: Player) = groupConfig.getPlayerGroupConfig(player)
    
    /**
     * 检查并扣除创建地标的费用
     */
    fun chargeCreateCost(player: Player): Boolean {
        val playerConfig = getPlayerConfig(player)

        // 检查Vault经济系统
        if (playerConfig.vaultConfig.enabled && vaultManager.hasEconomy()) {
            val cost = playerConfig.vaultConfig.createCost
            if (cost > 0.0) {
                if (!chargeVaultCost(player, cost, "economy.charged_create")) {
                    return false
                }
            }
        }

        // TODO: 检查PlayerPoints系统
        // if (playerConfig.playerPointsConfig.enabled && playerPointsManager.isAvailable()) {
        //     val cost = playerConfig.playerPointsConfig.createCost.toInt()
        //     if (cost > 0) {
        //         if (!chargePlayerPointsCost(player, cost, "economy.charged_create")) {
        //             return false
        //         }
        //     }
        // }

        return true
    }

    /**
     * 扣除Vault费用的通用方法
     */
    private fun chargeVaultCost(player: Player, cost: Double, successMessageKey: String): Boolean {
        if (cost <= 0.0) return true

        if (!vaultManager.hasEnough(player, cost)) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("economy.insufficient_funds")
                    .replace("{cost}", vaultManager.format(cost))
                    .replace("{balance}", vaultManager.format(vaultManager.getBalance(player)))
            ))
            return false
        }

        if (vaultManager.withdraw(player, cost)) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage(successMessageKey)
                    .replace("{cost}", vaultManager.format(cost))
            ))
            return true
        } else {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("economy.transaction_failed")
            ))
            return false
        }
    }
    
    /**
     * 检查并扣除传送费用
     */
    fun chargeTeleportCost(player: Player, isPublic: Boolean): Boolean {
        val playerConfig = getPlayerConfig(player)

        // 检查Vault经济系统
        if (playerConfig.vaultConfig.enabled && vaultManager.hasEconomy()) {
            val cost = playerConfig.vaultConfig.getActualTeleportCost(isPublic)
            if (cost > 0.0) {
                if (!chargeVaultCost(player, cost, "economy.charged_teleport")) {
                    return false
                }
            }
        }

        // TODO: 检查PlayerPoints系统
        // if (playerConfig.playerPointsConfig.enabled && playerPointsManager.isAvailable()) {
        //     val cost = playerConfig.playerPointsConfig.getActualTeleportCost(isPublic).toInt()
        //     if (cost > 0) {
        //         if (!chargePlayerPointsCost(player, cost, "economy.charged_teleport")) {
        //             return false
        //         }
        //     }
        // }

        return true
    }
    
    /**
     * 检查并扣除设置公开的费用
     */
    fun chargeSetPublicCost(player: Player): Boolean {
        val playerConfig = getPlayerConfig(player)

        // 检查Vault经济系统
        if (playerConfig.vaultConfig.enabled && vaultManager.hasEconomy()) {
            val cost = playerConfig.vaultConfig.setPublicCost
            if (cost > 0.0) {
                if (!chargeVaultCost(player, cost, "economy.charged_set_public")) {
                    return false
                }
            }
        }

        // TODO: 检查PlayerPoints系统
        // if (playerConfig.playerPointsConfig.enabled && playerPointsManager.isAvailable()) {
        //     val cost = playerConfig.playerPointsConfig.setPublicCost.toInt()
        //     if (cost > 0) {
        //         if (!chargePlayerPointsCost(player, cost, "economy.charged_set_public")) {
        //             return false
        //         }
        //     }
        // }

        return true
    }
    
    /**
     * 退还删除地标的费用
     */
    fun refundDeleteCost(player: Player) {
        val playerConfig = getPlayerConfig(player)

        // 退还Vault费用
        if (playerConfig.vaultConfig.enabled && vaultManager.hasEconomy()) {
            val originalCost = playerConfig.vaultConfig.createCost
            val refundAmount = playerConfig.vaultConfig.calculateRefund(originalCost)

            if (refundAmount > 0.0) {
                if (vaultManager.deposit(player, refundAmount)) {
                    player.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("economy.refunded_delete")
                            .replace("{amount}", vaultManager.format(refundAmount))
                    ))
                }
            }
        }

        // TODO: 退还PlayerPoints费用
        // if (playerConfig.playerPointsConfig.enabled && playerPointsManager.isAvailable()) {
        //     val originalCost = playerConfig.playerPointsConfig.createCost.toInt()
        //     val refundAmount = playerConfig.playerPointsConfig.calculateRefund(originalCost.toDouble()).toInt()
        //
        //     if (refundAmount > 0) {
        //         if (playerPointsManager.give(player, refundAmount)) {
        //             player.sendMessage(MessageUtil.color(
        //                 MessageUtil.getMessage("economy.refunded_delete_points")
        //                     .replace("{amount}", refundAmount.toString())
        //             ))
        //         }
        //     }
        // }
    }
    
    /**
     * 获取玩家余额信息
     */
    fun getBalanceInfo(player: Player): String {
        val playerConfig = getPlayerConfig(player)

        if (!playerConfig.hasAnyEconomyEnabled()) {
            return MessageUtil.getMessage("economy.not_available")
        }

        val info = mutableListOf<String>()

        // Vault余额信息
        if (playerConfig.vaultConfig.enabled && vaultManager.hasEconomy()) {
            val balance = vaultManager.getBalance(player)
            info.add(MessageUtil.getMessage("economy.balance_info")
                .replace("{balance}", vaultManager.format(balance))
                .replace("{currency}", if (balance == 1.0) {
                    vaultManager.getCurrencyNameSingular()
                } else {
                    vaultManager.getCurrencyNamePlural()
                }))
        }

        // TODO: PlayerPoints余额信息
        // if (playerConfig.playerPointsConfig.enabled && playerPointsManager.isAvailable()) {
        //     val points = playerPointsManager.look(player)
        //     info.add(MessageUtil.getMessage("economy.points_balance_info")
        //         .replace("{points}", points.toString()))
        // }

        return info.joinToString("\n")
    }

    /**
     * 获取费用信息
     */
    fun getCostInfo(player: Player): List<String> {
        val playerConfig = getPlayerConfig(player)

        if (!playerConfig.hasAnyEconomyEnabled()) {
            return listOf(MessageUtil.getMessage("economy.not_available"))
        }

        val info = mutableListOf<String>()

        // 显示权限组信息
        info.add(MessageUtil.getMessage("economy.group_info")
            .replace("{group}", playerConfig.groupName))

        // Vault费用信息
        if (playerConfig.vaultConfig.enabled && vaultManager.hasEconomy()) {
            info.add("&a=== Vault经济系统 ===")
            info.add(MessageUtil.getMessage("economy.cost_create")
                .replace("{cost}", vaultManager.format(playerConfig.vaultConfig.createCost)))
            info.add(MessageUtil.getMessage("economy.cost_teleport_public")
                .replace("{cost}", vaultManager.format(playerConfig.vaultConfig.getActualTeleportCost(true))))
            info.add(MessageUtil.getMessage("economy.cost_teleport_private")
                .replace("{cost}", vaultManager.format(playerConfig.vaultConfig.getActualTeleportCost(false))))
            info.add(MessageUtil.getMessage("economy.cost_set_public")
                .replace("{cost}", vaultManager.format(playerConfig.vaultConfig.setPublicCost)))
        }

        // TODO: PlayerPoints费用信息
        // if (playerConfig.playerPointsConfig.enabled && playerPointsManager.isAvailable()) {
        //     info.add("&e=== PlayerPoints点券系统 ===")
        //     info.add(MessageUtil.getMessage("economy.cost_create_points")
        //         .replace("{cost}", playerConfig.playerPointsConfig.createCost.toInt().toString()))
        //     info.add(MessageUtil.getMessage("economy.cost_teleport_public_points")
        //         .replace("{cost}", playerConfig.playerPointsConfig.getActualTeleportCost(true).toInt().toString()))
        //     info.add(MessageUtil.getMessage("economy.cost_teleport_private_points")
        //         .replace("{cost}", playerConfig.playerPointsConfig.getActualTeleportCost(false).toInt().toString()))
        //     info.add(MessageUtil.getMessage("economy.cost_set_public_points")
        //         .replace("{cost}", playerConfig.playerPointsConfig.setPublicCost.toInt().toString()))
        // }

        return info
    }
}
