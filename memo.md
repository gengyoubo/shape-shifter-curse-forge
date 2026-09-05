迁移内容：
| 项目                              | 当前判断                            | 影响                                     |
| ------------------------------- | ------------------------------- | -------------------------------------- |
| `continuous=true` 主动能力          | 🔴 明确不完整                        | 有些能力按住键不会在条件后来满足时触发                    |
| `origins:toggle` / 潜行边缘切换       | 🔴 明确缺一段                        | 豹猫 `active_skill_6` 不能完整工作             |
| 饰品 `accessory_power` 动态增删 Power | 🔴 高概率未完整实现                     | 饰品能装备，但部分实际能力可能不会获得                    |
| 部分特殊 Power type                 | 🟡 需继续逐项审计                      | JSON 存在，但 Forge handler 可能没有           |
| 部分 Condition / Action           | 🟡 存在静默误判风险                     | 未支持 condition 会被当 `true`，action 会直接不执行 |
| 客户端 tick 型能力                    | 🟡 部分已转服务端，仍需核对                 | ItemStore / CustomEdible 等可能行为不同       |
| 动态形态/数据包形态                      | 🟡 Forge 有自己的 Form 系统，但还没证明完全等价 | 外部 Form Pack 兼容性                       |
| 少量视觉特效/渲染辅助                     | 🟡 需对照                          | gameplay 不一定受影响                        |
| Advancement                     | 🔴 之前确认缺                        | 只影响进度，不是玩法主体                           |
