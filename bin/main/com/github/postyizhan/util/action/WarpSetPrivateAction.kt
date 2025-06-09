package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 设置地标为私有动作处理器
 */
class WarpSetPrivateAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val name = extractActionValue(actionValue, ActionType.WARP_SET_PRIVATE.prefix)
        logDebug("Player ${player.name} setting warp private: $name")
        
        // 设置地标为私有
        setWarpPrivate(player, name)
    }
    
    /**
     * 设置地标为私有
     */
    private fun setWarpPrivate(player: Player, name: String) {
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
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("private.not-found")
                    .replace("{name}", if (name.isEmpty()) "selected warp" else name)
            ))
            return
        }
        
        // 检查是否是自己的地标
        if (warp.owner != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("private.not-owner")
            ))
            return
        }
        
        // 检查当前状态
        if (!warp.isPublic) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("private.already-private")
                    .replace("{name}", warp.name)
            ))
            return
        }
        
        logDebug("Setting warp ${warp.name} (ID: ${warp.id}) to private")
        
        // 更新状态
        val success = plugin.getDatabaseManager().setWarpPublic(warp.id, false)
        if (success) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("private.success")
                    .replace("{name}", warp.name)
            ))
            
            // 刷新数据
            val data = plugin.getMenuManager().getPlayerData(player)
            data["public"] = false
            
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
