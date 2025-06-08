package com.github.postyizhan.listeners

import com.github.postyizhan.PostWarps
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val plugin: PostWarps) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        // 如果玩家正在传送中，取消传送
        if (plugin.warpManager.isPendingTeleport(event.player)) {
            // 检查玩家是否真的移动了（而不是只是转头）
            if (event.from.blockX != event.to?.blockX ||
                event.from.blockY != event.to?.blockY ||
                event.from.blockZ != event.to?.blockZ) {
                plugin.warpManager.cancelPendingTeleport(event.player)
                event.player.sendMessage(plugin.i18n.getMessage("warp.teleport.cancelled"))
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

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // 如果玩家是OP，发送更新检查信息
        if (player.isOp && plugin.config.getBoolean("update-checker.enabled", true)) {
            // 在玩家加入后2秒发送更新信息，避免消息过多
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (player.isOnline) {
                    plugin.sendUpdateInfo(player)
                }
            }, 40L)  // 40 ticks = 2 seconds
        }
    }
}
