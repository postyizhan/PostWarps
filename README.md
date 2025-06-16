# PostWarps

PostWarps 是一个功能强大的 Minecraft 地标传送插件，允许玩家创建、管理和传送到各种地标位置。

## 功能特点

- 🌟 支持创建公开和私有地标
- 📝 支持地标描述和自定义名称
- 🔍 地标搜索功能
- 📊 漂亮的 GUI 菜单系统
- 🔒 完整的权限控制
- 💾 支持 MySQL/SQLite 数据存储
- 🌐 多语言支持
- 🛠️ 铁砧界面输入系统，更好的用户体验

## 依赖项

- **必需**：Bukkit/Spigot/Paper 服务器 (1.13+)

## 安装

1. 下载最新版本的 PostWarps 插件
2. 将插件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 完成！现在你可以使用 `/pw` 命令来管理地标了

## 权限

- `postwarps.use` - 允许使用基本命令
- `postwarps.create` - 允许创建地标
- `postwarps.delete` - 允许删除自己的地标
- `postwarps.delete.others` - 允许删除他人的地标
- `postwarps.teleport` - 允许传送到地标
- `postwarps.list` - 允许查看地标列表
- `postwarps.info` - 允许查看地标信息
- `postwarps.public` - 允许将地标设为公开
- `postwarps.private` - 允许将地标设为私有
- `postwarps.menu` - 允许使用菜单
- `postwarps.admin` - 管理员权限（包含所有权限）

## 命令

- `/pw create <名称> [描述] [公开(true/false)]` - 创建新地标
- `/pw delete <名称>` - 删除一个地标
- `/pw list [my/public]` - 查看地标列表
- `/pw tp <名称>` - 传送到地标
- `/pw info <名称>` - 查看地标信息
- `/pw public <名称>` - 将地标设为公开
- `/pw private <名称>` - 将地标设为私有
- `/pw menu [main/private_warps/public_warps/create]` - 打开菜单
- `/pw reload` - 重载插件
- `/pw version` - 显示版本信息

## 配置

插件的配置文件位于 `plugins/PostWarps/config.yml`。你可以在那里自定义各种设置。

## 常见问题

### 菜单中设置名称或描述按钮无法工作？

如果你遇到铁砧界面无法打开的问题，可能是插件安装不完整或版本不兼容导致的。请尝试重新下载最新版本的插件，或检查服务器的错误日志。

### 如何备份我的地标数据？

如果您使用 SQLite 存储，数据保存在 `plugins/PostWarps/database.db` 中。
如果您使用 MySQL 存储，可以使用数据库备份工具备份数据。

## 支持与贡献

如果您遇到任何问题或有改进建议，请提交 issue 或 PR。

## 许可证

本插件采用 [GNU General Public License v2.0](LICENSE) 许可证。
