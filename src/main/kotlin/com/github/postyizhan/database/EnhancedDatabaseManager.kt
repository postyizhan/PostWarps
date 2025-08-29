package com.github.postyizhan.database

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import com.github.postyizhan.service.WarpCacheService
import kotlinx.coroutines.*
import java.util.*

/**
 * 增强的数据库管理器 - 集成缓存服务提升性能
 */
class EnhancedDatabaseManager(
    private val plugin: PostWarps,
    private val baseManager: DatabaseManager
) {
    
    private val cacheService = WarpCacheService(plugin)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 获取地标（优先从缓存）
     */
    suspend fun getWarp(id: Int): Warp? {
        return cacheService.getWarp(id)
    }
    
    /**
     * 获取地标（按名称和所有者）
     */
    suspend fun getWarp(name: String, owner: UUID): Warp? {
        return cacheService.getWarp(name, owner)
    }
    
    /**
     * 同步方法（兼容现有代码）
     */
    fun getWarpSync(id: Int): Warp? {
        return runBlocking { getWarp(id) }
    }
    
    fun getWarpSync(name: String, owner: UUID): Warp? {
        return runBlocking { getWarp(name, owner) }
    }
    
    /**
     * 获取玩家地标列表
     */
    suspend fun getPlayerWarps(owner: UUID): List<Warp> {
        return cacheService.getPlayerWarps(owner)
    }
    
    /**
     * 创建地标
     */
    suspend fun createWarp(warp: Warp): Boolean = withContext(Dispatchers.IO) {
        val success = baseManager.createWarp(warp)
        if (success) {
            // 重新加载以获取正确的ID
            val createdWarp = baseManager.getWarp(warp.name, warp.owner)
            createdWarp?.let { cacheService.updateCache(it) }
        }
        success
    }
    
    /**
     * 更新地标
     */
    suspend fun updateWarp(warp: Warp): Boolean = withContext(Dispatchers.IO) {
        // 使用现有的更新方法
        val success = baseManager.updateWarpDescription(warp.id, warp.description) &&
                     baseManager.updateWarpMaterial(warp.id, warp.displayMaterial, warp.skullOwner, warp.skullTexture)
        if (success) {
            cacheService.updateCache(warp)
        }
        success
    }
    
    /**
     * 设置地标公开状态
     */
    suspend fun setWarpPublic(id: Int, isPublic: Boolean): Boolean = withContext(Dispatchers.IO) {
        val success = baseManager.setWarpPublic(id, isPublic)
        if (success) {
            // 更新缓存中的地标
            val updatedWarp = baseManager.getWarp(id)
            updatedWarp?.let { cacheService.updateCache(it) }
        }
        success
    }
    
    /**
     * 删除地标
     */
    suspend fun deleteWarp(id: Int): Boolean = withContext(Dispatchers.IO) {
        val success = baseManager.deleteWarp(id)
        if (success) {
            cacheService.removeFromCache(id)
        }
        success
    }
    
    /**
     * 预热缓存 - 加载热门地标
     */
    suspend fun warmupCache() = withContext(Dispatchers.IO) {
        try {
            // 加载公开地标
            val publicWarps = baseManager.getAllPublicWarps().take(50)
            publicWarps.forEach { cacheService.updateCache(it) }
            
            plugin.logger.info("Cache warmed up with ${publicWarps.size} public warps")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to warm up cache: ${e.message}")
        }
    }
    
    /**
     * 委托给基础管理器的方法
     */
    fun init() = baseManager.init()
    fun close() {
        cacheService.shutdown()
        scope.cancel()
        baseManager.close()
    }
    
    // 同步委托方法
    fun getAllPublicWarps() = baseManager.getAllPublicWarps()
    fun getWarpsByOwner(owner: UUID) = baseManager.getPlayerWarps(owner)
    fun createWarpSync(warp: Warp) = baseManager.createWarp(warp)
    fun updateWarpSync(warp: Warp): Boolean {
        return baseManager.updateWarpDescription(warp.id, warp.description) &&
               baseManager.updateWarpMaterial(warp.id, warp.displayMaterial, warp.skullOwner, warp.skullTexture)
    }
    fun setWarpPublicSync(id: Int, isPublic: Boolean) = baseManager.setWarpPublic(id, isPublic)
    fun deleteWarpSync(id: Int) = baseManager.deleteWarp(id)
}