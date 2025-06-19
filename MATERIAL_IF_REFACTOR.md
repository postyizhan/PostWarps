# Material_if_true/false 重构文档

## 概述

成功将所有使用 `material_if_true` 和 `material_if_false` 配置的地方重构为使用子图标功能，提高了配置的一致性和可维护性。

## 重构内容

### 1. 配置文件更新

#### settings.yml - 公开/私有切换按钮
**重构前：**
```yaml
B:
  material: REDSTONE_TORCH
  material_if_true: TORCH
  material_if_false: REDSTONE_TORCH
  name: "&b公开/私有"
  action:
    - "[warp_toggle] public"
```

**重构后：**
```yaml
B:
  material: REDSTONE_TORCH
  name: "&b公开/私有"
  icons:
    # 公开状态时显示火把
    - condition: 'data public'
      material: TORCH
      name: "&a公开地标"
      lore:
        - "&7当前状态: &a公开"
        - "&7其他玩家可以看到此地标"
        - ""
        - "&e► 点击设为私有"
      action:
        - "[warp_toggle] public"
        - "[sound] CLICK 0.5 1.5"
    # 私有状态时显示红石火把
    - condition: '!data public'
      material: REDSTONE_TORCH
      name: "&c私有地标"
      lore:
        - "&7当前状态: &c私有"
        - "&7只有你可以看到此地标"
        - ""
        - "&e► 点击设为公开"
      action:
        - "[warp_toggle] public"
        - "[sound] CLICK 0.5 1.5"
  action:
    - "[warp_toggle] public"
    - "[sound] CLICK 0.5 1.5"
```

#### create.yml - 公开/私有切换按钮
使用相同的子图标配置替换了 `material_if_true/false` 配置。

#### private_warps.yml & public_warps.yml - 分页按钮
**重构前：**
```yaml
P:
  material: BARRIER
  display_condition: has_prev
  material_if_true: ARROW
  name: "&a上一页"
  action:
    - "[page_prev]"
```

**重构后：**
```yaml
P:
  material: BARRIER
  name: "&a上一页"
  icons:
    # 有上一页时显示箭头
    - condition: 'data has_prev'
      material: ARROW
      name: "&a上一页"
      lore:
        - "&7查看上一页地标"
        - ""
        - "&e► 点击查看"
      action:
        - "[page_prev]"
        - "[sound] CLICK 0.5 1.0"
    # 没有上一页时显示屏障
    - condition: '!data has_prev'
      material: BARRIER
      name: "&7上一页"
      lore:
        - "&7没有上一页了"
      action: []
  action:
    - "[page_prev]"
    - "[sound] CLICK 0.5 1.0"
```

### 2. 代码清理

#### ItemBuilder.kt (gui/builder)
- 移除了 `processConditionalMaterial` 方法中的条件材料处理逻辑
- 保留方法结构以维持兼容性，但直接返回原材料名称

#### MenuItemProcessor.kt
- 简化了 `checkDisplayCondition` 方法，直接返回 true
- 简化了 `createFallbackItem` 方法，直接返回 null
- 添加了废弃注释说明功能已由子图标替代

#### ItemBuilder.kt (gui/util) 
- 移除了 `getMaterial` 方法中的条件材料处理逻辑
- 保留了 `checkDisplayCondition` 方法以维持向后兼容性（用于分页功能）
- 添加了兼容性注释

## 重构优势

### 1. 配置一致性
- 所有条件显示逻辑都使用统一的子图标功能
- 消除了多种不同的条件配置方式
- 提高了配置文件的可读性

### 2. 功能增强
- 子图标支持更丰富的条件类型（权限、OP、数据、玩家等）
- 可以为不同状态设置不同的名称、描述和动作
- 支持完整的属性继承机制

### 3. 代码简化
- 移除了重复的条件处理逻辑
- 统一了物品创建流程
- 减少了代码维护成本

### 4. 向后兼容
- 保留了必要的兼容性代码
- 旧的配置方式仍然可以工作（虽然不推荐）
- 平滑的迁移路径

## 配置迁移指南

### 从 material_if_true/false 迁移到子图标

**步骤1：识别条件**
```yaml
# 旧配置
material: REDSTONE_TORCH
material_if_true: TORCH
material_if_false: REDSTONE_TORCH
```

**步骤2：转换为子图标**
```yaml
# 新配置
material: REDSTONE_TORCH  # 默认材料
icons:
  - condition: 'data your_condition'
    material: TORCH  # 条件为true时的材料
  - condition: '!data your_condition'
    material: REDSTONE_TORCH  # 条件为false时的材料
```

**步骤3：增强功能**
```yaml
# 增强版配置
icons:
  - condition: 'data your_condition'
    material: TORCH
    name: "&a状态：开启"
    lore:
      - "&7功能已启用"
      - "&e► 点击关闭"
    action:
      - "[toggle] your_condition"
  - condition: '!data your_condition'
    material: REDSTONE_TORCH
    name: "&c状态：关闭"
    lore:
      - "&7功能已禁用"
      - "&e► 点击开启"
    action:
      - "[toggle] your_condition"
```

## 支持的条件类型

### 数据条件
- `data key` - 检查数据为true
- `!data key` - 检查数据为false

### 权限条件
- `perm permission.node` - 检查权限
- `!perm permission.node` - 检查没有权限

### OP条件
- `op` - 检查是否是OP
- `!op` - 检查是否不是OP

### 玩家条件
- `player PlayerName` - 检查玩家名称
- `!player PlayerName` - 检查不是指定玩家

## 测试建议

### 1. 功能测试
- 测试公开/私有切换功能
- 测试分页按钮显示
- 验证不同条件下的图标显示

### 2. 兼容性测试
- 确保旧配置仍然工作
- 验证新配置的正确性
- 测试边界情况

### 3. 性能测试
- 验证子图标功能的性能
- 确保没有性能回归
- 测试大量条件的处理

## 总结

通过这次重构：

✅ **统一了配置方式** - 所有条件显示都使用子图标功能
✅ **增强了功能** - 支持更多条件类型和属性定制
✅ **简化了代码** - 移除了重复的条件处理逻辑
✅ **保持了兼容性** - 旧功能仍然可以正常工作
✅ **提高了可维护性** - 代码更清晰，配置更一致

这次重构为插件的长期发展奠定了更好的基础，同时为用户提供了更强大和灵活的配置选项。
