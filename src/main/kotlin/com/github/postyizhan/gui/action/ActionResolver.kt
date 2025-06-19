package com.github.postyizhan.gui.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.icon.IconConfig
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

/**
 * 动作解析器，处理子图标动作继承
 */
class ActionResolver(private val plugin: PostWarps) {
    
    /**
     * 获取物品的动作列表（支持子图标继承）
     * @param mainConfig 主配置
     * @param iconConfig 子图标配置（可为null）
     * @param player 玩家
     * @param data 数据上下文
     * @return 动作列表
     */
    fun resolveActions(
        mainConfig: ConfigurationSection,
        iconConfig: IconConfig?,
        player: Player,
        @Suppress("UNUSED_PARAMETER") data: Map<String, Any>
    ): List<String> {
        // 优先使用子图标的动作
        val iconActions = iconConfig?.action
        if (iconActions != null && iconActions.isNotEmpty()) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] 玩家 ${player.name} 使用子图标动作: $iconActions")
            }
            return iconActions
        }
        
        // 使用主配置的动作
        val mainActions = when {
            mainConfig.isList("action") -> mainConfig.getStringList("action")
            mainConfig.isString("action") -> listOf(mainConfig.getString("action") ?: "")
            else -> emptyList()
        }
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] 玩家 ${player.name} 使用主配置动作: $mainActions")
        }
        
        return mainActions
    }
    
    /**
     * 检查动作是否有效
     * @param actions 动作列表
     * @return 是否有有效动作
     */
    fun hasValidActions(actions: List<String>): Boolean {
        return actions.isNotEmpty() && actions.any { it.isNotBlank() }
    }
}
