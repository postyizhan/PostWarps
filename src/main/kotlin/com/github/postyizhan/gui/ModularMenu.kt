package com.github.postyizhan.gui

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.core.MenuRenderer
import com.github.postyizhan.gui.core.MenuDataProvider
import com.github.postyizhan.gui.core.MenuContext
import com.github.postyizhan.gui.impl.*
import com.github.postyizhan.gui.util.MenuCache
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * 模块化菜单实现
 * 使用新的模块化架构，支持不同类型的菜单
 */
class ModularMenu(
    val plugin: PostWarps,
    val name: String,
    val config: YamlConfiguration,
    private val cache: MenuCache
) {
    
    // 渲染器列表
    private val renderers = listOf(
        StandardMenuRenderer(),
        WarpListMenuRenderer()
    )
    
    // 数据提供器列表
    private val dataProviders = listOf(
        WarpMenuDataProvider(plugin, cache),
        StaticMenuDataProvider()
    )
    
    /**
     * 创建菜单库存
     */
    fun createInventory(player: Player, playerData: MutableMap<String, Any>): Inventory? {
        try {
            // 1. 创建菜单上下文
            val context = MenuContext(plugin, player, name, config, playerData)

            // 2. 获取数据提供器
            val dataProvider = getDataProvider()

            // 3. 获取菜单数据（如果需要）
            val menuData = if (dataProvider != null) {
                try {
                    kotlinx.coroutines.runBlocking {
                        dataProvider.getData(player, name, context)
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("获取菜单数据时发生错误: ${e.message}")
                    null
                }
            } else null

            // 4. 更新上下文
            val updatedContext = context.copy(menuData = menuData)

            // 5. 获取渲染器并渲染
            val renderer = getRenderer()
            return renderer.render(updatedContext)

        } catch (e: Exception) {
            plugin.logger.warning("创建菜单 $name 时发生错误: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
            return null
        }
    }

    private fun getRenderer(): MenuRenderer {
        return renderers.find { it.supports(name) }
            ?: renderers.first() // 默认使用标准渲染器
    }

    private fun getDataProvider(): MenuDataProvider? {
        return dataProviders.find { it.supports(name) }
    }
    
    fun handleClick(player: Player, slot: Int, playerData: MutableMap<String, Any>): List<String> {
        // 找出对应的物品符号
        val layout = config.getStringList("layout")
        val row = slot / 9
        val col = slot % 9
        
        if (row < 0 || row >= layout.size || col < 0 || col >= layout[row].length) {
            return emptyList()
        }
        
        val symbol = layout[row][col].toString()
        if (symbol == " ") {
            return emptyList()
        }
        
        // 获取物品配置
        val itemsConfig = config.getConfigurationSection("items")
        val itemConfig = itemsConfig?.getConfigurationSection(symbol) ?: return emptyList()
        
        // 检查是否是地标物品
        if (itemConfig.getBoolean("warp_item", false)) {
            return handleWarpItemClick(player, row, col, playerData)
        }
        
        // 处理普通按钮
        return when {
            itemConfig.isList("action") -> itemConfig.getStringList("action")
            itemConfig.isString("action") -> listOf(itemConfig.getString("action") ?: "")
            else -> emptyList()
        }
    }
    
    /**
     * 处理地标物品点击
     */
    private fun handleWarpItemClick(player: Player, row: Int, col: Int, playerData: MutableMap<String, Any>): List<String> {
        // 获取点击位置对应的地标ID
        val slotKey = "warp_id_${row}_${col}"
        
        // 检查该位置是否有地标数据
        if (!playerData.containsKey(slotKey)) {
            return emptyList()
        }
        
        val warpId = playerData[slotKey] as? Int ?: return emptyList()
        
        // 更新当前选中的地标ID
        playerData["warp_id"] = warpId
        
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] Player ${player.name} clicked warp slot: $row,$col, warp ID: $warpId")
        }
        
        // 检查点击类型
        val isShiftClick = playerData["is_shift_click"] as? Boolean ?: false
        val isLeftClick = playerData["is_left_click"] as? Boolean ?: true
        
        return if (isShiftClick && isLeftClick) {
            // Shift+左键点击，打开设置菜单
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Player ${player.name} shift+left clicked warp ID: $warpId, opening settings menu")
            }
            listOf("[menu] settings")
        } else {
            // 普通点击，传送
            listOf("[warp_tp]")
        }
    }
}
