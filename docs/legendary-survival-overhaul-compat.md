# Legendary Survival Overhaul 兼容

对应 [issue #7](https://github.com/gengyoubo/shape-shifter-curse-forge/issues/7)。

- `shape-shifter-curse:axolotl_0` 至 `axolotl_3` 四个阶段接触水或气泡柱时，在服务端获得 LSO 内置的 `legendarysurvivaloverhaul:temperature_immunity`，持续 40 tick（正常运行时 2 秒），隐藏粒子和图标。
- 通过公开的 `SscPowerApi.currentFormId(player)` 判断形态。普通人形及其他形态不会获得此效果；雨水不触发。
- 效果到期后，只要仍符合条件，就在玩家 tick 的 END 阶段、LSO 的普通优先级温度处理前重新给予。离水、变换形态或死亡后停止给予，已有短效果自然到期，最多保留 40 tick。
- 玩家已经有同类效果时完全跳过：不改持续时间、等级、粒子、图标或隐藏效果。其他来源在 SSC 效果期间给予的效果仍按原版合并规则处理；SSC 不主动删除同类效果，也不存储或恢复过期快照。
- 兼容层仅按注册 ID 读取原版 `MobEffect`，不链接 LSO 类、不反射 SSC 字段、不设置或钳制体温。没有安装 LSO 或没有注册该效果时直接跳过，无新增必装依赖。

LSO 的 [效果注册表](https://github.com/sfiomn/LegendarySurvivalOverhaul/blob/1.20.1/src/main/java/sfiomn/legendarysurvivaloverhaul/registry/MobEffectRegistry.java) 定义了此效果；体温变化及免疫行为由 LSO 自身负责。

验证记录：`gradlew.bat build --offline` 通过。真实 Forge 服务端加载 LSO `1.20.1-2.4.7`，使用测试玩家控制水中状态并推进原版药水计时，通过 284 项断言，覆盖四阶段、其他形态、自然到期、连续续接、死亡以及外部效果和隐藏效果链。也验证了 LSO 自身的寒冷／炎热免疫判断能够识别给予的效果。未安装 LSO 的独立服务端启动和四阶段检查也通过（6 项断言）。测试源和测试模组位于忽略的 `build/` 中，未打入发布 JAR。

游戏内人工验收尚未完成：安装 LSO 后分别切换四个美西螈阶段，在寒冷和炎热环境入水、离水，再检查其他形态、雨水、气泡柱；用药水或 `/effect` 给予更长、较短、更高级或无限时长的同类效果，确认 SSC 不改变它们。
