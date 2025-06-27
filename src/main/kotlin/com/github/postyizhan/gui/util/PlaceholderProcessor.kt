package com.github.postyizhan.gui.util

import com.github.postyizhan.gui.core.MenuContext
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 占位符处理器
 * 统一处理所有类型的占位符替换
 */
class PlaceholderProcessor {
    
    companion object {
        /**
         * 处理通用占位符
         * @param text 原始文本
         * @param context 菜单上下文
         * @return 处理后的文本
         */
        fun processPlaceholders(text: String, context: MenuContext): String {
            var result = text

            // 处理玩家占位符
            result = processPlayerPlaceholders(result, context.player)

            // 处理菜单数据占位符
            result = processMenuDataPlaceholders(result, context)

            // 处理玩家数据占位符（带国际化支持）
            result = processPlayerDataPlaceholders(result, context.playerData, context.player)

            return result
        }
        
        /**
         * 处理地标特定占位符
         * @param text 原始文本
         * @param context 菜单上下文
         * @param warp 地标对象
         * @return 处理后的文本
         */
        fun processWarpPlaceholders(text: String, context: MenuContext, warp: Warp): String {
            var result = processPlaceholders(text, context)
            
            // 地标基本信息
            result = result.replace("{name}", warp.name)
                .replace("{owner}", warp.ownerName)
                .replace("{world}", warp.worldName)
                .replace("{coords}", warp.getFormattedCoordinates())
                .replace("{desc}", warp.description)
            
            // 公开/私有状态（国际化）
            val publicState = if (warp.isPublic) {
                MessageUtil.getMessage("status.public", context.player)
            } else {
                MessageUtil.getMessage("status.private", context.player)
            }
            result = result.replace("{public_state}", publicState)
            
            return result
        }
        
        /**
         * 处理玩家占位符
         */
        private fun processPlayerPlaceholders(text: String, player: Player): String {
            return text.replace("{player}", player.name)
                .replace("{player_name}", player.name)
                .replace("{player_uuid}", player.uniqueId.toString())
        }
        
        /**
         * 处理菜单数据占位符
         */
        private fun processMenuDataPlaceholders(text: String, context: MenuContext): String {
            val menuData = context.menuData ?: return text
            var result = text
            
            // 分页信息
            result = result.replace("{current_page}", (menuData.currentPage + 1).toString())
                .replace("{total_pages}", menuData.totalPages.toString())
                .replace("{has_prev}", (menuData.currentPage > 0).toString())
                .replace("{has_next}", (menuData.currentPage < menuData.totalPages - 1).toString())
            
            // 静态数据
            for ((key, value) in menuData.staticData) {
                result = result.replace("{$key}", value.toString())
            }
            
            // 动态数据
            for ((key, value) in menuData.dynamicData) {
                result = result.replace("{$key}", value.toString())
            }
            
            return result
        }
        
        /**
         * 处理玩家数据占位符
         */
        private fun processPlayerDataPlaceholders(text: String, playerData: Map<String, Any>): String {
            var result = text

            for ((key, value) in playerData) {
                result = result.replace("{$key}", value.toString())
            }

            // 特殊占位符处理
            result = result.replace("{name}", playerData["name"]?.toString() ?: "")
                .replace("{desc}", playerData["desc"]?.toString() ?: "")

            return result
        }

        /**
         * 处理带玩家上下文的数据占位符（支持国际化）
         */
        fun processPlayerDataPlaceholders(text: String, playerData: Map<String, Any>, player: Player): String {
            var result = processPlayerDataPlaceholders(text, playerData)

            // 公开/私有状态显示（国际化）
            val isPublic = playerData["public"] as? Boolean ?: playerData["is_public"] as? Boolean ?: false
            val publicState = if (isPublic) {
                MessageUtil.getMessage("status.public", player)
            } else {
                MessageUtil.getMessage("status.private", player)
            }
            result = result.replace("{public_state}", publicState)

            return result
        }
    }
}
