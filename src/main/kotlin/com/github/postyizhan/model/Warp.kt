package com.github.postyizhan.model

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.util.*

data class Warp(
    val id: Int = -1,
    val name: String,
    val ownerUUID: UUID,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val description: String = "",
    val server: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var visits: Int = 0
) {
    // 将地标转换为Location对象
    fun toLocation(): Location? {
        val world: World? = Bukkit.getWorld(worldName)
        return if (world != null) {
            Location(world, x, y, z, yaw, pitch)
        } else null
    }

    // 从Location创建地标
    companion object {
        fun fromLocation(
            name: String,
            ownerUUID: UUID,
            location: Location,
            description: String = "",
            server: String = ""
        ): Warp {
            return Warp(
                name = name,
                ownerUUID = ownerUUID,
                worldName = location.world?.name ?: "world",
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch,
                description = description,
                server = server
            )
        }
    }
}
