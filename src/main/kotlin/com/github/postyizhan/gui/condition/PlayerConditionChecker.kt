package com.github.postyizhan.gui.condition

import org.bukkit.entity.Player

/**
 * 玩家名称条件检查器
 */
class PlayerConditionChecker : ConditionChecker {
    
    override fun checkCondition(condition: String, player: Player, data: Map<String, Any>): Boolean {
        return when {
            condition.startsWith("player ") -> {
                val playerName = condition.substring(7).trim()
                player.name.equals(playerName, ignoreCase = true)
            }
            condition.startsWith("!player ") -> {
                val playerName = condition.substring(8).trim()
                !player.name.equals(playerName, ignoreCase = true)
            }
            else -> false
        }
    }
    
    override fun getSupportedPrefixes(): List<String> {
        return listOf("player ", "!player ")
    }
}
