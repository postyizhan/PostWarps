package com.github.postyizhan.model

import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

/**
 * 地标数据模型
 * @param id 唯一ID
 * @param name 地标名称
 * @param owner 地标拥有者UUID
 * @param ownerName 地标拥有者名称
 * @param worldName 世界名称
 * @param x X坐标
 * @param y Y坐标
 * @param z Z坐标
 * @param yaw 偏航角
 * @param pitch 俯仰角
 * @param isPublic 是否公开
 * @param description 地标描述
 * @param displayMaterial 显示材质
 * @param createTime 创建时间
 */
data class Warp(
    val id: Int = -1,
    val name: String,
    val owner: UUID,
    val ownerName: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val isPublic: Boolean = false,
    val description: String = "",
    val displayMaterial: String = "ENDER_PEARL",
    val skullOwner: String? = null,
    val skullTexture: String? = null,
    val createTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取地标位置
     */
    fun getLocation(): Location? {
        val world = Bukkit.getWorld(worldName) ?: return null
        return Location(world, x, y, z, yaw, pitch)
    }
    
    /**
     * 从位置创建地标
     */
    companion object {
        fun fromLocation(
            name: String,
            owner: UUID,
            ownerName: String,
            location: Location,
            isPublic: Boolean = false,
            description: String = "",
            displayMaterial: String = "ENDER_PEARL",
            skullOwner: String? = null,
            skullTexture: String? = null
        ): Warp {
            return Warp(
                name = name,
                owner = owner,
                ownerName = ownerName,
                worldName = location.world?.name ?: "world",
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch,
                isPublic = isPublic,
                description = description,
                displayMaterial = displayMaterial,
                skullOwner = skullOwner,
                skullTexture = skullTexture
            )
        }
    }
    
    /**
     * 获取格式化的坐标字符串
     */
    fun getFormattedCoordinates(): String {
        return String.format("%.1f, %.1f, %.1f", x, y, z)
    }

    /**
     * 获取头颅信息
     */
    fun getSkullInfo(): com.github.postyizhan.util.SkullUtil.SkullInfo {
        return com.github.postyizhan.util.SkullUtil.SkullInfo(
            material = displayMaterial,
            skullOwner = skullOwner,
            skullTexture = skullTexture
        )
    }

    /**
     * 是否为玩家头颅
     */
    fun isPlayerHead(): Boolean {
        return displayMaterial == "PLAYER_HEAD"
    }
}
