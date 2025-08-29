package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.WarpOperationUtils
import org.bukkit.entity.Player

/**
 * 设置地标为私有动作处理器
 */
class WarpSetPrivateAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val name = extractActionValue(actionValue, ActionType.WARP_SET_PRIVATE.prefix)
        logDebug("Player ${player.name} setting warp private: $name")
        
        // 使用工具类执行操作
        val result = WarpOperationUtils.setWarpPublicState(plugin, player, name, false)
        
        // 发送结果消息
        sendMessage(player, result.messageKey, *result.replacements.toList().toTypedArray())
        
        // 如果成功，刷新菜单
        if (result.success) {
            WarpOperationUtils.refreshMenu(plugin, player, result.warp, false)
        }
    }
}
