package com.github.postyizhan.gui.processor

import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.action.ActionResolver
import com.github.postyizhan.gui.builder.ItemBuilder
import com.github.postyizhan.gui.icon.IconProcessor
import com.github.postyizhan.model.Warp
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class MenuItemProcessor(private val plugin: PostWarps) {
    
    private val iconProcessor = IconProcessor(plugin)
    private val itemBuilder = ItemBuilder(plugin)
    private val actionResolver = ActionResolver(plugin)
    

    fun createMenuItem(
        itemConfig: ConfigurationSection,
        player: Player,
        data: Map<String, Any>
    ): ItemStack? {
        val matchedIcon = if (itemConfig.contains("icons")) {
            processIconsFromConfig(itemConfig, player, data)
        } else {
            null
        }
        
        return itemBuilder.createItem(itemConfig, matchedIcon, player, data)
    }


    fun createWarpMenuItem(
        itemConfig: ConfigurationSection,
        player: Player,
        warp: Warp
    ): ItemStack? {
        val data = mapOf(
            "name" to warp.name,
            "desc" to warp.description,
            "owner" to warp.ownerName,
            "world" to warp.worldName,
            "coords" to "${warp.x.toInt()}, ${warp.y.toInt()}, ${warp.z.toInt()}",
            "is_public" to warp.isPublic,
            "warp_id" to warp.id
        )

        val matchedIcon = if (itemConfig.contains("icons")) {
            processIconsFromConfig(itemConfig, player, data)
        } else {
            null
        }

        return itemBuilder.createWarpItem(itemConfig, matchedIcon, player, warp)
    }

    fun getMenuItemActions(
        itemConfig: ConfigurationSection,
        player: Player,
        data: Map<String, Any>
    ): List<String> {
        val matchedIcon = if (itemConfig.contains("icons")) {
            processIconsFromConfig(itemConfig, player, data)
        } else {
            null
        }
        
        return actionResolver.resolveActions(itemConfig, matchedIcon, player, data)
    }
    
    private fun processIconsFromConfig(
        itemConfig: ConfigurationSection,
        player: Player,
        data: Map<String, Any>
    ): com.github.postyizhan.gui.icon.IconConfig? {
        val iconsList = itemConfig.getList("icons")
        if (iconsList != null && iconsList.isNotEmpty()) {
            return processIconsList(iconsList, player, data)
        }

        val iconsSection = itemConfig.getConfigurationSection("icons")
        if (iconsSection != null) {
            return iconProcessor.processIcons(iconsSection, player, data)
        }

        return null
    }


    private fun processIconsList(
        iconsList: List<*>,
        player: Player,
        data: Map<String, Any>
    ): com.github.postyizhan.gui.icon.IconConfig? {
        for (iconItem in iconsList) {
            @Suppress("UNCHECKED_CAST")
            val iconMap = iconItem as? Map<String, Any> ?: continue
            val iconConfig = iconProcessor.processIconFromMap(iconMap, player)
            val condition = iconConfig.condition

            if (condition == null || iconProcessor.getConditionManager().checkCondition(condition, player, data)) {
                return iconConfig
            }
        }
        return null
    }


    
    fun getIconProcessor(): IconProcessor {
        return iconProcessor
    }
    

    fun getItemBuilder(): ItemBuilder {
        return itemBuilder
    }
    

    fun getActionResolver(): ActionResolver {
        return actionResolver
    }
}
