package com.github.postyizhan.gui.condition

import org.bukkit.entity.Player

/**
 * 数据条件检查器
 */
class DataConditionChecker : ConditionChecker {
    
    override fun checkCondition(condition: String, player: Player, data: Map<String, Any>): Boolean {
        return when {
            condition.startsWith("data ") -> {
                val dataKey = condition.substring(5).trim()
                data[dataKey] as? Boolean ?: false
            }
            condition.startsWith("!data ") -> {
                val dataKey = condition.substring(6).trim()
                !(data[dataKey] as? Boolean ?: false)
            }
            else -> false
        }
    }
    
    override fun getSupportedPrefixes(): List<String> {
        return listOf("data ", "!data ")
    }
}
