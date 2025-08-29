package com.github.postyizhan.util

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import org.bukkit.entity.Player

/**
 * 地标操作工具类 - 封装常用的地标操作逻辑
 */
object WarpOperationUtils {
    
    /**
     * 操作结果
     */
    data class OperationResult(
        val success: Boolean,
        val messageKey: String,
        val warp: Warp? = null,
        val replacements: Map<String, String> = emptyMap()
    )
    
    /**
     * 安全地执行地标操作
     */
    inline fun executeWarpOperation(
        plugin: PostWarps,
        player: Player,
        actionName: String,
        warpName: String,
        crossinline operation: (Warp) -> Boolean
    ): OperationResult {
        // 获取地标
        val warpResult = ValidationUtils.getWarp(plugin, player, warpName)
        if (warpResult.result != ValidationUtils.ValidationResult.SUCCESS) {
            return OperationResult(
                false, 
                ValidationUtils.getMessageKey(warpResult.result, actionName),
                replacements = mapOf("name" to if (warpName.isEmpty()) "selected warp" else warpName)
            )
        }
        
        val warp = warpResult.warp!!
        
        // 验证所有权
        val ownershipResult = ValidationUtils.validateOwnership(player, warp)
        if (ownershipResult != ValidationUtils.ValidationResult.SUCCESS) {
            return OperationResult(
                false,
                ValidationUtils.getMessageKey(ownershipResult, actionName)
            )
        }
        
        // 执行操作
        val success = operation(warp)
        return OperationResult(
            success,
            if (success) "$actionName.success" else "$actionName.failed",
            warp,
            mapOf("name" to warp.name)
        )
    }
    
    /**
     * 设置地标公开状态
     */
    fun setWarpPublicState(
        plugin: PostWarps,
        player: Player,
        warpName: String,
        isPublic: Boolean
    ): OperationResult {
        val actionName = if (isPublic) "public" else "private"
        
        // 获取地标
        val warpResult = ValidationUtils.getWarp(plugin, player, warpName)
        if (warpResult.result != ValidationUtils.ValidationResult.SUCCESS) {
            return OperationResult(
                false,
                ValidationUtils.getMessageKey(warpResult.result, actionName),
                replacements = mapOf("name" to if (warpName.isEmpty()) "selected warp" else warpName)
            )
        }
        
        val warp = warpResult.warp!!
        
        // 验证所有权
        val ownershipResult = ValidationUtils.validateOwnership(player, warp)
        if (ownershipResult != ValidationUtils.ValidationResult.SUCCESS) {
            return OperationResult(
                false,
                ValidationUtils.getMessageKey(ownershipResult, actionName)
            )
        }
        
        // 验证状态
        val stateResult = ValidationUtils.validatePublicState(warp, isPublic)
        if (stateResult != ValidationUtils.ValidationResult.SUCCESS) {
            return OperationResult(
                false,
                ValidationUtils.getMessageKey(stateResult, actionName),
                warp,
                mapOf("name" to warp.name)
            )
        }
        
        // 检查费用（设置为公开时）
        if (isPublic && !plugin.getEconomyService().chargeSetPublicCost(player)) {
            return OperationResult(false, "$actionName.insufficient-funds")
        }
        
        // 执行操作（使用增强数据库管理器）
        val success = plugin.getEnhancedDatabaseManager().setWarpPublicSync(warp.id, isPublic)
        return OperationResult(
            success,
            if (success) "$actionName.success" else "update.failed",
            warp,
            mapOf("name" to warp.name)
        )
    }
    
    /**
     * 刷新菜单数据并重新打开
     */
    fun refreshMenu(plugin: PostWarps, player: Player, warp: Warp?, isPublic: Boolean? = null) {
        if (warp != null && isPublic != null) {
            val data = plugin.getMenuManager().getPlayerData(player)
            data["is_public"] = isPublic
        }
        
        val currentMenu = plugin.getMenuManager().getOpenMenu(player)
        if (currentMenu != null) {
            plugin.getMenuManager().openMenu(player, currentMenu)
        }
    }
}