# SSC Geo 形态建模指南

这份文档面向从零制作 SSC Forge Form 的模型作者。SSC 会在每一帧把原版
`PlayerModel` 的姿势写入 Geo 模型中约定名称的骨骼。因此，Geo 模型不是只要
“外形对了”就够：骨骼名称和父子关系决定头部转向、走路、游泳、攻击、下蹲、
鞘翅飞行，以及物品栏预览是否正常。

## 1. 先理解两层模型

普通 Form 默认同时渲染原版玩家与 Geo 模型：Geo 常用于耳朵、尾巴、翅膀、盔甲
外壳等附加部件。完全自定义模型也可以使用 Geo，但仍应保留下面六个驱动骨骼，
才能获得玩家姿势同步。

不要把玩家可动的部件做成独立根骨骼。独立根骨骼不会继承玩家身体、头部或四肢的
变换，通常会表现为耳朵不随转头、尾巴不随下蹲、手臂模型不随攻击动作等。

## 2. 必需的玩家驱动骨骼

以下名称大小写敏感，必须完全一致。没有 cube 也要建立空骨骼；它们是给 SSC 写入
原版玩家姿势的锚点。

| Geo 骨骼名 | 建议 pivot | 接收的原版姿势 | 附属部件示例 |
| --- | --- | --- | --- |
| `bipedHead` | `[0, 24, 0]` | 看向、抬头、低头 | 耳朵、角、头发、头鳍、面具 |
| `bipedBody` | `[0, 24, 0]` | 身体朝向、下蹲、游泳姿势 | 尾巴根、翅膀、背包、裙摆 |
| `bipedLeftArm` | `[-5, 22, 0]` | 左臂摆动、挥动、持物姿势 | 左臂护甲、袖子、左手道具 |
| `bipedRightArm` | `[5, 22, 0]` | 右臂摆动、挥动、持物姿势 | 右臂护甲、袖子、右手道具 |
| `bipedLeftLeg` | `[-2, 12, 0]` | 左腿行走、游泳姿势 | 左腿护甲、蹄、鞋 |
| `bipedRightLeg` | `[2, 12, 0]` | 右腿行走、游泳姿势 | 右腿护甲、蹄、鞋 |

这些 pivot 与内置 `form_anubis_wolf_0.geo.json` 的基准一致。保持它们作为起点；
在子骨骼上调位置，而不是随意移动这六个骨骼的 pivot。

## 3. 从零建立骨架

在 Blockbench 新建 Bedrock Geometry 后，先建立六个空骨骼，再开始放 cube。最小可用
骨架如下；`cubes` 可留空。

```json
{
  "format_version": "1.12.0",
  "minecraft:geometry": [{
    "description": {
      "identifier": "geometry.example_form",
      "texture_width": 64,
      "texture_height": 64,
      "visible_bounds_width": 2,
      "visible_bounds_height": 3,
      "visible_bounds_offset": [0, 1.25, 0]
    },
    "bones": [
      { "name": "bipedHead", "pivot": [0, 24, 0] },
      { "name": "bipedBody", "pivot": [0, 24, 0] },
      { "name": "bipedLeftArm", "pivot": [-5, 22, 0] },
      { "name": "bipedRightArm", "pivot": [5, 22, 0] },
      { "name": "bipedLeftLeg", "pivot": [-2, 12, 0] },
      { "name": "bipedRightLeg", "pivot": [2, 12, 0] }
    ]
  }]
}
```

然后将实际模型分配为上述骨骼的子级。模型部件可以有任意自定义名称；只有六个锚点
名称是 SSC 的固定契约。

## 4. 附属部件应该挂在哪里

```text
bipedHead
├─ ear_left / ear_right
├─ horn_left / horn_right
├─ hair
└─ head_gill_* 

bipedBody
├─ tail_0 → tail_1 → tail_2
├─ wing_left_0 → wing_left_1
├─ wing_right_0 → wing_right_1
└─ skirt / backpack

bipedLeftArm
└─ left_sleeve / left_claw

bipedRightArm
└─ right_sleeve / right_claw

bipedLeftLeg / bipedRightLeg
└─ boots / hoofs / leg_armor
```

尾巴、翅膀、长发等多节部件要串成父子链：第二节的父级是第一节，而不是再次挂到
身体。这样上一节旋转时，整条链会自然跟随。

## 5. 猫耳：错误与正确做法

以下是错误结构。`ear` 是根骨骼，因此它不会随 `bipedHead` 的原版头部姿势旋转：

```json
{ "name": "ear", "pivot": [3, 32, 0] }
```

正确做法是先有 `bipedHead`，再把左右耳分别挂上去：

```json
{ "name": "bipedHead", "pivot": [0, 24, 0] },
{
  "name": "ear_right",
  "parent": "bipedHead",
  "pivot": [3, 32, 0],
  "cubes": []
},
{
  "name": "ear_left",
  "parent": "bipedHead",
  "pivot": [-3, 32, 0],
  "cubes": []
}
```

内置 `form_anubis_wolf_0.geo.json` 用的是相同原则：`ear_a_0` 与 `ear_b_0` 的
`parent` 都是 `bipedHead`。

## 6. 物品栏与第一人称

物品栏预览同样使用这六个骨骼，但不会使用上一帧的世界插值数据。因此不要依赖
“上一帧残留的旋转”来摆正模型；所有静止外形都应写在 Geo 文件的初始 pose 中。

第一人称手臂默认查找 `bipedRightArm` 与 `bipedLeftArm`。如果模型把手臂替换为自定义
骨骼，可在 Form 的 `ssc_form_model` 元数据中通过 `first_person_render` 覆盖；否则保持
默认骨骼名最稳妥。

## 7. 导出与排查清单

1. Geo、贴图、`ssc_form_model` 元数据中的资源路径使用同一个 namespace 和 Form ID。
2. 检查六个固定骨骼名称的拼写与大小写；`head`、`body`、`left_arm` 不是替代名称。
3. 每个会随玩家动的附属部件都必须有正确的 `parent`。
4. 尾巴/翅膀分节应形成链，而非多个独立根骨骼。
5. 进入世界后依次检查转头、攻击、行走、下蹲、游泳、物品栏预览与第一人称持物。
6. 若仅某个附属部件不动，先检查其 `parent`；若整条肢体不动，再检查对应的
   `biped*` 骨骼是否存在。

如需参考完整骨架，请从内置
`assets/shape-shifter-curse/geo/form/form_anubis_wolf_0.geo.json` 复制结构，再替换自己的
cube 与贴图，而不是从一个独立耳朵/尾巴根骨骼开始。
