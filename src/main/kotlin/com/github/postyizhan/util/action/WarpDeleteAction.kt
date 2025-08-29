package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.WarpOperationUtils
import org.bukkit.entity.Player

/**
 * 删除地标动作处理器
 */
class WarpDeleteAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val name = extractActionValue(actionValue, ActionType.WARP_DELETE.prefix)
        logDebug("Player ${player.name} deleting warp: $name")
        
        // 使用工具类执行删除操作
        val result = WarpOperationUtils.executeWarpOperation(
            plugin, player, "warp_delete", name
        ) { warp ->
            val success = plugin.getEnhancedDatabaseManager().deleteWarpSync(warp.id)
            if (success) {
                // 退还费用
                plugin.getEconomyService().refundDeleteCost(player)
                // 关闭菜单
                player.closeInventory()
            }
            success
        }
        
        // 发送结果消息
        sendMessage(player, result.messageKey, *result.replacements.toList().toTypedArray())
    }
}
