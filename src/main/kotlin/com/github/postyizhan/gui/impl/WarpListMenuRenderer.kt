package com.github.postyizhan.gui.impl

import com.github.postyizhan.gui.core.MenuRenderer
import com.github.postyizhan.gui.core.MenuContext
import com.github.postyizhan.gui.processor.MenuItemProcessor
import com.github.postyizhan.gui.util.PlaceholderProcessor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.inventory.Inventory

/**
 * 地标列表菜单渲染器
 * 专门处理带分页功能的地标列表菜单
 */
class WarpListMenuRenderer : MenuRenderer {
    
    override fun render(context: MenuContext): Inventory? {
        val layout = context.getLayout()
        val menuData = context.menuData
        
        if (layout.isEmpty() || menuData == null) {
            return null
        }
        
        try {
            // 创建库存
            val rows = layout.size
            val title = processTitle(context)
            val inventory = Bukkit.createInventory(null, rows * 9, title)
            
            // 创建菜单项处理器
            val menuItemProcessor = MenuItemProcessor(context.plugin)
            val itemsConfig = context.getItemsConfig()

            // 合并MenuData的dynamicData到playerData中，用于条件检查
            val combinedData = context.playerData.toMutableMap()
            menuData.dynamicData.forEach { (key, value) ->
                combinedData[key] = value
            }

            // 计算分页信息
            val currentPage = context.getCurrentPage()
            val warpsPerPage = countWarpSlots(layout)
            val startIndex = currentPage * warpsPerPage

            // 用于追踪当前显示的地标索引
            var currentWarpIndex = startIndex
            
            // 填充物品
            layout.forEachIndexed { row, layoutLine ->
                layoutLine.forEachIndexed { col, symbol ->
                    if (symbol != ' ') {
                        val itemConfig = itemsConfig?.getConfigurationSection(symbol.toString())
                        if (itemConfig != null) {
                            val item = if (itemConfig.getBoolean("warp_item", false)) {
                                // 这是地标物品槽位
                                if (currentWarpIndex < menuData.warps.size) {
                                    val warp = menuData.warps[currentWarpIndex]

                                    // 存储地标ID到玩家数据中，用于点击处理
                                    val slotKey = "warp_id_${row}_${col}"
                                    context.setPlayerData(slotKey, warp.id)

                                    currentWarpIndex++
                                    menuItemProcessor.createWarpMenuItem(itemConfig, context.player, warp)
                                } else {
                                    // 没有更多地标，不显示物品
                                    null
                                }
                            } else {
                                // 普通菜单按钮
                                menuItemProcessor.createMenuItem(itemConfig, context.player, combinedData)
                            }
                            
                            if (item != null) {
                                inventory.setItem(row * 9 + col, item)
                            }
                        }
                    }
                }
            }
            
            return inventory
            
        } catch (e: Exception) {
            context.plugin.logger.warning("渲染地标列表菜单 ${context.menuName} 时发生错误: ${e.message}")
            if (context.plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
            return null
        }
    }
    
    override fun supports(menuType: String): Boolean {
        return menuType in listOf("private_warps", "public_warps")
    }
    
    /**
     * 处理菜单标题
     */
    private fun processTitle(context: MenuContext): String {
        val rawTitle = context.getTitle()
        val processedTitle = PlaceholderProcessor.processPlaceholders(rawTitle, context)
        return ChatColor.translateAlternateColorCodes('&', processedTitle)
    }
    
    /**
     * 计算布局中地标槽位的数量
     */
    private fun countWarpSlots(layout: List<String>): Int {
        var count = 0
        for (line in layout) {
            for (char in line) {
                if (char == 'W') { // 假设 'W' 是地标物品的符号
                    count++
                }
            }
        }
        return count
    }
}
