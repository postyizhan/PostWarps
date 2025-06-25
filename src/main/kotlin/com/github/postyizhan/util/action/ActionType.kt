package com.github.postyizhan.util.action

/**
 * 动作类型枚举
 */
enum class ActionType(val prefix: String) {
    // 通用动作
    COMMAND("[command]"),
    OP_COMMAND("[op]"),
    CONSOLE_COMMAND("[console]"),
    SOUND("[sound]"),
    MESSAGE("[message]"),
    TITLE("[title]"),
    
    // 地标特有动作
    WARP_CREATE("[warp_create]"),
    WARP_DELETE("[warp_delete]"),
    WARP_TELEPORT("[warp_tp]"),
    WARP_INFO("[warp_info]"),
    WARP_SET_PUBLIC("[warp_public]"),
    WARP_SET_PRIVATE("[warp_private]"),
    WARP_SET("[warp_set]"),
    WARP_SEARCH("[warp_search]"),
    WARP_SEARCH_CLEAR("[warp_search_clear]"),
    WARP_TOGGLE("[warp_toggle]"),
    WARP_LOCATION("[warp_location]"),
    MENU("[menu]"),
    CLOSE("[close]"),
    PAGE_NEXT("[page_next]"),
    PAGE_PREV("[page_prev]"),
    UNKNOWN("");
    
    companion object {
        /**
         * 根据动作内容获取动作类型
         * @param action 动作内容
         * @return 对应的动作类型
         */
        fun fromAction(action: String): ActionType {
            return values().firstOrNull { action.startsWith(it.prefix) } ?: UNKNOWN
        }
    }
}
