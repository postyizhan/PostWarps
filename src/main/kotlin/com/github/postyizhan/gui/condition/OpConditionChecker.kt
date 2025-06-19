package com.github.postyizhan.gui.condition

import org.bukkit.entity.Player

/**
 * OP状态条件检查器
 */
class OpConditionChecker : ConditionChecker {

    override fun checkCondition(condition: String, player: Player, data: Map<String, Any>): Boolean {
        val result = when (condition) {
            "op" -> player.isOp
            "!op" -> !player.isOp
            else -> false
        }

        return result
    }
    
    override fun getSupportedPrefixes(): List<String> {
        return listOf("op", "!op")
    }
}
