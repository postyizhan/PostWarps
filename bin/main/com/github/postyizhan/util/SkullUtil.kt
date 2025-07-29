package com.github.postyizhan.util

import com.github.postyizhan.PostWarps
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import com.google.gson.JsonParser
import java.util.logging.Level

/**
 * 头颅工具类 - 处理玩家头颅相关操作
 */
object SkullUtil {
    
    private val plugin: PostWarps get() = PostWarps.getInstance()
    
    // 皮肤缓存 - 玩家名 -> 纹理数据
    private val skinCache = ConcurrentHashMap<String, String>()
    
    // UUID缓存 - 玩家名 -> UUID
    private val uuidCache = ConcurrentHashMap<String, String>()
    
    /**
     * 头颅信息数据类
     */
    data class SkullInfo(
        val material: String = "PLAYER_HEAD",
        val skullOwner: String? = null,
        val skullTexture: String? = null
    ) {
        fun isPlayerHead(): Boolean = material == "PLAYER_HEAD"
        fun hasValidData(): Boolean = isPlayerHead() && (skullOwner != null || skullTexture != null)
    }
    
    /**
     * 从物品中提取头颅信息
     */
    fun extractSkullInfo(item: ItemStack): SkullInfo {
        if (item.type != Material.PLAYER_HEAD) {
            return SkullInfo(material = item.type.name)
        }
        
        val meta = item.itemMeta as? SkullMeta ?: return SkullInfo()
        
        // 尝试获取拥有者
        val owner = meta.owningPlayer?.name
        if (owner != null) {
            return SkullInfo(skullOwner = owner)
        }
        
        // 尝试获取自定义纹理（1.18+支持）
        try {
            val profileMethod = meta.javaClass.getMethod("getPlayerProfile")
            val profile = profileMethod.invoke(meta)
            if (profile != null) {
                val texturesMethod = profile.javaClass.getMethod("getTextures")
                val textures = texturesMethod.invoke(profile)
                val skinMethod = textures.javaClass.getMethod("getSkin")
                val skin = skinMethod.invoke(textures)
                if (skin != null) {
                    val texture = encodeTextureUrl(skin.toString())
                    return SkullInfo(skullTexture = texture)
                }
            }
        } catch (e: Exception) {
            // 版本不支持或其他错误，忽略
        }
        
        return SkullInfo()
    }
    
    /**
     * 创建玩家头颅物品
     */
    fun createPlayerSkull(player: Player): ItemStack {
        val skull = ItemStack(Material.PLAYER_HEAD)
        val meta = skull.itemMeta as SkullMeta
        meta.owningPlayer = player
        skull.itemMeta = meta
        return skull
    }
    
    /**
     * 创建指定玩家名的头颅物品
     */
    fun createPlayerSkull(playerName: String): ItemStack {
        val skull = ItemStack(Material.PLAYER_HEAD)
        val meta = skull.itemMeta as SkullMeta
        
        // 尝试从在线玩家获取
        val onlinePlayer = Bukkit.getPlayer(playerName)
        if (onlinePlayer != null) {
            meta.owningPlayer = onlinePlayer
        } else {
            // 使用离线玩家
            val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
            meta.owningPlayer = offlinePlayer
        }
        
        skull.itemMeta = meta
        return skull
    }
    
