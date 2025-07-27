package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 动作抽象基类
 */
abstract class AbstractAction(protected val plugin: PostWarps) : Action {
    /**
     * 提取动作参数
     * @param action 原始动作字符串
     * @param prefix 动作前缀
     * @return 提取后的参数部分
     */
    protected fun extractActionValue(action: String, prefix: String): String {
        return action.substring(prefix.length).trim()
    }
    
    /**
     * 记录调试日志
     * @param message 调试消息
     */
    protected fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] $message")
        }
    }
    
    /**
     * 记录警告日志
     * @param message 警告消息
     */
    protected fun logWarning(message: String) {
        plugin.logger.warning(message)
    }
    
    /**
     * 从语言文件获取消息（带玩家语言支持）
     * @param player 玩家
     * @param key 消息键
     * @param replacements 替换参数
     * @return 格式化后的消息
     */
    protected fun getMessage(player: Player, key: String, vararg replacements: Pair<String, String>): String {
        var message = MessageUtil.getMessage("actions.$key", player)
        replacements.forEach { (placeholder, value) ->
            message = message.replace("{$placeholder}", value)
        }
        return MessageUtil.color(message)
    }

    /**
     * 向玩家发送消息
     * @param player 玩家
     * @param key 消息键
     * @param replacements 替换参数
     */
    protected fun sendMessage(player: Player, key: String, vararg replacements: Pair<String, String>) {
        player.sendMessage(getMessage(player, key, *replacements))
    }


}
