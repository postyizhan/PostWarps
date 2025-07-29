package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.SkullUtil
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


/**
 * 地标设置显示材质动作处理器
 * 支持多种点击方式：
 * - 左键：设置手持物品为显示材质
 * - 右键：设置为自己的头颅
 * - Shift+右键：输入玩家名设置头颅
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

        // 获取点击类型
        val clickType = data["click_type"] as? String ?: "left"

        // 调试信息
        logDebug("Player ${player.name} clicked with type: $clickType")

        when (clickType) {
            "left" -> {
                logDebug("Handling left click - hand item setting")
                handleHandItemSetting(player, currentWarp)
            }
            "right" -> {
                logDebug("Handling right click - self skull setting")
                handleSelfSkullSetting(player, currentWarp)
            }
            "shift_right" -> {
                logDebug("Handling shift+right click - player skull input")
                handlePlayerSkullInput(player, currentWarp)
            }
            else -> {
                logDebug("Handling default click type: $clickType")
                handleHandItemSetting(player, currentWarp) // 默认行为
            }
        }
    }

    /**
     * 处理手持物品设置
     */
    private fun handleHandItemSetting(player: Player, warp: Warp) {
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) {
            sendMessage(player, "warp_material.no_item")
            return
        }

        // 提取物品信息
        val skullInfo = SkullUtil.extractSkullInfo(itemInHand)

        // 更新地标显示材质
        val success = plugin.getDatabaseManager().updateWarpMaterial(
            warp.id,
            skullInfo.material,
            skullInfo.skullOwner,
            skullInfo.skullTexture
        )

        if (success) {
            val materialDisplayName = if (skullInfo.isPlayerHead()) {
                if (skullInfo.skullOwner != null) {
                    "${skullInfo.skullOwner}的头颅"
                } else {
                    "自定义头颅"
                }
            } else {
                skullInfo.material
            }

            sendMessage(player, "warp_material.success",
                "name" to warp.name,
                "material" to materialDisplayName
            )
            player.closeInventory()
        } else {
            sendMessage(player, "warp_material.failed")
        }
    }

    /**
     * 处理设置为自己头颅
     */
    private fun handleSelfSkullSetting(player: Player, warp: Warp) {
        val success = plugin.getDatabaseManager().updateWarpMaterial(
            warp.id,
            "PLAYER_HEAD",
            player.name,
            null
        )

        if (success) {
            sendMessage(player, "warp_material.success",
                "name" to warp.name,
                "material" to "${player.name}的头颅"
            )
            player.closeInventory()
        } else {
            sendMessage(player, "warp_material.failed")
        }
    }

    /**
     * 处理玩家头颅输入
     */
    private fun handlePlayerSkullInput(player: Player, warp: Warp) {
        // 获取当前菜单名称
        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: "settings"

        // 创建输入物品
        val inputItem = createInputItem()

        AnvilGUI.Builder()
            .onClose { stateSnapshot ->
                // 重新打开之前的菜单
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    plugin.getMenuManager().openMenu(stateSnapshot.player, currentMenu)
                }, 1L)
            }
            .onClick { slot, stateSnapshot ->
                if (slot == AnvilGUI.Slot.OUTPUT) {
                    // 获取输入文本并去掉最前面的空格
                    val playerName = stateSnapshot.text.removePrefix(" ").trim()
                    if (playerName.isEmpty()) {
                        sendMessage(stateSnapshot.player, "warp_material.invalid_player_name")
                        return@onClick emptyList()
                    }

                    // 异步获取皮肤信息
                    SkullUtil.getPlayerSkinAsync(playerName).thenAccept { skullInfo ->
                        plugin.server.scheduler.runTask(plugin, Runnable {
                            val success = plugin.getDatabaseManager().updateWarpMaterial(
                                warp.id,
                                "PLAYER_HEAD",
                                skullInfo.skullOwner,
                                skullInfo.skullTexture
                            )

                            if (success) {
                                sendMessage(stateSnapshot.player, "warp_material.success",
                                    "name" to warp.name,
                                    "material" to "${playerName}的头颅"
                                )
                            } else {
                                sendMessage(stateSnapshot.player, "warp_material.failed")
                            }
                        })
                    }

                    // 重新打开之前的菜单
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        plugin.getMenuManager().openMenu(stateSnapshot.player, currentMenu)
                    }, 1L)

                    return@onClick listOf(AnvilGUI.ResponseAction.close())
                } else {
                    emptyList()
                }
            }
            .itemLeft(inputItem)
            .title("设置玩家头颅")
            .plugin(plugin)
            .open(player)
    }

    /**
     * 创建输入物品
     */
    private fun createInputItem(): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta

        // 设置物品显示名称为一个空格
        meta?.setDisplayName(" ")

        item.itemMeta = meta
        return item
    }
}