    /**
     * 根据头颅信息创建物品
     */
    fun createSkullFromInfo(skullInfo: SkullInfo): ItemStack {
        if (!skullInfo.isPlayerHead()) {
            // 普通材质
            return try {
                ItemStack(Material.valueOf(skullInfo.material.uppercase()))
            } catch (e: IllegalArgumentException) {
                ItemStack(Material.STONE)
            }
        }
        
        val skull = ItemStack(Material.PLAYER_HEAD)
        val meta = skull.itemMeta as SkullMeta
        
        when {
            skullInfo.skullOwner != null -> {
                // 使用玩家名设置头颅
                val offlinePlayer = Bukkit.getOfflinePlayer(skullInfo.skullOwner)
                meta.owningPlayer = offlinePlayer
            }
            skullInfo.skullTexture != null -> {
                // 使用自定义纹理（1.18+支持）
                try {
                    val createProfileMethod = Bukkit::class.java.getMethod("createPlayerProfile", UUID::class.java, String::class.java)
                    val profile = createProfileMethod.invoke(null, UUID.randomUUID(), "CustomSkull")
                    val textureUrl = decodeTextureUrl(skullInfo.skullTexture)
                    if (textureUrl != null) {
                        val texturesMethod = profile.javaClass.getMethod("getTextures")
                        val textures = texturesMethod.invoke(profile)
                        val setSkinMethod = textures.javaClass.getMethod("setSkin", URL::class.java)
                        setSkinMethod.invoke(textures, URL(textureUrl))

                        val setPlayerProfileMethod = meta.javaClass.getMethod("setPlayerProfile", profile.javaClass.interfaces[0])
                        setPlayerProfileMethod.invoke(meta, profile)
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("创建自定义纹理头颅失败: ${e.message}")
                }
            }
        }
        
        skull.itemMeta = meta
        return skull
    }
    
    /**
     * 异步获取玩家皮肤信息
     */
    fun getPlayerSkinAsync(playerName: String): CompletableFuture<SkullInfo> {
        return CompletableFuture.supplyAsync {
            try {
                getPlayerSkin(playerName)
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "获取玩家 $playerName 皮肤失败", e)
                SkullInfo(skullOwner = playerName) // 降级到使用玩家名
            }
        }
    }
    
    /**
     * 获取玩家皮肤信息（同步）
     */
    private fun getPlayerSkin(playerName: String): SkullInfo {
        // 检查缓存
        skinCache[playerName]?.let { texture ->
            return SkullInfo(skullTexture = texture)
        }
        
        try {
            // 首先获取UUID
            val uuid = getPlayerUUID(playerName) ?: return SkullInfo(skullOwner = playerName)
            
            // 获取皮肤数据
            val skinUrl = "https://sessionserver.mojang.com/session/minecraft/profile/$uuid?unsigned=false"
            val connection = URL(skinUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val response = InputStreamReader(connection.inputStream).readText()
                val jsonObject = JsonParser.parseString(response).asJsonObject
                
                val properties = jsonObject.getAsJsonArray("properties")
                if (properties != null && properties.size() > 0) {
                    val textureProperty = properties[0].asJsonObject
                    val textureValue = textureProperty.get("value").asString
                    
                    // 缓存结果
                    skinCache[playerName] = textureValue
                    
                    return SkullInfo(skullTexture = textureValue)
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("获取玩家 $playerName 皮肤数据失败: ${e.message}")
        }
        
        // 降级到使用玩家名
        return SkullInfo(skullOwner = playerName)
    }
    
    /**
     * 获取玩家UUID
     */
    private fun getPlayerUUID(playerName: String): String? {
        // 检查缓存
        uuidCache[playerName]?.let { return it }
        
        try {
            val uuidUrl = "https://api.mojang.com/users/profiles/minecraft/$playerName"
            val connection = URL(uuidUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val response = InputStreamReader(connection.inputStream).readText()
                val jsonObject = JsonParser.parseString(response).asJsonObject
                val uuid = jsonObject.get("id").asString
                
                // 格式化UUID
                val formattedUuid = "${uuid.substring(0, 8)}-${uuid.substring(8, 12)}-${uuid.substring(12, 16)}-${uuid.substring(16, 20)}-${uuid.substring(20, 32)}"
                
                // 缓存结果
                uuidCache[playerName] = formattedUuid
                
                return formattedUuid
            }
        } catch (e: Exception) {
            plugin.logger.warning("获取玩家 $playerName UUID失败: ${e.message}")
        }
        
        return null
    }
    
    /**
     * 编码纹理URL为Base64
     */
    private fun encodeTextureUrl(textureUrl: String): String {
        val textureJson = """{"textures":{"SKIN":{"url":"$textureUrl"}}}"""
        return Base64.getEncoder().encodeToString(textureJson.toByteArray())
    }
    
    /**
     * 解码Base64纹理数据为URL
     */
    private fun decodeTextureUrl(textureData: String): String? {
        return try {
            val decoded = String(Base64.getDecoder().decode(textureData))
            val jsonObject = JsonParser.parseString(decoded).asJsonObject
            jsonObject.getAsJsonObject("textures")
                ?.getAsJsonObject("SKIN")
                ?.get("url")?.asString
        } catch (e: Exception) {
            plugin.logger.warning("解码纹理数据失败: ${e.message}")
            null
        }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        skinCache.clear()
        uuidCache.clear()
    }
}
