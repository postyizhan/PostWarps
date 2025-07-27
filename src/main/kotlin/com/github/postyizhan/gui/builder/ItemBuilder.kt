package com.github.postyizhan.gui.builder

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.icon.IconConfig
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.MessageUtil
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

    // 国际化处理器
    private val i18nProcessor = com.github.postyizhan.gui.util.MenuI18nProcessor(plugin)
    
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
     * 创建地标物品（支持地标占位符）
     * @param mainConfig 主配置
     * @param iconConfig 子图标配置（可为null）
     * @param player 玩家
     * @param warp 地标对象
     * @return 创建的物品，如果失败返回null
     */
    fun createWarpItem(
        mainConfig: ConfigurationSection,
        iconConfig: IconConfig?,
        player: Player,
        warp: Warp
    ): ItemStack? {
        // 创建基础数据，包含地标信息
        val data = mutableMapOf<String, Any>(
            "name" to warp.name,
            "desc" to warp.description,
            "owner" to warp.owner,
            "world" to warp.worldName,
            "coords" to "${warp.x.toInt()}, ${warp.y.toInt()}, ${warp.z.toInt()}",
            "is_public" to warp.isPublic,
            "warp_id" to warp.id
        )

        // 创建物品
        val item = createItem(mainConfig, iconConfig, player, data) ?: return null

        // 存储地标ID到物品中（用于点击识别）
        val meta = item.itemMeta
        if (meta != null) {
            storeWarpId(meta, warp)
            item.itemMeta = meta
        }

        return item
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
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Failed to set warp ID on item: ${e.message}")
            }
        }
    }

    /**
     * 处理条件材料（已废弃，现在使用子图标功能）
     */
    private fun processConditionalMaterial(
        materialName: String,
        @Suppress("UNUSED_PARAMETER") mainConfig: ConfigurationSection,
        @Suppress("UNUSED_PARAMETER") data: Map<String, Any>
    ): String {
        // 不再处理条件材料，直接返回原材料名称
        // 条件材料功能已由子图标功能替代
        return materialName
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
        // 优先使用子图标的名称，然后是本地化名称，最后是默认名称
        val displayName = iconConfig?.name ?: i18nProcessor.getLocalizedName(mainConfig, player)
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
        // 优先使用子图标的lore，然后是本地化lore，最后是默认lore
        val lore = iconConfig?.lore ?: i18nProcessor.getLocalizedLore(mainConfig, player) ?: emptyList()
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
}
