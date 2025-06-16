package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 设置地标为公开动作处理器
 */
class WarpSetPublicAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val name = extractActionValue(actionValue, ActionType.WARP_SET_PUBLIC.prefix)
        logDebug("Player ${player.name} setting warp public: $name")
        
        // 设置地标为公开
        setWarpPublic(player, name, true)
    }
    
    /**
     * 设置地标公开状态
     */
    private fun setWarpPublic(player: Player, name: String, isPublic: Boolean) {
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
        }
        
        if (warp == null) {
            val messageKey = if (isPublic) "public.not-found" else "private.not-found"
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage(messageKey)
                    .replace("{name}", if (name.isEmpty()) "selected warp" else name)
            ))
            return
        }
        
        // 检查是否是自己的地标
        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("public.not-owner")
            ))
            return
        }
        
        // 检查当前状态
        if (warp.isPublic == isPublic) {
            val messageKey = if (isPublic) "public.already-public" else "private.already-private"
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage(messageKey)
                    .replace("{name}", warp.name)
            ))
            return
        }
        
        logDebug("Setting warp ${warp.name} (ID: ${warp.id}) to ${if (isPublic) "public" else "private"}")
        
        // 更新状态
        val success = plugin.getDatabaseManager().setWarpPublic(warp.id, isPublic)
        if (success) {
            val messageKey = if (isPublic) "public.success" else "private.success"
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage(messageKey)
                    .replace("{name}", warp.name)
            ))
            
            // 刷新数据
            val data = plugin.getMenuManager().getPlayerData(player)
            data["public"] = isPublic
            
            // 重新打开当前菜单
            val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
            plugin.getMenuManager().openMenu(player, currentMenu)
        } else {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("update.failed")
            ))
        }
    }
}
