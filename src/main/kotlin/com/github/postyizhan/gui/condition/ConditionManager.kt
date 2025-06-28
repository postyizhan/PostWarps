package com.github.postyizhan.gui.condition

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 条件管理器 - 重构后的版本，委托给优化的条件管理器
 * 保持向后兼容性，同时提供更好的性能
 */
class ConditionManager(private val plugin: PostWarps) {

    // 委托给优化的条件管理器
    private val optimizedManager = OptimizedConditionManager(plugin)

    /**
     * 注册条件检查器
     * @param checker 条件检查器
     */
    fun registerChecker(checker: ConditionChecker) {
        optimizedManager.registerChecker(checker)
    }

    /**
     * 移除条件检查器
     * @param checkerClass 检查器类
     */
    fun unregisterChecker(checkerClass: Class<out ConditionChecker>) {
        optimizedManager.unregisterChecker(checkerClass)
    }

    /**
     * 检查条件是否满足 - 委托给优化管理器
     * @param condition 条件字符串
     * @param player 玩家
     * @param data 数据上下文
     * @return 是否满足条件
     */
    fun checkCondition(condition: String, player: Player, data: Map<String, Any>): Boolean {
        return optimizedManager.checkCondition(condition, player, data)
    }

    /**
     * 获取所有支持的条件前缀
     * @return 支持的条件前缀列表
     */
    fun getSupportedConditions(): List<String> {
        return optimizedManager.getSupportedConditions()
    }

    /**
     * 清理过期缓存
     */
    fun cleanupExpiredCache() {
        optimizedManager.cleanupExpiredCache()
    }

    /**
     * 清理所有缓存
     */
    fun clearAllCache() {
        optimizedManager.clearAllCache()
    }

    /**
     * 清理指定玩家的缓存
     * @param player 玩家
     */
    fun clearPlayerCache(player: Player) {
        optimizedManager.clearPlayerCache(player)
    }

    /**
     * 获取缓存统计信息
     * @return 缓存统计
     */
    fun getCacheStats(): OptimizedConditionManager.CacheStats {
        return optimizedManager.getCacheStats()
    }
}
