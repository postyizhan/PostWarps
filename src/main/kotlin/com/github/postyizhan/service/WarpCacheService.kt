package com.github.postyizhan.service

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * 地标智能缓存服务 - 减少数据库查询，提升性能
 */
class WarpCacheService(private val plugin: PostWarps) {
    
    // 缓存配置
    private val cacheExpireTime = 300_000L // 5分钟过期
    private val maxCacheSize = 1000
    
    // 缓存存储
    private val warpCache = ConcurrentHashMap<Int, CacheEntry>()
    private val nameCache = ConcurrentHashMap<String, Int>() // name -> id映射
    private val ownerCache = ConcurrentHashMap<UUID, MutableSet<Int>>() // owner -> warp ids
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 清理任务
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "WarpCache-Cleanup").apply { isDaemon = true }
    }
    
    data class CacheEntry(
        val warp: Warp,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 300_000L
    }
    
    init {
        // 启动定期清理
        cleanupExecutor.scheduleAtFixedRate(::cleanupExpiredEntries, 60, 60, java.util.concurrent.TimeUnit.SECONDS)
    }
    
    /**
     * 根据ID获取地标（优先从缓存）
     */
    suspend fun getWarp(id: Int): Warp? = withContext(Dispatchers.IO) {
        val cached = warpCache[id]
        if (cached != null && !cached.isExpired()) {
            return@withContext cached.warp
        }
        
        // 从数据库获取
        val warp = plugin.getDatabaseManager().getWarp(id)
        if (warp != null) {
            cacheWarp(warp)
        } else {
            // 从缓存中移除无效条目
            removeFromCache(id)
        }
        warp
    }
    
    /**
     * 根据名称和所有者获取地标
     */
    suspend fun getWarp(name: String, owner: UUID): Warp? = withContext(Dispatchers.IO) {
        val cacheKey = "${name}_${owner}"
        val cachedId = nameCache[cacheKey]
        
        if (cachedId != null) {
            val warp = getWarp(cachedId)
            if (warp?.name == name && warp.owner == owner) {
                return@withContext warp
            }
        }
        
        // 从数据库获取
        val warp = plugin.getDatabaseManager().getWarp(name, owner)
        if (warp != null) {
            cacheWarp(warp)
        }
        warp
    }
    
    /**
     * 获取玩家的地标列表
     */
    suspend fun getPlayerWarps(owner: UUID): List<Warp> = withContext(Dispatchers.IO) {
        val cachedIds = ownerCache[owner]
        val cachedWarps = mutableListOf<Warp>()
        val expiredIds = mutableSetOf<Int>()
        
        cachedIds?.forEach { id ->
            val cached = warpCache[id]
            if (cached != null && !cached.isExpired()) {
                cachedWarps.add(cached.warp)
            } else {
                expiredIds.add(id)
            }
        }
        
        // 清理过期缓存
        expiredIds.forEach { removeFromCache(it) }
        
        // 如果缓存不完整，从数据库获取
        if (cachedIds == null || expiredIds.isNotEmpty()) {
            val dbWarps = plugin.getDatabaseManager().getPlayerWarps(owner)
            dbWarps.forEach { cacheService -> cacheWarp(cacheService) }
            return@withContext dbWarps
        }
        
        cachedWarps
    }
    
    /**
     * 缓存地标
     */
    private fun cacheWarp(warp: Warp) {
        // 检查缓存大小
        if (warpCache.size >= maxCacheSize) {
            cleanupExpiredEntries()
            if (warpCache.size >= maxCacheSize) {
                // 移除最旧的条目
                val oldestEntry = warpCache.values.minByOrNull { it.timestamp }
                oldestEntry?.let { removeFromCache(it.warp.id) }
            }
        }
        
        warpCache[warp.id] = CacheEntry(warp)
        nameCache["${warp.name}_${warp.owner}"] = warp.id
        ownerCache.computeIfAbsent(warp.owner) { ConcurrentHashMap.newKeySet() }.add(warp.id)
    }
    
    /**
     * 从缓存中移除地标
     */
    fun removeFromCache(id: Int) {
        val entry = warpCache.remove(id)
        if (entry != null) {
            val warp = entry.warp
            nameCache.remove("${warp.name}_${warp.owner}")
            ownerCache[warp.owner]?.remove(id)
        }
    }
    
    /**
     * 更新缓存中的地标
     */
    fun updateCache(warp: Warp) {
        removeFromCache(warp.id)
        cacheWarp(warp)
    }
    
    /**
     * 清理过期条目
     */
    private fun cleanupExpiredEntries() {
        val expiredIds = warpCache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredIds.forEach { removeFromCache(it) }
        
        if (plugin.isDebugEnabled() && expiredIds.isNotEmpty()) {
            plugin.logger.info("[Cache] Cleaned up ${expiredIds.size} expired entries")
        }
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAll() {
        warpCache.clear()
        nameCache.clear()
        ownerCache.clear()
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): Map<String, Int> {
        return mapOf(
            "warp_cache_size" to warpCache.size,
            "name_cache_size" to nameCache.size,
            "owner_cache_size" to ownerCache.size
        )
    }
    
    /**
     * 关闭服务
     */
    fun shutdown() {
        cleanupExecutor.shutdown()
        scope.cancel()
        clearAll()
    }
}