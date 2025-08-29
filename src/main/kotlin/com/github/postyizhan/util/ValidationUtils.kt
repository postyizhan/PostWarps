package com.github.postyizhan.util

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import org.bukkit.entity.Player
import java.util.*

/**
 * 验证工具类 - 集中处理权限验证和地标获取逻辑
 */
object ValidationUtils {
    
    /**
     * 操作结果枚举
     */
    enum class ValidationResult {
        SUCCESS,
        WARP_NOT_FOUND,
        NOT_OWNER,
        ALREADY_PUBLIC,
        ALREADY_PRIVATE,
        PERMISSION_DENIED
    }
    
    /**
     * 地标获取结果
     */
    data class WarpResult(
        val result: ValidationResult,
        val warp: Warp? = null,
        val messageKey: String = ""
    )
    
    /**
     * 获取地标（通过名称或玩家数据中的ID）
     */
    fun getWarp(plugin: PostWarps, player: Player, name: String): WarpResult {
        val warp = if (name.isEmpty()) {
            val data = plugin.getMenuManager().getPlayerData(player)
            val warpId = data["warp_id"] as? Int 
                ?: return WarpResult(ValidationResult.WARP_NOT_FOUND, messageKey = "warp.not-found")
            plugin.getEnhancedDatabaseManager().getWarpSync(warpId)
        } else {
            plugin.getEnhancedDatabaseManager().getWarpSync(name, player.uniqueId)
        }
        
        return if (warp != null) {
            WarpResult(ValidationResult.SUCCESS, warp)
        } else {
            WarpResult(ValidationResult.WARP_NOT_FOUND, messageKey = "warp.not-found")
        }
    }
    
    /**
     * 验证地标所有权
     */
    fun validateOwnership(player: Player, warp: Warp): ValidationResult {
        return if (warp.owner == player.uniqueId || player.hasPermission("postwarps.admin")) {
            ValidationResult.SUCCESS
        } else {
            ValidationResult.NOT_OWNER
        }
    }
    
    /**
     * 验证地标状态（用于公开/私有切换）
     */
    fun validatePublicState(warp: Warp, targetPublic: Boolean): ValidationResult {
        return when {
            warp.isPublic == targetPublic && targetPublic -> ValidationResult.ALREADY_PUBLIC
            warp.isPublic == targetPublic && !targetPublic -> ValidationResult.ALREADY_PRIVATE
            else -> ValidationResult.SUCCESS
        }
    }
    
    /**
     * 验证权限
     */
    fun validatePermission(player: Player, permission: String): ValidationResult {
        return if (player.hasPermission(permission)) {
            ValidationResult.SUCCESS
        } else {
            ValidationResult.PERMISSION_DENIED
        }
    }
    
    /**
     * 获取验证结果对应的消息键
     */
    fun getMessageKey(result: ValidationResult, action: String): String {
        return when (result) {
            ValidationResult.WARP_NOT_FOUND -> "$action.not-found"
            ValidationResult.NOT_OWNER -> "$action.not-owner"
            ValidationResult.ALREADY_PUBLIC -> "$action.already-public"
            ValidationResult.ALREADY_PRIVATE -> "$action.already-private"
            ValidationResult.PERMISSION_DENIED -> "$action.permission-denied"
            ValidationResult.SUCCESS -> "$action.success"
        }
    }
}