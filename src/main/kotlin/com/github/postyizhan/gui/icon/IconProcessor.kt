package com.github.postyizhan.gui.icon

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.condition.ConditionManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

/**
 * 子图标处理器
 */
class IconProcessor(private val plugin: PostWarps) {
    
    private val conditionManager = ConditionManager(plugin)
    
    /**
     * 处理子图标配置，找到第一个满足条件的子图标
     * @param iconsSection 子图标配置节
     * @param player 玩家
     * @param data 数据上下文
     * @return 匹配的子图标配置，如果没有匹配则返回null
     */
    fun processIcons(
        iconsSection: ConfigurationSection,
        player: Player,
        data: Map<String, Any>
    ): IconConfig? {
        val iconsList = parseIconsSection(iconsSection)
        
        // 遍历子图标，找到第一个满足条件的
        for (iconMap in iconsList) {
            val iconConfig = IconConfig.fromMap(iconMap)
            
            // 检查条件
            val condition = iconConfig.condition
            if (condition == null || conditionManager.checkCondition(condition, player, data)) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] Player ${player.name} matched icon condition: '$condition'")
                }
                return iconConfig
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] No matching icon condition found for player ${player.name}")
        }
        
        return null
    }
    
    /**
     * 解析icons配置节
     * @param iconsSection 配置节
     * @return 图标配置列表
     */
    private fun parseIconsSection(iconsSection: ConfigurationSection): List<Map<String, Any>> {
        // 检查是否是直接的列表配置
        val iconsList = iconsSection.getList("")

        return if (iconsList != null) {
            // 如果icons是一个列表
            iconsList.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                item as? Map<String, Any>
            }
        } else {
            // 如果icons是一个配置节，转换为列表格式
            val iconKeys = iconsSection.getKeys(false)
            iconKeys.mapNotNull { key ->
                val iconConfig = iconsSection.getConfigurationSection(key)
                iconConfig?.getValues(false)
            }
        }
    }
    
    /**
     * 获取条件管理器
     * @return 条件管理器实例
     */
    fun getConditionManager(): ConditionManager {
        return conditionManager
    }
}
