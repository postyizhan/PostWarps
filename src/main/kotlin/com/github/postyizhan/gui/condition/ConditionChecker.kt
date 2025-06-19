package com.github.postyizhan.gui.condition

import org.bukkit.entity.Player

/**
 * 条件检查器接口
 */
interface ConditionChecker {
    /**
     * 检查条件是否满足
     * @param condition 条件字符串
     * @param player 玩家
     * @param data 数据上下文
     * @return 是否满足条件
     */
    fun checkCondition(condition: String, player: Player, data: Map<String, Any>): Boolean
    
    /**
     * 获取支持的条件前缀
     * @return 支持的条件前缀列表
     */
    fun getSupportedPrefixes(): List<String>
}
