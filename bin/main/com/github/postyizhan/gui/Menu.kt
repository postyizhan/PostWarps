package com.github.postyizhan.gui

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * 菜单类，负责创建和管理菜单
 */
class Menu(
    private val plugin: PostWarps,
    val name: String,
    private val config: YamlConfiguration
) {
    // 菜单标题
    private val title: String = config.getString("title", "&8【 &b菜单 &8】").let { 
        ChatColor.translateAlternateColorCodes('&', it)
    }
    
    // 菜单布局
    private val layout: List<String> = config.getStringList("layout")
    
    // 菜单项配置
    private val items: ConfigurationSection? = config.getConfigurationSection("items")
    
    /**
     * 创建库存
     */
    fun createInventory(player: Player, data: Map<String, Any>): Inventory? {
        if (layout.isEmpty()) {
            return null
        }
        
        // 创建库存
        val rows = layout.size
        
        // 获取当前页码
        val currentPage = data["page"] as? Int ?: 0
        
        // 准备地标数据（如果是地标菜单）
        val warps = when (name) {
            "private_warps" -> {
                // 获取玩家的地标
                plugin.getDatabaseManager().getPlayerWarps(player.uniqueId)
            }
            "public_warps" -> {
                // 获取公开地标
                plugin.getDatabaseManager().getAllPublicWarps()
            }
            else -> null
        }
        
        // 每页显示的地标数量
        val warpsPerPage = 12
        
        // 计算分页
        val totalPages = if (warps?.isEmpty() != false) 1 else ((warps.size - 1) / warpsPerPage + 1)
        val startIndex = currentPage * warpsPerPage
        val endIndex = minOf(startIndex + warpsPerPage, warps?.size ?: 0)
        
        // 存储分页相关数据
        (data as? MutableMap<String, Any>)?.apply {
            this["total_pages"] = totalPages
            this["current_page"] = currentPage + 1 // 显示给用户从1开始
            this["has_next"] = currentPage < totalPages - 1 && totalPages > 0
            this["has_prev"] = currentPage > 0
        }
        
        // 处理标题中的占位符
        var processedTitle = title
        if (name == "private_warps" || name == "public_warps") {
            processedTitle = processedTitle
                .replace("{current_page}", "${currentPage + 1}") // 页码从1开始显示
                .replace("{total_pages}", "$totalPages")
        }
        
        val inventory = Bukkit.createInventory(null, rows * 9, processedTitle)
        
        // 用于追踪当前显示的地标索引
        var currentWarpIndex = startIndex
        
        // 填充物品
        layout.forEachIndexed { row, layoutLine ->
            layoutLine.forEachIndexed { col, symbol ->
                if (symbol != ' ') {
                    // 获取物品配置
                    val itemConfig = items?.getConfigurationSection(symbol.toString())
                    
                    // 检查是否是地标物品
                    if (itemConfig != null && itemConfig.getBoolean("warp_item", false)) {
                        // 检查是否有对应的地标
                        if (warps != null && currentWarpIndex < warps.size && currentWarpIndex < endIndex) {
                            val warp = warps[currentWarpIndex]
                            
                            // 创建地标物品
                            createWarpItem(symbol.toString(), player, data, warp)?.let { item ->
                                inventory.setItem(row * 9 + col, item)
                                
                                // 存储该位置对应的地标ID
                                (data as? MutableMap<String, Any>)?.put("warp_id_${row}_${col}", warp.id)
                            }
                            
                            // 记录第一个地标ID作为默认值
                            if (currentWarpIndex == startIndex) {
                                (data as? MutableMap<String, Any>)?.put("warp_id", warp.id)
                                plugin.logger.info("设置当前地标ID: ${warp.id}, 名称: ${warp.name}")
                            }
                            
                            // 移动到下一个地标
                            currentWarpIndex++
                        }
                    } else {
                        // 普通菜单按钮
                        val item = createItem(symbol.toString(), player, data)
                        if (item != null) {
                            inventory.setItem(row * 9 + col, item)
                        }
                    }
                }
            }
        }
        
        return inventory
    }
    
    /**
     * 创建物品
     */
    private fun createItem(symbol: String, player: Player, data: Map<String, Any>): ItemStack? {
        val itemConfig = items?.getConfigurationSection(symbol) ?: return null
        
        // 检查显示条件
        val displayCondition = itemConfig.getString("display_condition")
        if (displayCondition != null) {
            // 根据条件判断是否显示物品
            val conditionValue = data[displayCondition] as? Boolean ?: false
            if (!conditionValue) {
                // 条件不满足，返回一个占位物品（如果配置了material_if_false）或者返回null
                val materialIfFalse = itemConfig.getString("material_if_false")
                if (materialIfFalse != null) {
                    try {
                        val material = Material.valueOf(materialIfFalse.uppercase())
                        val item = ItemStack(material, 1)
                        val meta = item.itemMeta
                        if (meta != null) {
                            meta.setDisplayName(" ")
                            item.itemMeta = meta
                        }
                        return item
                    } catch (e: IllegalArgumentException) {
                        return null
                    }
                } else {
                    return null
                }
            }
        }
        
        // 获取材料
        val materialName = itemConfig.getString("material") ?: return null
        
        // 根据条件判断是否使用不同的材料
        val finalMaterialName = when {
            data.containsKey("has_next") && data["has_next"] == true && 
                itemConfig.getString("display_condition") == "has_next" && 
                itemConfig.contains("material_if_true") ->
                    itemConfig.getString("material_if_true") ?: materialName
                    
            data.containsKey("has_prev") && data["has_prev"] == true && 
                itemConfig.getString("display_condition") == "has_prev" && 
                itemConfig.contains("material_if_true") ->
                    itemConfig.getString("material_if_true") ?: materialName
                    
            data.containsKey("public") && data["public"] == true && 
                itemConfig.contains("material_if_true") ->
                    itemConfig.getString("material_if_true") ?: materialName
                    
            data.containsKey("public") && data["public"] == false && 
                itemConfig.contains("material_if_false") ->
                    itemConfig.getString("material_if_false") ?: materialName
                    
            else -> materialName
        }
        
        val material = try {
            Material.valueOf(finalMaterialName.uppercase())
        } catch (e: IllegalArgumentException) {
            plugin.logger.warning("未知的材料类型: $finalMaterialName")
            Material.STONE
        }
        
        // 创建物品
        val item = ItemStack(material, 1)
        val meta = item.itemMeta ?: return item
        
        // 设置名称
        val displayName = itemConfig.getString("name")?.let {
            processPlaceholders(it, player, data)
        }
        if (displayName != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName))
        }
        
        // 设置Lore
        val lore = itemConfig.getStringList("lore")
        if (lore.isNotEmpty()) {
            val processedLore = lore.map { 
                ChatColor.translateAlternateColorCodes('&', processPlaceholders(it, player, data))
            }
            meta.lore = processedLore
        }
        
        item.itemMeta = meta
        return item
    }
    
    /**
     * 创建地标物品
     */
    private fun createWarpItem(symbol: String, player: Player, data: Map<String, Any>, warp: com.github.postyizhan.model.Warp): org.bukkit.inventory.ItemStack? {
        val itemConfig = items?.getConfigurationSection(symbol) ?: return null
        
        // 获取物品材质
        val material = try {
            org.bukkit.Material.valueOf(itemConfig.getString("material", "STONE")!!.uppercase())
        } catch (e: IllegalArgumentException) {
            org.bukkit.Material.STONE
        }
        
        // 创建物品
        val item = org.bukkit.inventory.ItemStack(material)
        val meta = item.itemMeta ?: return item
        
        // 设置名称
        val name = itemConfig.getString("name")
        if (name != null) {
            meta.setDisplayName(MessageUtil.color(replacePlaceholders(name, player, data, warp)))
        }
        
        // 设置描述
        val lore = itemConfig.getStringList("lore")
        if (lore.isNotEmpty()) {
            meta.lore = lore.map { MessageUtil.color(replacePlaceholders(it, player, data, warp)) }
        }
        
        // 设置自定义NBT标签（如果Bukkit API支持的话）
        // 注意：如果服务器不支持此功能，可以移除这部分代码
        try {
            // 将地标ID存储在物品的显示名称中
            // 这种方式不建议用于生产环境，但作为临时解决方案可以使用
            val currentDisplayName = meta.displayName ?: ""
            meta.setDisplayName("$currentDisplayName§r§0§${warp.id}")
        } catch (e: Exception) {
            plugin.logger.warning("无法设置物品的NBT标签: ${e.message}")
        }
        
        item.itemMeta = meta
        return item
    }
    
    /**
     * 替换地标特定的占位符
     */
    private fun replacePlaceholders(text: String, player: Player, data: Map<String, Any>, warp: com.github.postyizhan.model.Warp): String {
        var result = text
            .replace("{name}", warp.name)
            .replace("{owner}", warp.ownerName)
            .replace("{world}", warp.worldName)
            .replace("{coords}", warp.getFormattedCoordinates())
            
        // 处理公开/私有状态
        val publicState = if (warp.isPublic) "&a公开" else "&c私有"
        result = result.replace("{public_state}", publicState)
            
        // 替换数据占位符
        for ((key, value) in data) {
            result = result.replace("{$key}", value.toString())
        }
        
        // 替换玩家占位符
        result = result.replace("{player}", player.name)
            
        return result
    }
    
    /**
     * 处理占位符
     */
    private fun processPlaceholders(text: String, player: Player, data: Map<String, Any>): String {
        var result = text
        
        // 处理数据占位符
        for ((key, value) in data) {
            result = result.replace("{$key}", value.toString())
        }
        
        // 特殊占位符处理
        result = result.replace("{name}", data["name"]?.toString() ?: "")
        result = result.replace("{desc}", data["desc"]?.toString() ?: "")
        
        // 公开/私有状态显示
        val isPublic = data["public"] as? Boolean ?: false
        val publicState = if (isPublic) "&a公开" else "&c私有"
        result = result.replace("{public_state}", publicState)
        
        return result
    }
    
    /**
     * 处理点击事件
     */
    fun handleClick(player: Player, slot: Int, data: Map<String, Any>): List<String> {
        // 找出对应的物品符号
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
        val itemConfig = items?.getConfigurationSection(symbol) ?: return emptyList()
        
        // 检查是否是地标物品
        if (itemConfig.getBoolean("warp_item", false)) {
            // 获取点击位置对应的地标ID
            val slotKey = "warp_id_${row}_${col}"
            
            // 检查该位置是否有地标数据
            if (!data.containsKey(slotKey)) {
                // 这是一个空的地标槽位，没有实际的地标物品
                return emptyList()
            }
            
            val warpId = data[slotKey] as? Int
            
            if (warpId != null) {
                // 更新当前选中的地标ID
                (data as? MutableMap<String, Any>)?.put("warp_id", warpId)
                
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] Player ${player.name} clicked warp slot: $row,$col, warp ID: $warpId")
                }
                
                // 检查是否按住Shift键和左键点击
                val isShiftClick = data["is_shift_click"] as? Boolean ?: false
                val isLeftClick = data["is_left_click"] as? Boolean ?: true
                
                return if (isShiftClick && isLeftClick) {
                    // Shift+左键点击，打开设置菜单
                    plugin.logger.info("[DEBUG] Player ${player.name} shift+left clicked warp ID: $warpId, opening settings menu")
                    listOf("[menu] settings")
                } else {
                    // 普通点击，传送
                    listOf("[warp_tp]")
                }
            } else {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] Player ${player.name} clicked warp slot: $row,$col, but warp ID is null")
                }
                return emptyList()
            }
        }
        
        // 处理普通按钮
        return when {
            itemConfig.isList("action") -> itemConfig.getStringList("action")
            itemConfig.isString("action") -> listOf(itemConfig.getString("action") ?: "")
            else -> emptyList()
        }
    }
}
