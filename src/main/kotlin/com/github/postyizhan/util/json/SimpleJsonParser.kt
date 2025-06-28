package com.github.postyizhan.util.json

/**
 * 简单的JSON解析器 - 专门用于解析GitHub API响应
 * 避免引入额外的JSON库依赖，提供基本的JSON解析功能
 */
object SimpleJsonParser {
    
    /**
     * 从JSON字符串中提取指定字段的值
     * @param json JSON字符串
     * @param fieldName 字段名称
     * @return 字段值，如果不存在则返回null
     */
    fun extractStringField(json: String, fieldName: String): String? {
        return try {
            // 查找字段名
            val fieldPattern = "\"$fieldName\"\\s*:\\s*\"([^\"]*)\""
            val regex = Regex(fieldPattern)
            val matchResult = regex.find(json)
            matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从JSON字符串中提取数字字段的值
     * @param json JSON字符串
     * @param fieldName 字段名称
     * @return 字段值，如果不存在或无法解析则返回null
     */
    fun extractNumberField(json: String, fieldName: String): Long? {
        return try {
            val fieldPattern = "\"$fieldName\"\\s*:\\s*(\\d+)"
            val regex = Regex(fieldPattern)
            val matchResult = regex.find(json)
            matchResult?.groupValues?.get(1)?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从JSON字符串中提取布尔字段的值
     * @param json JSON字符串
     * @param fieldName 字段名称
     * @return 字段值，如果不存在或无法解析则返回null
     */
    fun extractBooleanField(json: String, fieldName: String): Boolean? {
        return try {
            val fieldPattern = "\"$fieldName\"\\s*:\\s*(true|false)"
            val regex = Regex(fieldPattern)
            val matchResult = regex.find(json)
            matchResult?.groupValues?.get(1)?.toBooleanStrictOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 验证JSON字符串的基本格式
     * @param json JSON字符串
     * @return 如果格式有效则返回true
     */
    fun isValidJson(json: String): Boolean {
        if (json.isBlank()) return false
        
        val trimmed = json.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }
    
    /**
     * 解析GitHub Release API响应
     * @param json GitHub API响应JSON
     * @return GitHubRelease对象，如果解析失败则返回null
     */
    fun parseGitHubRelease(json: String): GitHubRelease? {
        if (!isValidJson(json)) return null
        
        return try {
            val tagName = extractStringField(json, "tag_name") ?: return null
            val name = extractStringField(json, "name")
            val body = extractStringField(json, "body")
            val publishedAt = extractStringField(json, "published_at")
            val prerelease = extractBooleanField(json, "prerelease") ?: false
            val draft = extractBooleanField(json, "draft") ?: false
            
            GitHubRelease(
                tagName = tagName,
                name = name,
                body = body,
                publishedAt = publishedAt,
                prerelease = prerelease,
                draft = draft
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * GitHub Release数据类
     */
    data class GitHubRelease(
        val tagName: String,
        val name: String?,
        val body: String?,
        val publishedAt: String?,
        val prerelease: Boolean,
        val draft: Boolean
    ) {
        /**
         * 获取清理后的版本号（移除v前缀）
         */
        val cleanVersion: String
            get() = tagName.removePrefix("v")
        
        /**
         * 检查是否为稳定版本
         */
        val isStable: Boolean
            get() = !prerelease && !draft
    }
}
