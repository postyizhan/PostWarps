package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.WarpOperationUtils
import org.bukkit.entity.Player

/**
 * 地标可见性切换动作处理器 - 统一处理公开和私有设置
 * 合并了原来的WarpSetPublicAction和WarpSetPrivateAction
 */
class WarpVisibilityAction(plugin: PostWarps) : AbstractAction(plugin) {
    
    override fun execute(player: Player, actionValue: String) {
        val (isPublic, name) = when {
            actionValue.startsWith(ActionType.WARP_SET_PUBLIC.prefix) -> {
                val extractedName = extractActionValue(actionValue, ActionType.WARP_SET_PUBLIC.prefix)
                true to extractedName
            }
            actionValue.startsWith(ActionType.WARP_SET_PRIVATE.prefix) -> {
                val extractedName = extractActionValue(actionValue, ActionType.WARP_SET_PRIVATE.prefix)
                false to extractedName
            }
            else -> {
                logDebug("Unknown visibility action: $actionValue")
                return
            }
        }
        
        logDebug("Player ${player.name} setting warp ${if (isPublic) "public" else "private"}: $name")
        
        // 使用工具类执行操作
        val result = WarpOperationUtils.setWarpPublicState(plugin, player, name, isPublic)
        
        // 发送结果消息
        sendMessage(player, result.messageKey, *result.replacements.toList().toTypedArray())
        
        // 如果成功，刷新菜单
        if (result.success) {
            WarpOperationUtils.refreshMenu(plugin, player, result.warp, isPublic)
        }
    }
}