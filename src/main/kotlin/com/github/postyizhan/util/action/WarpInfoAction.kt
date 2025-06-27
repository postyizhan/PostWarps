package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player
import java.text.SimpleDateFormat

/**
 * 显示地标信息动作处理器
 */
class WarpInfoAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val name = extractActionValue(actionValue, ActionType.WARP_INFO.prefix)
        logDebug("Player ${player.name} viewing info for warp: $name")
        
        // 从数据库中获取地标
        val warp = if (name.isEmpty()) {
            val data = plugin.getMenuManager().getPlayerData(player)
            val warpId = data["warp_id"] as? Int ?: run {
                logDebug("No warp_id found in player data")
                return
            }
            plugin.getDatabaseManager().getWarp(warpId)
        } else {
            plugin.getDatabaseManager().getWarp(name, player.uniqueId) 
                ?: plugin.getDatabaseManager().getPublicWarp(name)
        }
        
        if (warp == null) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("info.not-found")
                    .replace("{name}", if (name.isEmpty()) "selected warp" else name)
            ))
            return
        }
        
        logDebug("Displaying info for warp ID: ${warp.id}, name: ${warp.name}")
        
        // 显示信息
        player.sendMessage(MessageUtil.color(MessageUtil.getMessage("info.header", player)))
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("info.name", player).replace("{name}", warp.name)
        ))
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("info.owner", player).replace("{owner}", warp.ownerName)
        ))
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("info.world", player).replace("{world}", warp.worldName)
        ))
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("info.coordinates", player).replace("{coords}", warp.getFormattedCoordinates())
        ))
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("info.created", player).replace("{time}", formatTimestamp(warp.createTime))
        ))

        // 使用国际化的公开/私有状态
        val publicStatus = if (warp.isPublic) {
            MessageUtil.getMessage("status.public", player)
        } else {
            MessageUtil.getMessage("status.private", player)
        }
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("info.public", player).replace("{public}", publicStatus)
        ))

        // 使用国际化的描述默认值
        val descriptionText = if (warp.description.isEmpty()) {
            when (MessageUtil.getPlayerLanguage(player)) {
                "en_US" -> "None"
                else -> "无"
            }
        } else {
            warp.description
        }
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("info.description", player).replace("{desc}", descriptionText)
        ))
        
        // 关闭菜单
        player.closeInventory()
    }
    
    /**
     * 格式化时间戳
     */
    private fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(date)
    }
}
