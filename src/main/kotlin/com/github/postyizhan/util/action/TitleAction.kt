package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 标题动作处理器
 */
class TitleAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val content = extractActionValue(actionValue, ActionType.TITLE.prefix)
        
        // 分割标题和副标题
        val parts = content.split(" ", limit = 2)
        val title = parts[0]
        val subtitle = if (parts.size > 1) parts[1] else ""
        
        // 标题动画时间配置
        val fadeIn = plugin.getConfigManager().getConfig().getInt("title.fade-in", 10)
        val stay = plugin.getConfigManager().getConfig().getInt("title.stay", 70)
        val fadeOut = plugin.getConfigManager().getConfig().getInt("title.fade-out", 20)
        
        logDebug("Sending title to player ${player.name}: title=$title, subtitle=$subtitle")
        player.sendTitle(MessageUtil.color(title), MessageUtil.color(subtitle), fadeIn, stay, fadeOut)
    }
}
