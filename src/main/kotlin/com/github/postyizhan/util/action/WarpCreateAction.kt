package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 创建地标动作处理器
 */
class WarpCreateAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        logDebug("Player ${player.name} creating warp")
        
        val data = plugin.getMenuManager().getPlayerData(player)
        val name = data["name"] as? String
        val description = data["desc"] as? String ?: ""
        val isPublic = data["is_public"] as? Boolean ?: false
        
        // 检查名称是否存在
        if (name.isNullOrBlank()) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("create.no-name")
            ))
            return
        }
        
        // 检查名称是否已经存在
        if (plugin.getDatabaseManager().getWarp(name, player.uniqueId) != null) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("create.name-exists")
            ))
            return
        }
        
        logDebug("Creating warp: name=$name, desc=$description, public=$isPublic")

        // 检查经济费用
        if (!plugin.getEconomyService().chargeCreateCost(player)) {
            return
        }

        // 创建地标
        val warp = Warp.fromLocation(
            name = name,
            owner = player.uniqueId,
            ownerName = player.name,
            location = player.location,
            isPublic = isPublic,
            description = description
        )
        
        val success = plugin.getDatabaseManager().createWarp(warp)
        if (success) {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("create.success")
                    .replace("{name}", name)
            ))
            
            // 清除数据
            data.remove("name")
            data.remove("desc")
            
            // 关闭菜单
            player.closeInventory()
        } else {
            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("create.failed")
            ))
        }
    }
}
