package com.github.postyizhan.gui.icon

/**
 * 图标配置数据类
 */
data class IconConfig(
    val condition: String? = null,
    val material: String? = null,
    val name: String? = null,
    val lore: List<String>? = null,
    val action: List<String>? = null,
    val amount: Int? = null,
    val customModelData: Int? = null,
    val enchanted: Boolean? = null
) {
    companion object {
        /**
         * 从Map创建IconConfig
         * @param map 配置Map
         * @return IconConfig实例
         */
        fun fromMap(map: Map<String, Any>): IconConfig {
            return IconConfig(
                condition = map["condition"] as? String,
                material = map["material"] as? String,
                name = map["name"] as? String,
                lore = (map["lore"] as? List<*>)?.mapNotNull { it as? String },
                action = when (val actionValue = map["action"]) {
                    is List<*> -> actionValue.mapNotNull { it as? String }
                    is String -> listOf(actionValue)
                    else -> null
                },
                amount = map["amount"] as? Int,
                customModelData = map["customModelData"] as? Int ?: map["custom-model-data"] as? Int,
                enchanted = map["enchanted"] as? Boolean
            )
        }
    }
    
    /**
     * 检查是否有任何非空配置
     * @return 如果有任何非空配置返回true
     */
    fun hasAnyConfig(): Boolean {
        return material != null || name != null || lore != null || 
               action != null || amount != null || customModelData != null || enchanted != null
    }
}
