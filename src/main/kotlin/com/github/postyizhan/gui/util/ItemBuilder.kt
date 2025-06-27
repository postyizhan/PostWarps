package com.github.postyizhan.gui.util

import com.github.postyizhan.gui.core.MenuContext
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.MessageUtil
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * 物品构建器
 * 负责根据配置创建菜单物品
 */
class ItemBuilder(private val context: MenuContext) {

    // 国际化处理器
    private val i18nProcessor = MenuI18nProcessor(context.plugin)
    
    /**
     * 创建普通菜单物品
     * @param symbol 物品符号
     * @param itemConfig 物品配置
     * @return 创建的物品，失败返回null
     */
    fun createItem(symbol: String, itemConfig: ConfigurationSection): ItemStack? {
        try {
            // 检查显示条件
            if (!checkDisplayCondition(itemConfig)) {
                return null
            }
            
            // 获取材质
            val material = getMaterial(itemConfig)
            
            // 创建物品
            val item = ItemStack(material, 1)
            val meta = item.itemMeta ?: return item
            
            // 设置显示名称
            setDisplayName(meta, itemConfig)
            
            // 设置描述
            setLore(meta, itemConfig)
            
            item.itemMeta = meta
            return item
            
        } catch (e: Exception) {
            context.plugin.logger.warning("创建物品 $symbol 时发生错误: ${e.message}")
            return null
        }
    }
    
    /**
     * 创建地标物品
     * @param symbol 物品符号
     * @param itemConfig 物品配置
     * @param warp 地标对象
     * @return 创建的物品，失败返回null
     */
    fun createWarpItem(symbol: String, itemConfig: ConfigurationSection, warp: Warp): ItemStack? {
        try {
            // 获取材质
            val material = getMaterial(itemConfig)
            
            // 创建物品
            val item = ItemStack(material, 1)
            val meta = item.itemMeta ?: return item
            
            // 设置显示名称（使用地标占位符）
            setWarpDisplayName(meta, itemConfig, warp)
            
            // 设置描述（使用地标占位符）
            setWarpLore(meta, itemConfig, warp)
            
            // 存储地标ID到物品中（用于点击识别）
            storeWarpId(meta, warp)
            
            item.itemMeta = meta
            return item
            
        } catch (e: Exception) {
            context.plugin.logger.warning("创建地标物品 $symbol 时发生错误: ${e.message}")
            return null
        }
    }
    
    /**
     * 检查显示条件（兼容旧版本，建议使用子图标功能）
     */
    private fun checkDisplayCondition(itemConfig: ConfigurationSection): Boolean {
        val condition = itemConfig.getString("display_condition") ?: return true

        return when (condition) {
            "has_prev" -> {
                val currentPage = context.getCurrentPage()
                currentPage > 0
            }
            "has_next" -> {
                val menuData = context.menuData
                val currentPage = context.getCurrentPage()
                menuData != null && currentPage < menuData.totalPages - 1
            }
            else -> true
        }
    }
    
    /**
     * 获取材质
     */
    private fun getMaterial(itemConfig: ConfigurationSection): Material {
        val materialName = itemConfig.getString("material", "STONE")!!

        // 不再处理条件材质，条件材质功能已由子图标功能替代
        return try {
            Material.valueOf(materialName.uppercase())
        } catch (e: IllegalArgumentException) {
            context.plugin.logger.warning("未知的材料类型: $materialName")
            Material.STONE
        }
    }
    
    /**
     * 设置显示名称
     */
    private fun setDisplayName(meta: ItemMeta, itemConfig: ConfigurationSection) {
        // 优先使用本地化名称
        val rawName = i18nProcessor.getLocalizedName(itemConfig, context.player)
        val displayName = rawName?.let {
            PlaceholderProcessor.processPlaceholders(it, context)
        }
        if (displayName != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName))
        }
    }
    
    /**
     * 设置地标显示名称
     */
    private fun setWarpDisplayName(meta: ItemMeta, itemConfig: ConfigurationSection, warp: Warp) {
        // 优先使用本地化名称
        val rawName = i18nProcessor.getLocalizedName(itemConfig, context.player)
        val displayName = rawName?.let {
            PlaceholderProcessor.processWarpPlaceholders(it, context, warp)
        }
        if (displayName != null) {
            meta.setDisplayName(MessageUtil.color(displayName))
        }
    }
    
    /**
     * 设置描述
     */
    private fun setLore(meta: ItemMeta, itemConfig: ConfigurationSection) {
        // 优先使用本地化描述
        val rawLore = i18nProcessor.getLocalizedLore(itemConfig, context.player)
        if (rawLore != null && rawLore.isNotEmpty()) {
            meta.lore = rawLore.map {
                ChatColor.translateAlternateColorCodes('&',
                    PlaceholderProcessor.processPlaceholders(it, context))
            }
        }
    }
    
    /**
     * 设置地标描述
     */
    private fun setWarpLore(meta: ItemMeta, itemConfig: ConfigurationSection, warp: Warp) {
        // 优先使用本地化描述
        val rawLore = i18nProcessor.getLocalizedLore(itemConfig, context.player)
        if (rawLore != null && rawLore.isNotEmpty()) {
            meta.lore = rawLore.map {
                MessageUtil.color(PlaceholderProcessor.processWarpPlaceholders(it, context, warp))
            }
        }
    }
    
    /**
     * 存储地标ID到物品中
     */
    private fun storeWarpId(meta: ItemMeta, warp: Warp) {
        try {
            // 将地标ID存储在物品的显示名称中（临时方案）
            val currentDisplayName = meta.displayName ?: ""
            meta.setDisplayName("$currentDisplayName§r§0§${warp.id}")
        } catch (e: Exception) {
            context.plugin.logger.warning("无法设置物品的地标ID: ${e.message}")
        }
    }
}
