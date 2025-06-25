package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps

/**
 * 动作工厂类，负责创建各种动作处理器
 */
class ActionFactory(private val plugin: PostWarps) {
    private val actionRegistry = mutableMapOf<ActionType, Action>()
    
    init {
        // 注册所有动作处理器
        registerAction(ActionType.COMMAND, CommandAction(plugin))
        registerAction(ActionType.OP_COMMAND, OpCommandAction(plugin))
        registerAction(ActionType.CONSOLE_COMMAND, ConsoleCommandAction(plugin))
        registerAction(ActionType.SOUND, SoundAction(plugin))
        registerAction(ActionType.MESSAGE, MessageAction(plugin))
        registerAction(ActionType.TITLE, TitleAction(plugin))
        
        // 地标特有动作
        registerAction(ActionType.WARP_CREATE, WarpCreateAction(plugin))
        registerAction(ActionType.WARP_DELETE, WarpDeleteAction(plugin))
        registerAction(ActionType.WARP_TELEPORT, WarpTeleportAction(plugin))
        registerAction(ActionType.WARP_INFO, WarpInfoAction(plugin))
        registerAction(ActionType.WARP_SET_PUBLIC, WarpSetPublicAction(plugin))
        registerAction(ActionType.WARP_SET_PRIVATE, WarpSetPrivateAction(plugin))
        registerAction(ActionType.WARP_SET, WarpSetAction(plugin))
        registerAction(ActionType.WARP_SEARCH, WarpSearchAction(plugin))
        registerAction(ActionType.WARP_SEARCH_CLEAR, WarpSearchClearAction(plugin))
        registerAction(ActionType.WARP_TOGGLE, WarpToggleAction(plugin))
        registerAction(ActionType.WARP_LOCATION, WarpSetLocationAction(plugin))
        registerAction(ActionType.MENU, MenuAction(plugin))
        registerAction(ActionType.CLOSE, CloseAction(plugin))
        registerAction(ActionType.PAGE_NEXT, PageNextAction(plugin))
        registerAction(ActionType.PAGE_PREV, PagePrevAction(plugin))
        registerAction(ActionType.UNKNOWN, UnknownAction(plugin))
    }
    
    /**
     * 注册动作处理器
     * @param type 动作类型
     * @param action 动作处理器
     */
    private fun registerAction(type: ActionType, action: Action) {
        actionRegistry[type] = action
    }
    
    /**
     * 获取动作处理器
     * @param action 动作内容
     * @return 对应的动作处理器
     */
    fun getAction(action: String): Action {
        val type = ActionType.fromAction(action)
        return actionRegistry[type] ?: actionRegistry[ActionType.UNKNOWN]!!
    }
    
    /**
     * 执行动作
     * @param player 玩家
     * @param action 动作内容
     */
    fun executeAction(player: org.bukkit.entity.Player, action: String) {
        val actionHandler = getAction(action)
        actionHandler.execute(player, action)
    }
}
