package com.github.postyizhan.gui.condition

import org.bukkit.entity.Player

/**
 * 权限条件检查器
 */
class PermissionConditionChecker : ConditionChecker {
    
    override fun checkCondition(condition: String, player: Player, data: Map<String, Any>): Boolean {
        return when {
            condition.startsWith("perm ") -> {
                val permission = condition.substring(5).trim()
                player.hasPermission(permission)
            }
            condition.startsWith("!perm ") -> {
                val permission = condition.substring(6).trim()
                !player.hasPermission(permission)
            }
            else -> false
        }
    }
    
    override fun getSupportedPrefixes(): List<String> {
        return listOf("perm ", "!perm ")
    }
}
