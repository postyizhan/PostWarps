# PostWarps

![版本](https://img.shields.io/github/v/release/postyizhan/PostSpawner?color=blue&label=版本)
![Minecraft](https://img.shields.io/badge/Minecraft-1.13+-green)
![语言](https://img.shields.io/badge/语言-简体中文|English-orange)

一个功能丰富的 Minecraft 地标传送插件，支持跨服传送和多种存储方式。

## 📚 功能特性

- 创建和管理个人地标
- 传送到地标
- 跨服传送（需要BungeeCord）
- 支持SQLite和MySQL存储
- 完整的权限控制
- 国际化支持（中文和英文）

## 💻 安装

1. 下载最新版本的 `PostWarps-x.x.jar` 文件
2. 将JAR文件放入服务器的 `plugins` 目录
3. 重启服务器或使用插件管理器加载插件
4. 配置文件和语言文件将在首次启动时自动创建
5. 根据需要编辑配置文件
6. 使用 `/warp help` 查看命令帮助

## 🔧 命令

| 命令 | 描述 |
|------|------|
| `/warp create <名称> [描述]` | 创建一个新地标 |
| `/warp delete <名称>` | 删除一个地标 |
| `/warp list [页码]` | 列出你的地标 |
| `/warp teleport <名称>` | 传送到地标 |
| `/warp info <名称>` | 查看地标信息 |
| `/warp reload` | 重新加载配置（需要管理员权限） |
| `/warp help` | 显示帮助信息 |

## 🔒 权限

| 权限 | 描述 | 默认 |
|------|------|------|
| `postwarps.create` | 允许创建地标 | 所有玩家 |
| `postwarps.delete` | 允许删除地标 | 所有玩家 |
| `postwarps.list` | 允许列出地标 | 所有玩家 |
| `postwarps.teleport` | 允许传送到地标 | 所有玩家 |
| `postwarps.admin` | 管理员权限 | OP |

## 📜 许可证

本插件采用 [GNU General Public License v2.0](LICENSE) 许可证。
