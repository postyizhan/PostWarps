package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * 音效动作处理器
 */
class SoundAction(plugin: PostWarps) : AbstractAction(plugin) {
    
    // 默认声音映射，用于处理不同Minecraft版本的声音差异
    private val soundFallbacks = mapOf(
        "UI_BUTTON_CLICK" to arrayOf("UI_BUTTON_CLICK", "BLOCK_STONE_BUTTON_CLICK_ON", "BLOCK_WOOD_BUTTON_CLICK_ON", "CLICK"),
        "BLOCK_NOTE_BLOCK_CHIME" to arrayOf("BLOCK_NOTE_BLOCK_CHIME", "NOTE_BLOCK_CHIME", "NOTE_PIANO"),
        "BLOCK_CHEST_CLOSE" to arrayOf("BLOCK_CHEST_CLOSE", "CHEST_CLOSE", "BLOCK_WOOD_BREAK"),
        "ENTITY_ENDERMAN_TELEPORT" to arrayOf("ENTITY_ENDERMAN_TELEPORT", "ENDERMAN_TELEPORT"),
        "BLOCK_NOTE_BLOCK_PLING" to arrayOf("BLOCK_NOTE_BLOCK_PLING", "NOTE_BLOCK_PLING", "NOTE_PLING"),
        "ENTITY_ITEM_BREAK" to arrayOf("ENTITY_ITEM_BREAK", "ITEM_BREAK"),
        "ENTITY_PLAYER_LEVELUP" to arrayOf("ENTITY_PLAYER_LEVELUP", "PLAYER_LEVELUP", "LEVEL_UP"),
        "ENTITY_EXPERIENCE_ORB_PICKUP" to arrayOf("ENTITY_EXPERIENCE_ORB_PICKUP", "EXPERIENCE_ORB_PICKUP", "ORB_PICKUP"),
        "BLOCK_NOTE_BLOCK_HARP" to arrayOf("BLOCK_NOTE_BLOCK_HARP", "NOTE_BLOCK_HARP", "NOTE_HARP")
    )
    
    override fun execute(player: Player, actionValue: String) {
        val content = extractActionValue(actionValue, ActionType.SOUND.prefix)
        val parts = content.split(" ")
        val soundName = parts[0]
        val volume = if (parts.size > 1) parts[1].toFloatOrNull() ?: 1.0f else 1.0f
        val pitch = if (parts.size > 2) parts[2].toFloatOrNull() ?: 1.0f else 1.0f
        
        try {
            // 尝试使用指定的声音
            val sound = findSound(soundName)
            if (sound != null) {
                logDebug("Playing sound for player ${player.name}: $soundName (resolved as $sound), volume=$volume, pitch=$pitch")
                player.playSound(player.location, sound, volume, pitch)
            } else {
                logWarning("无法找到任何匹配的声音: $soundName")
            }
        } catch (e: Exception) {
            logWarning("播放声音时出错: ${e.message}")
        }
    }
    
    /**
     * 查找声音，支持使用fallback机制
     */
    private fun findSound(soundName: String): Sound? {
        // 先直接尝试查找声音
        try {
            return Sound.valueOf(soundName)
        } catch (e: Exception) {
            logDebug("找不到声音: $soundName, 尝试使用fallback")
        }
        
        // 如果有预定义的fallback，尝试使用
        val fallbacks = soundFallbacks[soundName]
        if (fallbacks != null) {
            for (fallback in fallbacks) {
                try {
                    return Sound.valueOf(fallback)
                } catch (e: Exception) {
                    logDebug("找不到fallback声音: $fallback")
                }
            }
        }
        
        // 如果所有尝试都失败，尝试更通用的声音
        val genericSounds = arrayOf(
            "UI_BUTTON_CLICK",
            "CLICK",
            "ITEM_PICKUP"
        )
        
        for (genericSound in genericSounds) {
            try {
                return Sound.valueOf(genericSound)
            } catch (e: Exception) {
                // 忽略错误，继续尝试
            }
        }
        
        return null
    }
}
