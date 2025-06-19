package com.github.postyizhan.gui.condition

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player

/**
 * 条件管理器，统一管理所有条件检查器
 */
class ConditionManager(private val plugin: PostWarps) {
    
    private val checkers = mutableListOf<ConditionChecker>()
    
    init {
        // 注册所有条件检查器
        registerChecker(PermissionConditionChecker())
        registerChecker(OpConditionChecker())
        registerChecker(DataConditionChecker())
        registerChecker(PlayerConditionChecker())
    }
    
    /**
     * 注册条件检查器
     * @param checker 条件检查器
     */
    fun registerChecker(checker: ConditionChecker) {
        checkers.add(checker)
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] Registered condition checker: ${checker::class.simpleName}")
        }
    }
    
    /**
     * 检查条件是否满足
     * @param condition 条件字符串
     * @param player 玩家
     * @param data 数据上下文
     * @return 是否满足条件
     */
    fun checkCondition(condition: String, player: Player, data: Map<String, Any>): Boolean {
        if (condition.isBlank()) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Empty condition, returning true")
            }
            return true // 空条件默认为true
        }

        // 遍历所有检查器，找到能处理此条件的检查器
        for (checker in checkers) {
            for (prefix in checker.getSupportedPrefixes()) {
                if (condition.startsWith(prefix) || condition == prefix.trim()) {
                    val result = checker.checkCondition(condition, player, data)
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("[DEBUG] Condition check: '$condition' -> $result (using ${checker::class.simpleName}) for player ${player.name}")
                    }
                    return result
                }
            }
        }

        // 没有找到合适的检查器
        if (plugin.isDebugEnabled()) {
            plugin.logger.warning("[DEBUG] Unknown condition type: '$condition'")
        } else {
            plugin.logger.warning("Unknown condition type: '$condition'")
        }
        return false
    }
    
    /**
     * 获取所有支持的条件前缀
     * @return 支持的条件前缀列表
     */
    fun getSupportedConditions(): List<String> {
        return checkers.flatMap { it.getSupportedPrefixes() }
    }
}
