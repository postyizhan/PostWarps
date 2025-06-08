package com.github.postyizhan.listeners

import com.github.postyizhan.PostWarps
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val plugin: PostWarps) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        // 如果玩家移动了位置（而不是仅仅转动视角）
        if (event.from.x != event.to?.x || event.from.y != event.to?.y || event.from.z != event.to?.z) {
            val player = event.player
            
            // 检查玩家是否正在等待传送
            if (plugin.warpManager.isPendingTeleport(player)) {
                // 取消传送
                plugin.warpManager.cancelPendingTeleport(player)
                
                // 发送取消消息
                player.sendMessage(plugin.i18n.getMessage("prefix") + plugin.i18n.getMessage("warp.teleport.warmup_cancelled"))
                
                if (plugin.debugMode) {
                    plugin.logger.info("Cancelled teleport for ${player.name} due to movement")
                }
            }
        }
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // 玩家退出时，取消待传送任务
        val player = event.player
        
        // 检查玩家是否正在等待传送
        if (plugin.warpManager.isPendingTeleport(player)) {
            // 取消传送
            plugin.warpManager.cancelPendingTeleport(player)
            
            if (plugin.debugMode) {
                plugin.logger.info("Cancelled teleport for ${player.name} due to logout")
            }
        }
    }
}
