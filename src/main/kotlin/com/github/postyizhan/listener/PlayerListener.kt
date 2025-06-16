package com.github.postyizhan.listener

import com.github.postyizhan.PostWarps
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 玩家监听器，处理玩家相关事件
 */
class PlayerListener(private val plugin: PostWarps) : Listener {
    
    /**
     * 处理玩家加入事件
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // 如果玩家是OP，发送更新检查信息
        if (player.isOp && plugin.getConfigManager().getConfig().getBoolean("update-checker.enabled", true)) {
            // 延迟发送，避免消息过多
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (player.isOnline) {
                    plugin.sendUpdateInfo(player)
                }
            }, 40L)  // 2秒后发送
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler
    fun onPlayerQuit(@Suppress("UNUSED_PARAMETER") event: PlayerQuitEvent) {
        // 清理玩家数据
    }
}
