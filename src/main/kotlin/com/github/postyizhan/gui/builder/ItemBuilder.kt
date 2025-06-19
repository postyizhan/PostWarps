package com.github.postyizhan.gui.builder

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.icon.IconConfig
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * 物品构建器，支持继承机制
 */
class ItemBuilder(private val plugin: PostWarps) {
    
    /**
     * 从配置创建物品（支持子图标继承）
     * @param mainConfig 主配置
     * @param iconConfig 子图标配置（可为null）
     * @param player 玩家
     * @param data 数据上下文
     * @return 创建的物品，如果失败返回null
     */
    fun createItem(
        mainConfig: ConfigurationSection,
        iconConfig: IconConfig?,
        player: Player,
        data: Map<String, Any>
    ): ItemStack? {
        // 获取材料（子图标优先，然后是主配置）
        val materialName = iconConfig?.material 
            ?: mainConfig.getString("material") 
            ?: return null
        
        // 处理条件材料
        val finalMaterialName = processConditionalMaterial(materialName, mainConfig, data)
        
        val material = try {
            Material.valueOf(finalMaterialName.uppercase())
        } catch (e: IllegalArgumentException) {
            plugin.logger.warning("未知的材料类型: $finalMaterialName")
            Material.STONE
        }
        
        // 创建物品
        val amount = iconConfig?.amount ?: mainConfig.getInt("amount", 1)
        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item
        
        // 设置名称
        setItemName(meta, mainConfig, iconConfig, player, data)
        
        // 设置Lore
        setItemLore(meta, mainConfig, iconConfig, player, data)
        
        // 设置自定义模型数据
        setCustomModelData(meta, mainConfig, iconConfig)
        
        // 设置附魔效果
        setEnchantment(item, meta, mainConfig, iconConfig)
        
        item.itemMeta = meta
        return item
    }
    
    /**
     * 处理条件材料
     */
    private fun processConditionalMaterial(
        materialName: String,
        mainConfig: ConfigurationSection,
        data: Map<String, Any>
    ): String {
        return when {
            data.containsKey("has_next") && data["has_next"] == true && 
                mainConfig.getString("display_condition") == "has_next" && 
                mainConfig.contains("material_if_true") ->
                    mainConfig.getString("material_if_true") ?: materialName
                    
            data.containsKey("has_prev") && data["has_prev"] == true && 
                mainConfig.getString("display_condition") == "has_prev" && 
                mainConfig.contains("material_if_true") ->
                    mainConfig.getString("material_if_true") ?: materialName
                    
            data.containsKey("public") && data["public"] == true && 
                mainConfig.contains("material_if_true") ->
                    mainConfig.getString("material_if_true") ?: materialName
                    
            data.containsKey("public") && data["public"] == false && 
                mainConfig.contains("material_if_false") ->
                    mainConfig.getString("material_if_false") ?: materialName
                    
            else -> materialName
        }
    }
    
    /**
     * 设置物品名称
     */
    private fun setItemName(
        meta: ItemMeta,
        mainConfig: ConfigurationSection,
        iconConfig: IconConfig?,
        player: Player,
        data: Map<String, Any>
    ) {
        val displayName = iconConfig?.name ?: mainConfig.getString("name")
        if (displayName != null) {
            val processedName = processPlaceholders(displayName, player, data)
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', processedName))
        }
    }
    
    /**
     * 设置物品Lore
     */
    private fun setItemLore(
        meta: ItemMeta,
        mainConfig: ConfigurationSection,
        iconConfig: IconConfig?,
        player: Player,
        data: Map<String, Any>
    ) {
        val lore = iconConfig?.lore ?: mainConfig.getStringList("lore")
        if (lore.isNotEmpty()) {
            val processedLore = lore.map { 
                ChatColor.translateAlternateColorCodes('&', processPlaceholders(it, player, data))
            }
            meta.lore = processedLore
        }
    }
    
    /**
     * 设置自定义模型数据
     */
    private fun setCustomModelData(
        meta: ItemMeta,
        mainConfig: ConfigurationSection,
        iconConfig: IconConfig?
    ) {
        val customModelData = iconConfig?.customModelData
            ?: mainConfig.getInt("customModelData", -1).takeIf { it != -1 }
            ?: mainConfig.getInt("custom-model-data", -1).takeIf { it != -1 }

        if (customModelData != null) {
            try {
                // 使用反射来调用setCustomModelData方法，以兼容不同版本
                val method = meta.javaClass.getMethod("setCustomModelData", Int::class.java)
                method.invoke(meta, customModelData)
            } catch (e: Exception) {
                // 某些版本可能不支持自定义模型数据
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("[DEBUG] 当前版本不支持自定义模型数据: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 设置附魔效果
     */
    private fun setEnchantment(
        item: ItemStack,
        meta: ItemMeta,
        mainConfig: ConfigurationSection,
        iconConfig: IconConfig?
    ) {
        val enchanted = iconConfig?.enchanted ?: mainConfig.getBoolean("enchanted", false)
        if (enchanted) {
            // 添加一个不可见的附魔效果
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
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
        
        // 处理玩家占位符
        result = result.replace("{player}", player.name)
        
        // 特殊占位符处理
        result = result.replace("{name}", data["name"]?.toString() ?: "")
        result = result.replace("{desc}", data["desc"]?.toString() ?: "")
        
        // 公开/私有状态显示
        val isPublic = data["public"] as? Boolean ?: false
        val publicState = if (isPublic) "&a公开" else "&c私有"
        result = result.replace("{public_state}", publicState)
        
        return result
    }
}
