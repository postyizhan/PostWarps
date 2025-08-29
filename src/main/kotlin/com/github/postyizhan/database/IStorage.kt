package com.github.postyizhan.database

import com.github.postyizhan.model.Warp
import java.util.*

interface IStorage {

    fun init()
    

    fun close()
    

    fun createWarp(warp: Warp): Boolean
    

    fun deleteWarp(id: Int): Boolean
    

    fun deleteWarp(name: String, owner: UUID): Boolean
    

    fun getWarp(id: Int): Warp?
    

    fun getWarp(name: String, owner: UUID): Warp?
    

    fun getPublicWarp(name: String): Warp?
    

    fun getAllWarps(): List<Warp>


    fun getAllPublicWarps(): List<Warp>
    

    fun getPlayerWarps(owner: UUID): List<Warp>
    

    fun getPlayerPublicWarps(owner: UUID): List<Warp>
    
    /**
     * 获取指定玩家的私有地标
     */
    fun getPlayerPrivateWarps(owner: UUID): List<Warp>
    
    /**
     * 设置地标公开状态
     */
    fun setWarpPublic(id: Int, isPublic: Boolean): Boolean
    
    /**
     * 根据名称和所有者设置地标公开状态
     */
    fun setWarpPublic(name: String, owner: UUID, isPublic: Boolean): Boolean
    
    /**
     * 更新地标描述
     */
    fun updateWarpDescription(id: Int, description: String): Boolean
    
    /**
     * 根据名称和所有者更新地标描述
     */
    fun updateWarpDescription(name: String, owner: UUID, description: String): Boolean
    
    /**
     * 更新地标位置
     */
    fun updateWarpLocation(id: Int, worldName: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean

    /**
     * 更新地标显示材质
     */
    fun updateWarpMaterial(id: Int, material: String): Boolean

    /**
     * 根据名称和所有者更新地标显示材质
     */
    fun updateWarpMaterial(name: String, owner: UUID, material: String): Boolean

    /**
     * 更新地标显示材质和头颅信息
     */
    fun updateWarpMaterial(id: Int, material: String, skullOwner: String?, skullTexture: String?): Boolean

    /**
     * 根据名称和所有者更新地标显示材质和头颅信息
     */
    fun updateWarpMaterial(name: String, owner: UUID, material: String, skullOwner: String?, skullTexture: String?): Boolean
}
