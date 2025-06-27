package com.github.postyizhan.gui

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.processor.MenuItemProcessor
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
open class Menu(
    protected val plugin: PostWarps,
    val name: String,
    protected val config: YamlConfiguration
) {
    // 获取本地化的菜单标题
    private fun getLocalizedTitle(player: Player): String {
        val rawTitle = i18nProcessor.getLocalizedTitle(config, player)
            ?: config.getString("title", "&8【 &b菜单 &8】")
        return ChatColor.translateAlternateColorCodes('&', rawTitle)
    }

    // 菜单布局
    private val layout: List<String> = config.getStringList("layout")

    // 菜单项配置
    private val items: ConfigurationSection? = config.getConfigurationSection("items")

    // 菜单项处理器
    private val menuItemProcessor = MenuItemProcessor(plugin)

    // 国际化处理器
    private val i18nProcessor = com.github.postyizhan.gui.util.MenuI18nProcessor(plugin)
    
    /**
     * 创建库存
     */
    open fun createInventory(player: Player, data: Map<String, Any>): Inventory? {
        if (layout.isEmpty()) {
            return null
        }
        
        // 创建库存
        val rows = layout.size
        
        // 获取当前页码
        val currentPage = data["page"] as? Int ?: 0
        
        // 准备地标数据（如果是地标菜单）
        val allWarps = when (name) {
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

        // 应用搜索过滤器
        val searchFilter = data["search_filter"] as? String
        val warps = if (allWarps != null && !searchFilter.isNullOrEmpty()) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: Menu search - Filter: '$searchFilter', Total warps: ${allWarps.size}")
            }

            val filtered = when (name) {
                "private_warps" -> {
                    allWarps.filter { warp ->
                        warp.name.contains(searchFilter, ignoreCase = true) ||
                        warp.description.contains(searchFilter, ignoreCase = true) ||
                        warp.worldName.contains(searchFilter, ignoreCase = true)
                    }
                }
                "public_warps" -> {
                    allWarps.filter { warp ->
                        warp.name.contains(searchFilter, ignoreCase = true) ||
                        warp.description.contains(searchFilter, ignoreCase = true) ||
                        warp.worldName.contains(searchFilter, ignoreCase = true) ||
                        warp.ownerName.contains(searchFilter, ignoreCase = true)
                    }
                }
                else -> allWarps
            }

            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: Menu search - Filtered warps: ${filtered.size}")
            }
            filtered
        } else {
            allWarps
        }
        
        // 每页显示的地标数量
        val warpsPerPage = 12
        
        // 计算分页
        val totalPages = if (warps?.isEmpty() != false) 1 else ((warps.size - 1) / warpsPerPage + 1)
        val startIndex = currentPage * warpsPerPage
        val endIndex = minOf(startIndex + warpsPerPage, warps?.size ?: 0)
        
        // 存储分页相关数据和搜索相关数据
        (data as? MutableMap<String, Any>)?.apply {
            this["total_pages"] = totalPages
            this["current_page"] = currentPage + 1 // 显示给用户从1开始
            this["has_next"] = currentPage < totalPages - 1 && totalPages > 0
            this["has_prev"] = currentPage > 0

            // 搜索相关数据
            this["warp_count"] = warps?.size ?: 0
            this["total_warp_count"] = allWarps?.size ?: 0
            this["has_warps"] = (warps?.size ?: 0) > 0
            this["is_searching"] = !searchFilter.isNullOrEmpty()
            this["search_keyword"] = data["search_display"] as? String ?: searchFilter ?: ""
        }
        
        // 获取本地化标题并处理占位符
        var processedTitle = getLocalizedTitle(player)
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
                            
                            // 记录第一个地标ID作为默认值（只在相关菜单中设置）
                            if (currentWarpIndex == startIndex && shouldSetDefaultWarpId(warp)) {
                                (data as? MutableMap<String, Any>)?.put("warp_id", warp.id)
                                if (plugin.isDebugEnabled()) {
                                    plugin.logger.info("[DEBUG] Set default warp_id: ${warp.id} for menu: $name")
                                }
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
        return menuItemProcessor.createMenuItem(itemConfig, player, data)
    }



    /**
     * 创建地标物品（支持国际化）
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

        // 设置名称（支持国际化）
        val name = getLocalizedItemName(itemConfig, player)
        if (name != null) {
            meta.setDisplayName(MessageUtil.color(replacePlaceholders(name, player, data, warp)))
        }

        // 设置描述（支持国际化）
        val lore = getLocalizedItemLore(itemConfig, player)
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
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Failed to set item NBT tags: ${e.message}")
            }
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
            .replace("{desc}", warp.description)
            
        // 处理公开/私有状态（国际化）
        val publicState = if (warp.isPublic) {
            MessageUtil.getMessage("status.public", player)
        } else {
            MessageUtil.getMessage("status.private", player)
        }
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
    private fun processPlaceholders(text: String, @Suppress("UNUSED_PARAMETER") player: Player, data: Map<String, Any>): String {
        var result = text
        
        // 处理数据占位符
        for ((key, value) in data) {
            result = result.replace("{$key}", value.toString())
        }
        
        // 特殊占位符处理
        result = result.replace("{name}", data["name"]?.toString() ?: "")
        result = result.replace("{desc}", data["desc"]?.toString() ?: "")
        
        // 公开/私有状态显示（国际化）
        val isPublic = data["is_public"] as? Boolean ?: false
        val publicState = if (isPublic) {
            MessageUtil.getMessage("status.public", player)
        } else {
            MessageUtil.getMessage("status.private", player)
        }
        result = result.replace("{public_state}", publicState)
        
        return result
    }
    
    /**
     * 处理点击事件
     */
    open fun handleClick(player: Player, slot: Int, data: Map<String, Any>): List<String> {
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
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] Player ${player.name} clicked empty warp slot: $row,$col, ignoring click")
                }
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
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("[DEBUG] Player ${player.name} shift+left clicked warp ID: $warpId, opening settings menu")
                    }
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
        
        // 处理普通按钮 - 支持子图标动作和点击类型
        val actions = menuItemProcessor.getMenuItemActions(itemConfig, player, data)

        // 特殊处理搜索按钮的右键点击
        if (symbol == "S" && !(data["is_left_click"] as? Boolean ?: true)) {
            // 右键点击搜索按钮，执行清除搜索
            return listOf("[warp_search_clear]")
        }

        return actions
    }

    /**
     * 获取本地化的物品名称
     */
    private fun getLocalizedItemName(itemConfig: ConfigurationSection, player: Player): String? {
        // 获取玩家语言
        val language = MessageUtil.getPlayerLanguage(player)

        // 尝试从i18n配置获取
        val i18nConfig = itemConfig.getConfigurationSection("i18n")
        if (i18nConfig != null) {
            val langConfig = i18nConfig.getConfigurationSection(language)
            if (langConfig != null) {
                val i18nName = langConfig.getString("name")
                if (i18nName != null) {
                    return i18nName
                }
            }
        }

        // 回退到默认名称
        return itemConfig.getString("name")
    }

    /**
     * 获取本地化的物品描述
     */
    private fun getLocalizedItemLore(itemConfig: ConfigurationSection, player: Player): List<String> {
        // 获取玩家语言
        val language = MessageUtil.getPlayerLanguage(player)

        // 尝试从i18n配置获取
        val i18nConfig = itemConfig.getConfigurationSection("i18n")
        if (i18nConfig != null) {
            val langConfig = i18nConfig.getConfigurationSection(language)
            if (langConfig != null) {
                val i18nLore = langConfig.getStringList("lore")
                if (i18nLore.isNotEmpty()) {
                    return i18nLore
                }
            }
        }

        // 回退到默认描述
        return itemConfig.getStringList("lore")
    }

    /**
     * 判断是否应该设置默认的warp_id
     * 只有在相关菜单中才设置，避免跨菜单污染
     */
    private fun shouldSetDefaultWarpId(warp: com.github.postyizhan.model.Warp): Boolean {
        return when (name) {
            "private_warps" -> !warp.isPublic  // 私有地标菜单只设置私有地标
            "public_warps" -> warp.isPublic    // 公开地标菜单只设置公开地标
            else -> true  // 其他菜单（如设置菜单）可以设置任何地标
        }
    }


}
