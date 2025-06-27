package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 传送到地标动作处理器
 */
class WarpTeleportAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val name = extractActionValue(actionValue, ActionType.WARP_TELEPORT.prefix)
        logDebug("Player ${player.name} teleporting to warp: $name")
        
        // 从数据库中获取地标
        val warp = if (name.isEmpty()) {
            val data = plugin.getMenuManager().getPlayerData(player)
            val warpId = data["warp_id"] as? Int ?: run {
                logDebug("No warp_id found in player data")
                player.sendMessage(MessageUtil.color(
                    MessageUtil.getMessage("teleport.no-warp-id")
                ))
                return
            }
            logDebug("Attempting to teleport to warp ID: $warpId")
            plugin.getDatabaseManager().getWarp(warpId) ?: run {
                logDebug("Warp with ID $warpId not found")
                player.sendMessage(MessageUtil.color(
                    MessageUtil.getMessage("teleport.not-found-id")
                        .replace("{id}", warpId.toString())
                ))
                return
            }
        } else {
            logDebug("Attempting to teleport to warp by name: $name")
            plugin.getDatabaseManager().getWarp(name, player.uniqueId) 
                ?: plugin.getDatabaseManager().getPublicWarp(name)
        }
        
        if (warp == null) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("teleport.not-found")
                    .replace("{name}", if (name.isEmpty()) "selected warp" else name)
            ))
            return
        }
        
        logDebug("Found warp: ${warp.name}, owner: ${warp.ownerName}, world: ${warp.worldName}, coords: ${warp.getFormattedCoordinates()}")

        // 检查权限
        if (warp.owner != player.uniqueId && !warp.isPublic && !player.hasPermission("postwarps.admin")) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("teleport.no-permission")
            ))
            return
        }

        // 检查传送费用
        if (!plugin.getEconomyService().chargeTeleportCost(player, warp.isPublic)) {
            return
        }

        // 获取位置
        val location = warp.getLocation()
        if (location == null) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("teleport.failed")
                    .replace("{name}", warp.name)
            ))
            logDebug("Failed to get location for warp ${warp.name}, world ${warp.worldName} may not exist")
            return
        }

        // 获取玩家的传送配置
        val groupConfig = plugin.getGroupConfig().getPlayerGroupConfig(player)
        val teleportConfig = groupConfig.teleportConfig

        // 关闭菜单
        player.closeInventory()

        // 使用传送管理器执行传送
        logDebug("Teleporting ${player.name} to warp ${warp.name} with delay ${teleportConfig.delay}s")
        plugin.getTeleportManager().teleportToWarp(
            player = player,
            warp = warp,
            teleportConfig = teleportConfig,
            onSuccess = {
                logDebug("Successfully teleported ${player.name} to warp ${warp.name}")
            },
            onCancel = {
                logDebug("Teleportation cancelled for ${player.name} to warp ${warp.name}")
                // 如果传送被取消，退还费用
                plugin.getEconomyService().refundTeleportCost(player, warp.isPublic)
            }
        )
    }
}
