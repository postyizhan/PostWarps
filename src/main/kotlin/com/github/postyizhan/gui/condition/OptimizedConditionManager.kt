package com.github.postyizhan.gui.condition

import com.github.postyizhan.PostWarps
import com.github.postyizhan.constants.ConfigurableConstants
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * 优化的条件管理器 - 使用Map缓存提高查找效率
 * 避免嵌套循环，提供更好的性能
 */
class OptimizedConditionManager(private val plugin: PostWarps) {
    
    // 条件检查器工厂
    private val checkerFactory = ConditionCheckerFactory(plugin)

    // 条件检查器列表
    private val checkers = mutableListOf<ConditionChecker>()

    // 前缀到检查器的映射缓存
    private val prefixToCheckerMap = ConcurrentHashMap<String, ConditionChecker>()

    // 条件结果缓存（可选，用于频繁检查的条件）
    private val conditionResultCache = ConcurrentHashMap<String, CachedResult>()

    // 缓存清理计数器
    private var cacheCleanupCounter = 0

    init {
        // 注册所有条件检查器
        registerDefaultCheckers()

        // 构建前缀映射缓存
        rebuildPrefixCache()
    }
    
    /**
     * 注册默认的条件检查器
     */
    private fun registerDefaultCheckers() {
        val defaultCheckers = checkerFactory.createDefaultCheckers()
        for (checker in defaultCheckers) {
            if (checkerFactory.validateChecker(checker)) {
                registerChecker(checker)
            } else {
                logWarning("跳过无效的条件检查器: ${checker::class.simpleName}")
            }
        }
    }
    
    /**
     * 注册条件检查器
     * @param checker 条件检查器
     */
    fun registerChecker(checker: ConditionChecker) {
        checkers.add(checker)
        
        // 更新前缀映射缓存
        for (prefix in checker.getSupportedPrefixes()) {
            prefixToCheckerMap[prefix] = checker
        }
        
        logDebug("注册条件检查器: ${checker::class.simpleName}")
    }
    
    /**
     * 移除条件检查器
     * @param checkerClass 检查器类
     */
    fun unregisterChecker(checkerClass: Class<out ConditionChecker>) {
        val iterator = checkers.iterator()
        while (iterator.hasNext()) {
            val checker = iterator.next()
            if (checker::class.java == checkerClass) {
                iterator.remove()
                
                // 从前缀映射中移除
                for (prefix in checker.getSupportedPrefixes()) {
                    prefixToCheckerMap.remove(prefix)
                }
                
                logDebug("移除条件检查器: ${checker::class.simpleName}")
                break
            }
        }
    }
    
    /**
     * 检查条件是否满足 - 优化版本
     * @param condition 条件字符串
     * @param player 玩家
     * @param data 数据上下文
     * @return 是否满足条件
     */
    fun checkCondition(condition: String, player: Player, data: Map<String, Any>): Boolean {
        if (condition.isBlank()) {
            logDebug("空条件，返回true")
            return true
        }
        
        // 检查缓存（如果启用）
        if (isCacheEnabled()) {
            val cacheKey = buildCacheKey(condition, player, data)
            val cachedResult = conditionResultCache[cacheKey]
            if (cachedResult != null && !cachedResult.isExpired()) {
                logDebug("使用缓存结果: '$condition' -> ${cachedResult.result}")
                return cachedResult.result
            }
        }
        
        // 查找匹配的检查器
        val checker = findMatchingChecker(condition)
        if (checker != null) {
            val result = checker.checkCondition(condition, player, data)
            
            // 缓存结果（如果启用）
            if (isCacheEnabled()) {
                val cacheKey = buildCacheKey(condition, player, data)
                conditionResultCache[cacheKey] = CachedResult(result, System.currentTimeMillis())
                
                // 定期清理缓存
                cleanupCacheIfNeeded()
            }
            
            logDebug("条件检查: '$condition' -> $result (使用 ${checker::class.simpleName}) 玩家 ${player.name}")
            return result
        }
        
        // 没有找到合适的检查器
        logWarning("未知条件类型: '$condition'")
        return false
    }
    
    /**
     * 查找匹配的条件检查器 - 优化版本
     * @param condition 条件字符串
     * @return 匹配的检查器，如果没有找到则返回null
     */
    private fun findMatchingChecker(condition: String): ConditionChecker? {
        // 首先尝试精确匹配
        val exactMatch = prefixToCheckerMap[condition.trim()]
        if (exactMatch != null) {
            return exactMatch
        }
        
        // 然后尝试前缀匹配（按前缀长度降序排列，优先匹配更具体的前缀）
        val sortedPrefixes = prefixToCheckerMap.keys.sortedByDescending { it.length }
        for (prefix in sortedPrefixes) {
            if (condition.startsWith(prefix)) {
                return prefixToCheckerMap[prefix]
            }
        }
        
        return null
    }
    
