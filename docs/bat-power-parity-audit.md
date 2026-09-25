# 蝙蝠 0–3 阶 Power 对照（2026-09-25）

参照 `C:\Users\gengy\Desktop\Shape-Shifter-Curse_Unofficial-Port` 的 Fabric 形态数据，以及本机 Gradle 缓存中的 Apoli 2.9.2 源码。四阶分别分配 8、20、25、29 个 Power，共 46 个不同 ID；Forge 与 Fabric 的形态分配和这 46 份 Power JSON 逐项相同。下表检查执行路径，不能替代游戏内行为验证。

| 范围 | 本次对照及处理 |
| --- | --- |
| `bat_vision`（1 阶起） | 删除 Forge 给所有蝙蝠额外附加的原版夜视药水。Fabric 从 1 阶起通过 `apoli:night_vision` 渲染，0 阶没有该 Power。 |
| `slow_falling`（2 阶起） | 删除 Forge 形态标志的额外缓降；将 `apoli:modify_falling` 从服务端 tick 的 Y 速度钳制改为在 `LivingEntity.travel()` 中修改重力参数，并在 `take_fall_damage=false` 时清零落距，与 Apoli 源码的注入位置一致。 |
| `form_bat_3_jump_high` | 删除 Forge 形态定义的额外 0.2 跳跃加成，保留 Power 的 `modify_jump`，避免末阶重复加成。`form_bat_3_jump_boost` 的动作路径保持原样。 |
| `form_bat_3_block_attach` | 侧面贴附的第一次定位改为 Fabric 的方块中心加 0.75 法线位移和 -0.5 Y；维持贴附的位置仍使用原有路径。服务端及客户端同步入口同时修改。 |
| `drop_tool_after_digging`、饰品附带的 `action_on_block_break` | 从 Forge 的破坏前事件移到 `ServerPlayerGameMode.destroyBlock()` 成功采收后，按 Apoli 的默认 `only_when_harvested=true` 执行动作；加入 Power 条件判断。 |
| `drop_weapon_after_hit`、饰品附带的 `self_action_on_hit` | 从 `LivingHurtEvent` 移到目标 `LivingEntity.hurt()` 返回 `true` 后执行，避免未成功命中时掉落武器或消耗饰品耐久。这个修正也影响其他形态使用相同 Apoli 类型的 Power。 |
| `form_bat_hit_wall_damage_reduce`（2 阶起） | 补上 `modify_damage_taken` 的 `damage_condition` 判断。原先所有伤害都会经过 `set_total 0`，导致蝙蝠免疫普通伤害甚至 `/kill`；现在仅匹配 `bat_immune_damage_tag` 中的 `minecraft:fly_into_wall`。同时将 Power 的实体条件作用于受伤玩家本人。 |
| `bypass_landing_effect`（2 阶起） | 二次复查发现 Forge 错把它实现为无摔伤。Fabric 只覆盖 `Entity.isSuppressingBounce()`，现已改为只压制方块弹跳；摔伤仍由 `modify_falling` 等独立 Power 处理。 |
| `elytra_no_render`（2 阶起） | 删除 Forge 在空中疾跑时自动开始滑翔的额外逻辑。Fabric 的 `elytra_flight` 提供原版空中再次按跳跃键的滑翔能力，不主动开始滑翔。 |
| `form_bat_3_sky_speed`（3 阶） | 二次核对发现 Forge 只在服务端每 tick 执行前向 `add_velocity`，客户端没有同样的本地移动计算。已给客户端加入独立的 Power 相位和速度执行路径。Fabric JSON 明确要求水外、离地、疾跑且**未处于** `fall_flying`；描述中的“加快滑翔”指一般空中缓降移动，鞘翅滑翔时不会由这条 Power 加速。 |
| `prevent_ranged_weapon_use` | 改为判断本次实际使用的手及物品，补上 Power 条件；原先只检查主手，副手弓可能漏禁，主手持弓时还可能错误阻止副手物品。 |
| 蝙蝠日照 `delay_attribute` | Fabric 将延迟计数初始化为目标延迟，刚获得 Power 时若已暴露于日照会立即生效；Forge 原先仍等待 100 tick。已同步初始化与客户端亮度刷新。 |
| 阳光与亮度条件 | `apoli:exposed_to_sun` 改为在实体碰撞箱底部位置检查白天、雨、亮度和天空；`apoli:brightness` 改为在眼部位置取世界的光照相关亮度，按 Apoli 对应条件源码处理。影响蝙蝠日照惩罚和暗处增益，也影响其他形态。 |

其余分配到蝙蝠的 Power 已找到 Forge 对应入口：条件属性、装备限制、伤害修饰、跳跃/滑翔、饮食、声音、持续本能、方块贴附、矿爪、TAN 兼容。这里仅确认入口和数据对应，不宣称每项在游戏内已达到逐 tick 一致。

仍需游戏内重点观察：末阶贴附后的碰撞与客户端位置、冲刺飞行的状态切换、缓降与滑翔/潜行组合、夜视强度、采收与命中后的物品动作，以及安装 Tough As Nails 时的兼容 Power。本次只运行 `gradlew compileJava --offline`，编译成功；未启动游戏逐项触发。

## 末阶图鉴文字逐句核对

| 图鉴描述 | 对应实现 |
| --- | --- |
| 空手右键侧面攀附、右键底面倒挂 | `form_bat_3_block_attach`；`attach_condition` 接受空主手或主手持钻石矿爪。 |
| 蹬墙滑翔、倒挂回血 | 贴附状态下跳跃解除并给水平/竖直速度；底面贴附每 40 tick 恢复 1 点生命值。 |
| 疾跑跳跃前冲、空中疾跑加速 | `form_bat_3_jump_boost` 跳跃时加前向速度；`form_bat_3_sky_speed` 在离地且非鞘翅状态下持续加速。 |
| 夜视 | `bat_vision` 在眼睛不浸水时以 0.4 强度渲染。 |
| 近战、空手挖掘及收获 | `form_bat_3_damage_up_when_no_sun`、`barehand_digging_speed_up`、`always_harvest`；近战加成要求不暴露于日照。 |
| 高跳、缓降、鞘翅 | `form_bat_3_jump_high`、`slow_falling`、`elytra_no_render`。缓降条件排除部分潜行/鞘翅组合。 |
| 非肉类食物收益 | `form_vegetarian_food_up` 配合 `form_vegetarian`。 |
