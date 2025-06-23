package com.github.postyizhan.database

import com.github.postyizhan.model.Warp
import java.util.*

/**
 * 数据库存储接口，定义数据库操作
 */
interface IStorage {
    /**
     * 初始化数据库
     */
    fun init()
    
    /**
     * 关闭数据库连接
     */
    fun close()
    
    /**
     * 创建地标
     */
    fun createWarp(warp: Warp): Boolean
    
    /**
     * 根据ID删除地标
     */
    fun deleteWarp(id: Int): Boolean
    
    /**
     * 根据名称和所有者删除地标
     */
    fun deleteWarp(name: String, owner: UUID): Boolean
    
    /**
     * 获取地标
     */
    fun getWarp(id: Int): Warp?
    
    /**
     * 根据名称和所有者获取地标
     */
    fun getWarp(name: String, owner: UUID): Warp?
    
    /**
     * 根据名称获取公开地标
     */
    fun getPublicWarp(name: String): Warp?
    
    /**
     * 获取所有地标
     */
    fun getAllWarps(): List<Warp>

    /**
     * 获取所有公开地标
     */
    fun getAllPublicWarps(): List<Warp>
    
    /**
     * 获取指定玩家的所有地标
     */
    fun getPlayerWarps(owner: UUID): List<Warp>
    
    /**
     * 获取指定玩家的公开地标
     */
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
}
