package com.github.postyizhan.teleport

import com.github.postyizhan.PostWarps
import com.github.postyizhan.config.TeleportConfig
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap

/**
 * 传送管理器
 * 处理延迟传送、移动取消、伤害取消等功能
 */
class TeleportManager(private val plugin: PostWarps) : Listener {
    
    // 存储正在进行的传送请求
    private val pendingTeleports = ConcurrentHashMap<Player, TeleportRequest>()
    
    /**
     * 初始化传送管理器
     */
    fun initialize() {
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, plugin)
        plugin.logger.info("TeleportManager initialized")
    }
    
    /**
     * 关闭传送管理器
     */
    fun shutdown() {
        // 取消所有待处理的传送
        pendingTeleports.values.forEach { request ->
            request.task?.cancel()
        }
        pendingTeleports.clear()
        plugin.logger.info("TeleportManager shutdown")
    }
    
    /**
     * 执行传送到地标
     * @param player 玩家
     * @param warp 地标
     * @param teleportConfig 传送配置
     * @param onSuccess 成功回调
     * @param onCancel 取消回调
     */
    fun teleportToWarp(
        player: Player,
        warp: Warp,
        teleportConfig: TeleportConfig,
        onSuccess: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        // 如果玩家已有待处理的传送，先取消
        cancelTeleport(player)
        
        // 如果不需要延迟，直接传送
        if (!teleportConfig.needsDelay()) {
            val location = warp.getLocation()
            if (location != null) {
                performTeleport(player, location)
                onSuccess?.invoke()
            } else {
                player.sendMessage(MessageUtil.color(
                    MessageUtil.getMessage("teleport.failed", player)
                        .replace("{name}", warp.name)
                ))
                onCancel?.invoke()
            }
            return
        }
        
        // 记录玩家当前位置
        val startLocation = player.location.clone()
        
        // 发送开始传送消息
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("teleport.starting", player)
                .replace("{name}", warp.name)
                .replace("{delay}", teleportConfig.delay.toString())
        ))
        
        // 创建传送任务
        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            // 移除待处理的传送记录
            pendingTeleports.remove(player)

            // 执行传送
            if (player.isOnline) {
                val location = warp.getLocation()
                if (location != null) {
                    performTeleport(player, location)
                    player.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("teleport.success", player)
                            .replace("{name}", warp.name)
                    ))
                    onSuccess?.invoke()
                } else {
                    player.sendMessage(MessageUtil.color(
                        MessageUtil.getMessage("teleport.failed", player)
                            .replace("{name}", warp.name)
                    ))
                    onCancel?.invoke()
                }
            }
        }, teleportConfig.getDelayTicks())
        
        // 记录传送请求
        val request = TeleportRequest(
            player = player,
            warp = warp,
            startLocation = startLocation,
            teleportConfig = teleportConfig,
            task = task,
            onCancel = onCancel
        )
        pendingTeleports[player] = request
    }
    
    /**
     * 取消玩家的传送
     */
    fun cancelTeleport(player: Player): Boolean {
        val request = pendingTeleports.remove(player) ?: return false
        
        // 取消任务
        request.task?.cancel()
        
        // 发送取消消息
        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("teleport.cancelled", player)
        ))
        
        // 执行取消回调
        request.onCancel?.invoke()
        
        return true
    }
    
    /**
     * 检查玩家是否有待处理的传送
     */
    fun hasPendingTeleport(player: Player): Boolean {
        return pendingTeleports.containsKey(player)
    }
    
    /**
     * 获取玩家的待处理传送请求
     */
    fun getPendingTeleport(player: Player): TeleportRequest? {
        return pendingTeleports[player]
    }
    
    /**
     * 执行实际的传送
     */
    private fun performTeleport(player: Player, location: Location) {
        try {
            // 确保目标位置是安全的
            val safeLocation = findSafeLocation(location)
            if (safeLocation != null) {
                player.teleport(safeLocation)
            } else {
                player.sendMessage(MessageUtil.color(
                    MessageUtil.getMessage("teleport.unsafe_location", player)
                ))
            }
        } catch (e: Exception) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("teleport.failed", player)
            ))
            plugin.logger.warning("Failed to teleport player ${player.name}: ${e.message}")
        }
    }
    
    /**
     * 寻找安全的传送位置
     */
    private fun findSafeLocation(location: Location): Location? {
        val world = location.world ?: return null
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ
        
        // 检查原位置是否安全
        if (isSafeLocation(world.getBlockAt(x, y, z).location)) {
            return location
        }
        
        // 向上寻找安全位置
        for (i in 1..10) {
            val checkLocation = world.getBlockAt(x, y + i, z).location
            if (isSafeLocation(checkLocation)) {
                return checkLocation.add(0.5, 0.0, 0.5)
            }
        }
        
        // 向下寻找安全位置
        for (i in 1..5) {
            if (y - i < 0) break
            val checkLocation = world.getBlockAt(x, y - i, z).location
            if (isSafeLocation(checkLocation)) {
                return checkLocation.add(0.5, 0.0, 0.5)
            }
        }
        
        return null
    }
    
    /**
     * 检查位置是否安全
     */
    private fun isSafeLocation(location: Location): Boolean {
        val world = location.world ?: return false
        val block = world.getBlockAt(location)
        val above = world.getBlockAt(location.blockX, location.blockY + 1, location.blockZ)
        
        // 脚下必须是固体方块，头上两格必须是空气
        return block.type.isSolid &&
               above.isEmpty &&
               world.getBlockAt(location.blockX, location.blockY + 2, location.blockZ).isEmpty
    }
    
    /**
     * 处理玩家移动事件
     */
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val request = pendingTeleports[player] ?: return
        
        // 如果配置不取消移动，则忽略
        if (!request.teleportConfig.cancelOnMove) return
        
        // 检查是否真的移动了（忽略视角转动）
        val from = event.from
        val to = event.to ?: return
        
        if (from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ) {
            cancelTeleport(player)
        }
    }
    
    /**
     * 处理玩家受伤事件
     */
    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        val request = pendingTeleports[player] ?: return
        
        // 如果配置不取消受伤，则忽略
        if (!request.teleportConfig.cancelOnDamage) return
        
        cancelTeleport(player)
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cancelTeleport(event.player)
    }
}

/**
 * 传送请求数据类
 */
data class TeleportRequest(
    val player: Player,
    val warp: Warp,
    val startLocation: Location,
    val teleportConfig: TeleportConfig,
    val task: BukkitTask?,
    val onCancel: (() -> Unit)? = null
)
