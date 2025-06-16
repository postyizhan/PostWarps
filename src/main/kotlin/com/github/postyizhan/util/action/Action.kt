package com.github.postyizhan.util.action

import org.bukkit.entity.Player

/**
 * 动作接口，所有动作类型都需要实现此接口
 */
interface Action {
    /**
     * 执行动作
     * @param player 执行动作的玩家
     * @param actionValue 动作的具体参数
     */
    fun execute(player: Player, actionValue: String)
}
