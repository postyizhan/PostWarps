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
     * 获取物品的动作列表（支持子图标继承和点击类型动作）
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
        data: Map<String, Any>
    ): List<String> {
        // 获取点击类型
        val clickType = data["click_type"] as? String ?: "left"

        // 优先使用子图标的动作
        val iconActions = iconConfig?.action ?: emptyList()
        if (iconActions.isNotEmpty()) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] 玩家 ${player.name} 使用子图标动作 ($clickType): $iconActions")
            }
            return iconActions
        }

        // 使用主配置的动作
        val mainActions = resolveMainConfigActions(mainConfig, clickType)

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] 玩家 ${player.name} 使用主配置动作 ($clickType): $mainActions")
        }

        return mainActions
    }

    /**
     * 从主配置中解析动作（支持点击类型）
     * @param config 配置节
     * @param clickType 点击类型
     * @return 动作列表
     */
    private fun resolveMainConfigActions(config: ConfigurationSection, clickType: String): List<String> {
        // 获取action配置节
        val actionSection = config.getConfigurationSection("action")
        if (actionSection != null) {
            // 新格式：action下有子配置
            return getClickTypeActionsFromSection(actionSection, clickType)
        }

        // 如果没有action配置节，返回空列表
        return emptyList()
    }



    /**
     * 从动作配置节中获取特定点击类型的动作
     * @param actionSection 动作配置节
     * @param clickType 点击类型
     * @return 动作列表
     */
    private fun getClickTypeActionsFromSection(actionSection: ConfigurationSection, clickType: String): List<String> {
        val allActions = mutableListOf<String>()

        // 首先添加all节点的动作（如果存在）
        val allNodeActions = when {
            actionSection.isList("all") -> actionSection.getStringList("all")
            actionSection.isString("all") -> listOf(actionSection.getString("all") ?: "")
            else -> emptyList()
        }
        allActions.addAll(allNodeActions)

        // 然后尝试获取特定点击类型的动作
        val specificActions = when {
            actionSection.isList(clickType) -> actionSection.getStringList(clickType)
            actionSection.isString(clickType) -> listOf(actionSection.getString(clickType) ?: "")
            else -> emptyList()
        }

        if (specificActions.isNotEmpty()) {
            allActions.addAll(specificActions)
        }

        return allActions
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
