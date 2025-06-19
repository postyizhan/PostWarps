package com.github.postyizhan.gui.processor

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.action.ActionResolver
import com.github.postyizhan.gui.builder.ItemBuilder
import com.github.postyizhan.gui.icon.IconProcessor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 菜单项处理器，统一处理菜单项的创建和动作解析
 */
class MenuItemProcessor(private val plugin: PostWarps) {
    
    private val iconProcessor = IconProcessor(plugin)
    private val itemBuilder = ItemBuilder(plugin)
    private val actionResolver = ActionResolver(plugin)
    
    /**
     * 创建菜单项
     * @param itemConfig 物品配置
     * @param player 玩家
     * @param data 数据上下文
     * @return 创建的物品，如果失败返回null
     */
    fun createMenuItem(
        itemConfig: ConfigurationSection,
        player: Player,
        data: Map<String, Any>
    ): ItemStack? {
        // 检查显示条件
        if (!checkDisplayCondition(itemConfig, data)) {
            return createFallbackItem(itemConfig)
        }
        
        // 处理子图标
        val matchedIcon = if (itemConfig.contains("icons")) {
            processIconsFromConfig(itemConfig, player, data)
        } else {
            null
        }
        
        // 创建物品
        return itemBuilder.createItem(itemConfig, matchedIcon, player, data)
    }
    
    /**
     * 获取菜单项的动作
     * @param itemConfig 物品配置
     * @param player 玩家
     * @param data 数据上下文
     * @return 动作列表
     */
    fun getMenuItemActions(
        itemConfig: ConfigurationSection,
        player: Player,
        data: Map<String, Any>
    ): List<String> {
        // 处理子图标
        val matchedIcon = if (itemConfig.contains("icons")) {
            processIconsFromConfig(itemConfig, player, data)
        } else {
            null
        }
        
        // 解析动作
        return actionResolver.resolveActions(itemConfig, matchedIcon, player, data)
    }
    
    /**
     * 从配置中处理icons
     */
    private fun processIconsFromConfig(
        itemConfig: ConfigurationSection,
        player: Player,
        data: Map<String, Any>
    ): com.github.postyizhan.gui.icon.IconConfig? {
        // 首先尝试作为列表处理
        val iconsList = itemConfig.getList("icons")
        if (iconsList != null && iconsList.isNotEmpty()) {
            return processIconsList(iconsList, player, data)
        }

        // 然后尝试作为配置节处理
        val iconsSection = itemConfig.getConfigurationSection("icons")
        if (iconsSection != null) {
            return iconProcessor.processIcons(iconsSection, player, data)
        }

        return null
    }

    /**
     * 处理icons列表
     */
    private fun processIconsList(
        iconsList: List<*>,
        player: Player,
        data: Map<String, Any>
    ): com.github.postyizhan.gui.icon.IconConfig? {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] Processing icons list for player: ${player.name}, list size: ${iconsList.size}")
        }

        for ((index, iconItem) in iconsList.withIndex()) {
            @Suppress("UNCHECKED_CAST")
            val iconMap = iconItem as? Map<String, Any> ?: continue

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Processing icon ${index + 1}: condition=${iconMap["condition"]}, material=${iconMap["material"]}")
            }

            val iconConfig = com.github.postyizhan.gui.icon.IconConfig.fromMap(iconMap)

            // 检查条件
            val condition = iconConfig.condition

            if (condition == null) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] No condition specified, using this icon")
                }
                return iconConfig
            } else {
                val conditionResult = iconProcessor.getConditionManager().checkCondition(condition, player, data)

                if (conditionResult) {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("[DEBUG] Player ${player.name} matched icon condition: '$condition'")
                    }
                    return iconConfig
                } else {
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("[DEBUG] Player ${player.name} did not match icon condition: '$condition'")
                    }
                }
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] No matching icon found for player ${player.name}, using default")
        }
        return null
    }

    /**
     * 检查显示条件（已废弃，现在使用子图标功能）
     */
    private fun checkDisplayCondition(
        @Suppress("UNUSED_PARAMETER") itemConfig: ConfigurationSection,
        @Suppress("UNUSED_PARAMETER") data: Map<String, Any>
    ): Boolean {
        // 不再处理display_condition，直接返回true
        // 显示条件功能已由子图标功能替代
        return true
    }
    
    /**
     * 创建备用物品（已废弃，现在使用子图标功能）
     */
    private fun createFallbackItem(@Suppress("UNUSED_PARAMETER") itemConfig: ConfigurationSection): ItemStack? {
        // 不再创建备用物品，直接返回null
        // 备用物品功能已由子图标功能替代
        return null
    }
    
    /**
     * 获取图标处理器
     */
    fun getIconProcessor(): IconProcessor {
        return iconProcessor
    }
    
    /**
     * 获取物品构建器
     */
    fun getItemBuilder(): ItemBuilder {
        return itemBuilder
    }
    
    /**
     * 获取动作解析器
     */
    fun getActionResolver(): ActionResolver {
        return actionResolver
    }
}