    /**
     * 重建前缀映射缓存
     */
    private fun rebuildPrefixCache() {
        prefixToCheckerMap.clear()
        for (checker in checkers) {
            for (prefix in checker.getSupportedPrefixes()) {
                prefixToCheckerMap[prefix] = checker
            }
        }
        logDebug("Rebuilt prefix mapping cache with ${prefixToCheckerMap.size} prefixes")
    }
    
    /**
     * 构建缓存键
     * @param condition 条件
     * @param player 玩家
     * @param data 数据
     * @return 缓存键
     */
    private fun buildCacheKey(condition: String, player: Player, data: Map<String, Any>): String {
        // 简化的缓存键，只包含条件和玩家名
        // 对于更复杂的缓存需求，可以包含更多数据
        return "${condition}:${player.name}"
    }
    
    /**
     * 检查是否启用缓存
     * @return 如果启用缓存则返回true
     */
    private fun isCacheEnabled(): Boolean {
        return ConfigurableConstants.Cache.getMaxCacheSize(plugin) > 0
    }
    
    /**
     * 定期清理过期缓存
     */
    private fun cleanupCacheIfNeeded() {
        cacheCleanupCounter++
        if (cacheCleanupCounter >= 100) { // 每100次检查清理一次
            cleanupExpiredCache()
            cacheCleanupCounter = 0
        }
    }
    
    /**
     * 清理过期的缓存条目
     */
    fun cleanupExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val iterator = conditionResultCache.entries.iterator()
        var removedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired(currentTime)) {
                iterator.remove()
                removedCount++
            }
        }
        
        if (removedCount > 0) {
            logDebug("清理过期缓存条目: $removedCount 个")
        }
    }
    
    /**
     * 清理所有缓存
     */
    fun clearAllCache() {
        val size = conditionResultCache.size
        conditionResultCache.clear()
        logDebug("清理所有缓存条目: $size 个")
    }
    
    /**
     * 清理指定玩家的缓存
     * @param player 玩家
     */
    fun clearPlayerCache(player: Player) {
        val playerName = player.name
        val iterator = conditionResultCache.keys.iterator()
        var removedCount = 0
        
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.endsWith(":$playerName")) {
                iterator.remove()
                removedCount++
            }
        }
        
        if (removedCount > 0) {
            logDebug("清理玩家 $playerName 的缓存条目: $removedCount 个")
        }
    }
    
    /**
     * 获取所有支持的条件前缀
     * @return 支持的条件前缀列表
     */
    fun getSupportedConditions(): List<String> {
        return prefixToCheckerMap.keys.toList()
    }
    
    /**
     * 获取缓存统计信息
     * @return 缓存统计
     */
    fun getCacheStats(): CacheStats {
        val currentTime = System.currentTimeMillis()
        var expiredCount = 0
        
        for (cachedResult in conditionResultCache.values) {
            if (cachedResult.isExpired(currentTime)) {
                expiredCount++
            }
        }
        
        return CacheStats(
            totalEntries = conditionResultCache.size,
            expiredEntries = expiredCount,
            activeEntries = conditionResultCache.size - expiredCount,
            registeredCheckers = checkers.size,
            supportedPrefixes = prefixToCheckerMap.size
        )
    }
    
    /**
     * 记录调试信息
     */
    private fun logDebug(message: String) {
        if (ConfigurableConstants.Debug.isEnabled(plugin)) {
            plugin.logger.info("[DEBUG] OptimizedConditionManager: $message")
        }
    }
    
    /**
     * 记录警告信息
     */
    private fun logWarning(message: String) {
        plugin.logger.warning("OptimizedConditionManager: $message")
    }
    
    /**
     * 缓存结果数据类
     */
    private data class CachedResult(
        val result: Boolean,
        val timestamp: Long,
        val ttlMillis: Long = 30000L // 30秒TTL
    ) {
        fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
            return currentTime - timestamp > ttlMillis
        }
    }
    
    /**
     * 缓存统计数据类
     */
    data class CacheStats(
        val totalEntries: Int,
        val expiredEntries: Int,
        val activeEntries: Int,
        val registeredCheckers: Int,
        val supportedPrefixes: Int
    )
}
