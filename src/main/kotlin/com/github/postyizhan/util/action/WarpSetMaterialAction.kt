package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * 地标设置显示材质动作处理器
 */
class WarpSetMaterialAction(plugin: PostWarps) : AbstractAction(plugin) {

    override fun execute(player: Player, actionValue: String) {
        logDebug("Player ${player.name} setting warp material")

        // 从玩家数据中获取地标ID
        val data = plugin.getMenuManager().getPlayerData(player)
        val warpId = data["warp_id"] as? Int ?: run {
            logDebug("No warp_id found in player data")
            sendMessage(player, "warp_material.no_warp")
            return
        }

        // 从数据库获取地标
        val currentWarp = plugin.getDatabaseManager().getWarp(warpId) ?: run {
            logDebug("Warp with ID $warpId not found")
            sendMessage(player, "warp_material.no_warp")
            return
        }

        // 检查权限
        if (!player.hasPermission("postwarps.warp.material") &&
            !player.hasPermission("postwarps.admin") &&
            currentWarp.owner != player.uniqueId) {
            sendMessage(player, "no-permission")
            return
        }

        // 获取玩家手中的物品材质
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) {
            sendMessage(player, "warp_material.no_item")
            return
        }

        val materialName = itemInHand.type.name

        // 验证材质是否有效
        try {
            Material.valueOf(materialName)
        } catch (e: IllegalArgumentException) {
            sendMessage(player, "warp_material.invalid_material", "material" to materialName)
            return
        }

        // 更新地标显示材质
        val success = plugin.getDatabaseManager().updateWarpMaterial(
            currentWarp.id,
            materialName
        )

        if (success) {
            sendMessage(player, "warp_material.success",
                "name" to currentWarp.name,
                "material" to materialName
            )

            // 关闭菜单让玩家重新打开以看到更新
            player.closeInventory()
        } else {
            sendMessage(player, "warp_material.failed")
        }
    }
}
