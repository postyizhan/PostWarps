package com.github.postyizhan.manager

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.messaging.PluginMessageListener
import org.bukkit.scheduler.BukkitTask
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WarpManager(private val plugin: PostWarps) : PluginMessageListener {
    // 缓存所有地标
    private val warpCache = ConcurrentHashMap<String, Warp>()
    
    // 待传送玩家列表
    private val pendingTeleports = ConcurrentHashMap<UUID, BukkitTask>()
    
    init {
        // 如果启用了BungeeCord，注册通信通道
        if (plugin.enableBungeeCord) {
            plugin.server.messenger.registerOutgoingPluginChannel(plugin, "BungeeCord")
            plugin.server.messenger.registerIncomingPluginChannel(plugin, "BungeeCord", this)
            
            if (plugin.debugMode) {
                plugin.logger.info("BungeeCord support enabled")
            }
        }
        
        // 加载所有地标到缓存
        loadAllWarps()
    }
    
    // 加载所有地标到缓存
    private fun loadAllWarps() {
        warpCache.clear()
        
        val warps = plugin.databaseManager.getAllWarps()
        for (warp in warps) {
            warpCache[warp.name.lowercase()] = warp
        }
        
        if (plugin.debugMode) {
            plugin.logger.info("Loaded ${warpCache.size} warps into cache")
        }
    }
    
    // 创建地标
    fun createWarp(player: Player, name: String, description: String): Boolean {
        // 检查名称是否已存在
        if (warpExists(name)) {
            return false
        }
        
        // 检查是否达到地标上限
        val maxWarps = plugin.config.getInt("settings.maxWarpsPerPlayer", 5)
        if (maxWarps > 0 && plugin.databaseManager.getWarpCount(player.uniqueId) >= maxWarps) {
            return false
        }
        
        // 创建地标
        val serverName = if (plugin.enableBungeeCord) {
            plugin.config.getString("bungeecord.serverName") ?: "unknown"
        } else {
            ""
        }
        
        val warp = Warp.fromLocation(
            name = name,
            ownerUUID = player.uniqueId,
            location = player.location,
            description = description,
            server = serverName
        )
        
        // 保存到数据库
        val success = plugin.databaseManager.createWarp(warp)
        
        // 添加到缓存
        if (success) {
            warpCache[warp.name.lowercase()] = warp
        }
        
        return success
    }
    
    // 删除地标
    fun deleteWarp(player: Player, name: String): Boolean {
        // 获取地标
        val warp = getWarp(name) ?: return false
        
        // 检查权限（只能删除自己的地标，除非是管理员）
        if (warp.ownerUUID != player.uniqueId && !player.hasPermission("postwarps.admin")) {
            return false
        }
        
        // 从数据库删除
        val success = plugin.databaseManager.deleteWarp(name, warp.ownerUUID)
        
        // 从缓存删除
        if (success) {
            warpCache.remove(name.lowercase())
        }
        
        return success
    }
    
    // 获取地标
    fun getWarp(name: String): Warp? {
        return warpCache[name.lowercase()]
    }
    
    // 获取玩家的所有地标
    fun getWarpsByOwner(uuid: UUID): List<Warp> {
        return warpCache.values.filter { it.ownerUUID == uuid }
    }
    
    // 检查地标是否存在
    fun warpExists(name: String): Boolean {
        return warpCache.containsKey(name.lowercase())
    }
    
    // 传送到地标
    fun teleportToWarp(player: Player, warpName: String): Boolean {
        val warp = getWarp(warpName) ?: return false
        
        // 检查是否跨服
        if (plugin.enableBungeeCord && warp.server.isNotEmpty() && 
            warp.server != plugin.config.getString("bungeecord.serverName")) {
            // 跨服传送
            sendPlayerToServer(player, warp.server, warpName)
            return true
        }
        
        // 本服传送
        val location = warp.toLocation() ?: return false
        
        // 获取传送延迟
        val delay = plugin.config.getInt("settings.teleportDelay", 3)
        
        if (delay > 0) {
            // 发送等待消息
            player.sendMessage(plugin.i18n.getMessage("prefix") + 
                plugin.i18n.getMessage("warp.teleport.warmup", delay, warp.name))
            
            // 创建延迟传送任务
            val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                // 移除待传送列表
                pendingTeleports.remove(player.uniqueId)
                
                // 执行传送
                player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
                
                // 增加访问次数
                incrementVisits(warp)
                
                // 发送成功消息
                player.sendMessage(plugin.i18n.getMessage("prefix") + 
                    plugin.i18n.getMessage("warp.teleport.success", warp.name))
            }, delay * 20L)
            
            // 添加到待传送列表
            pendingTeleports[player.uniqueId] = task
        } else {
            // 直接传送
            player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
            
            // 增加访问次数
            incrementVisits(warp)
            
            // 发送成功消息
            player.sendMessage(plugin.i18n.getMessage("prefix") + 
                plugin.i18n.getMessage("warp.teleport.success", warp.name))
        }
        
        return true
    }
    
    // 取消待传送的玩家
    fun cancelPendingTeleport(player: Player) {
        val task = pendingTeleports.remove(player.uniqueId)
        task?.cancel()
    }
    
    // 检查玩家是否正在等待传送
    fun isPendingTeleport(player: Player): Boolean {
        return pendingTeleports.containsKey(player.uniqueId)
    }
    
    // 增加地标访问次数
    private fun incrementVisits(warp: Warp) {
        if (warp.id > 0) {
            plugin.databaseManager.incrementVisits(warp.id)
            warp.visits++
        }
    }
    
    // 跨服传送玩家
    private fun sendPlayerToServer(player: Player, server: String, warpName: String) {
        if (!plugin.enableBungeeCord) return
        
        try {
            val outStream = ByteArrayOutputStream()
            val dataStream = DataOutputStream(outStream)
            
            // 构建BungeeCord消息
            dataStream.writeUTF("Connect")
            dataStream.writeUTF(server)
            
            // 发送消息
            player.sendPluginMessage(plugin, "BungeeCord", outStream.toByteArray())
            
            // 发送消息
            player.sendMessage(plugin.i18n.getMessage("prefix") + 
                plugin.i18n.getMessage("warp.teleport.different_server", server, warpName))
            
            if (plugin.debugMode) {
                plugin.logger.info("Sending ${player.name} to server $server for warp $warpName")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error sending player to server: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // 处理BungeeCord返回的消息
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != "BungeeCord") return
        
        // 这里可以处理从BungeeCord返回的消息
        // 例如，确认玩家已成功传送到另一个服务器
    }
}
