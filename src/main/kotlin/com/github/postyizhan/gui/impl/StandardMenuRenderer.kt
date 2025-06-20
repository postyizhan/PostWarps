package com.github.postyizhan.gui.impl

import com.github.postyizhan.gui.core.MenuRenderer
import com.github.postyizhan.gui.core.MenuContext
import com.github.postyizhan.gui.util.ItemBuilder
import com.github.postyizhan.gui.util.PlaceholderProcessor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.inventory.Inventory

/**
 * 标准菜单渲染器
 * 处理大部分常规菜单的渲染
 */
class StandardMenuRenderer : MenuRenderer {
    
    override fun render(context: MenuContext): Inventory? {
        val layout = context.getLayout()
        if (layout.isEmpty()) {
            return null
        }
        
        try {
            // 创建库存
            val rows = layout.size
            val title = processTitle(context)
            val inventory = Bukkit.createInventory(null, rows * 9, title)
            
            // 创建物品构建器
            val itemBuilder = ItemBuilder(context)
            val itemsConfig = context.getItemsConfig()
            
            // 填充物品
            layout.forEachIndexed { row, layoutLine ->
                layoutLine.forEachIndexed { col, symbol ->
                    if (symbol != ' ') {
                        val itemConfig = itemsConfig?.getConfigurationSection(symbol.toString())
                        if (itemConfig != null) {
                            val item = itemBuilder.createItem(symbol.toString(), itemConfig)
                            if (item != null) {
                                inventory.setItem(row * 9 + col, item)
                            }
                        }
                    }
                }
            }
            
            return inventory
            
        } catch (e: Exception) {
            context.plugin.logger.warning("渲染菜单 ${context.menuName} 时发生错误: ${e.message}")
            if (context.plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
            return null
        }
    }
    
    override fun supports(menuType: String): Boolean {
        // 标准渲染器支持大部分菜单类型
        return menuType in listOf("main", "create", "settings")
    }
    
    /**
     * 处理菜单标题
     */
    private fun processTitle(context: MenuContext): String {
        val rawTitle = context.getTitle()
        val processedTitle = PlaceholderProcessor.processPlaceholders(rawTitle, context)
        return ChatColor.translateAlternateColorCodes('&', processedTitle)
    }
}
