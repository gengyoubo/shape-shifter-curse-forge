# 美西螈 0–3 阶 Power 对照（2026-09-25）

参照 `C:\Users\gengy\Desktop\Shape-Shifter-Curse_Unofficial-Port` 当前 Fabric 源码。四个形态分别分配 8、19、29、48 个 Power，共 66 个不同 ID；分配列表逐项相同。Forge 与 Fabric 的 JSON 唯有两个属性修饰器名称和金苹果显式 `trigger` 曾不同；金苹果已改回 Fabric 的省略形式。

以下“源码对照”只说明已经找到对应执行路径，不代表完成游戏内行为验证。每阶只列该阶首次出现的 ID；之后的阶段复用相同定义。

| 首见阶段 | Power ID（省略 `shape-shifter-curse:`） | 对照结果 |
| --- | --- | --- |
| 0 | `hold_breath` | 水下耗氧 1/4 概率；源码对照 |
| 0 | `form_axolotl_0_new_puff_speed_down` | 条件属性；源码对照 |
| 0 | `form_axolotl_0_swim_speed` | 游泳属性；源码对照 |
| 0 | `form_axolotl_instinct_in_water`, `form_axolotl_instinct_biome` | 持续本能；源码对照 |
| 0 | `form_instinct_use_catalyst`, `form_instinct_use_golden_apple` | 本次修正：未指定触发器按 Fabric 默认 `FINISH`；金苹果不再清空本能 |
| 0 | `form_axolotl_sound_key` | 主动技能按键；源码对照，待游戏内验证 |
| 1 | `aquatic` | Fabric Apoli 的 `SetEntityGroupPower` 注入被注释；Fabric 也不改变 `getMobType()` |
| 1 | `form_axolotl_1_swim_speed`, `breathing_under_water` | 游泳属性、1/101 水下耗氧；源码对照 |
| 1 | `form_raw_fish_food_up`, `form_carnivore` | 本次修正：在 `FoodData.eat` 前统一应用饥饿和饱和度修饰器 |
| 1 | `jump_out_water` | Forge 为避免水中阻尼采用移动结束后发射；效果已存在，但执行时序与 Fabric 并非逐指令一致，需游戏内对照 |
| 1 | `form_axolotl_2_regeneration` | 本次修正：`action_over_time` 以 Power 实例首次 tick 为计时起点 |
| 1 | `form_axolotl_1_new_water_flexibility` | 水中阻尼；源码对照 |
| 1 | `form_disable_head_armor` | 头部装备限制；已存在处理，待游戏内验证 |
| 1 | `form_axolotl_instinct_eat_fish` | 本次修正：食用完成后增加本能 |
| 1 | `form_axolotl_instinct_near_dripleaf` | 方块范围与持续本能；源码对照 |
| 1 | `form_axolotl_instinct_attack_fish` | 本次修正：仅命中鱼类时执行，应用默认 1 tick 冷却 |
| 1 | `form_tan_prevent_dirty_water_thirst` | 仅在安装 Tough As Nails 后生效；未在该组合下验证 |
| 2 | `no_render_arm_when_sneaking` | 第一人称手臂渲染钩子；源码对照，待游戏内验证 |
| 2 | `form_axolotl_2_water_breathing` | 陆地耗氧和水中呼吸；本次补齐 `canBreatheUnderwater()` 返回路径 |
| 2 | `aqua_affinity` | 本次修正：补 `apoli:enchantment` 条件，并让客户端也计算挖掘速度 |
| 2 | `like_water` | 本次修正：在水中 `travel()` 的重力调整处处理，移除原有 tick 速度限制 |
| 2 | `form_axolotl_2_new_oxygen_regeneration` | 本次修正：周期 Power 计时起点 |
| 2 | `form_axolotl_2_dry_speed_down`, `form_axolotl_2_wet_health` | 条件属性；源码对照 |
| 2 | `form_axolotl_2_new_water_flexibility` | 水中阻尼；源码对照 |
| 2 | `form_axolotl_2_water_spurt` | 主动冲刺；执行路径已定位，待游戏内验证 |
| 2 | `form_axolotl_2_falling_protection` | 本次修正：只在 `calculateFallDamage` 中扣保护距离 |
| 2 | `form_axolotl_2_crawling`, `form_axolotl_keep_sneaking_when_head_collide`, `sneaking_speed_up` | 姿态、强制潜行、速度互相依赖；源码路径已定位，待游戏内组合验证 |
| 2 | `air_from_potions`, `air_from_splash_potions` | 本次修正：饮用完成与 Fabric 三个飞溅药水入口 |
| 2 | `form_axolotl_sound`, `form_axolotl_sound_hurt` | 本次修正：周期相位、受伤音效概率和声音播放位置 |
| 2 | `form_tan_axolotl_thirst_in_water` | 仅在安装 Tough As Nails 后生效；未在该组合下验证 |
| 3 | `form_axolotl_3_no_render_arm` | 第一人称手臂渲染钩子；源码对照，待游戏内验证 |
| 3 | `form_axolotl_3_water_breathing` | 陆地耗氧和水中呼吸；同 2 阶 |
| 3 | `form_axolotl_3_slipperiness` | 本次修正：改在原版计算加速度与阻尼前修改摩擦，去掉 Fabric 不存在的钳制 |
| 3 | `form_axolotl_3_sneaking_speed`, `form_axolotl_3_sprinting_speed`, `form_axolotl_3_ground_speed_down` | 条件属性；源码对照，待姿态组合验证 |
| 3 | `form_axolotl_3_sprinting_cost_oxygen`, `form_axolotl_3_particle` | 本次修正：周期相位及粒子的实体尺寸换算 |
| 3 | `form_axolotl_3_sprinting_sneaking_water_explode` | 本次修正：爆炸遮挡、原版公式、护爆击退和实体筛选 |
| 3 | `form_axolotl_3_sprinting_jump`, `form_axolotl_3_sprinting_jump_high` | 跳跃动作和高度；源码路径已定位，待游戏内验证 |
| 3 | `form_axolotl_3_sprinting_attack` | 本次修正：执行 `bientity_action`、目标方向击退、音效/粒子/耗氧、条件及冷却 |
| 3 | `form_axolotl_3_sprint_step_height` | 步高倍率；源码对照，待游戏内验证 |
| 3 | `form_axolotl_3_keep_sneaking_no_air`, `form_axolotl_3_crawling` | 强制潜行和爬行姿态；待客户端与服务端组合验证 |
| 3 | `form_axolotl_3_always_swimming` | 水中冲刺状态与饥饿消耗；源码路径已定位，待游戏内验证 |
| 3 | `water_vision` | 本次修正：沿 Fabric 的亮度、光照图与雾颜色路径渲染，不再给玩家夜视药水效果 |
| 3 | `form_axolotl_2_new_oxygen_health_0`–`form_axolotl_2_new_oxygen_health_9` | 十级氧气区间的条件属性；源码对照 |

验证：`gradlew compileJava --offline` 成功；`runClient --offline` 到达主菜单，启动日志没有 Mixin 注入错误。尚未逐项在游戏内触发上述动作，因此不能宣称四阶 Power 全部正常。
