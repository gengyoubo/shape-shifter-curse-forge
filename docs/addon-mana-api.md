# SSC Forge 法力接口

附属通过 `net.onixary.shapeShifterCurseForge.api.SscManaApi` 访问玩家当前激活的
SSC 法力池。接口随主模组发布，依赖配置见 [Power API](addon-power-api.md#dependency)。
所有方法接收 `ServerPlayer`，在服务器线程调用；写入会限制在 `0..maximumMana`
并同步给该玩家的客户端。没有激活法力类 Power 时，类型为空、数值为零、写入无效。

| 方法 | 用途 |
| --- | --- |
| `hasManaPool(player)` | 是否有激活的法力池 |
| `manaType(player)` | 当前法力类型，返回 `Optional<ResourceLocation>` |
| `mana(player)` / `maximumMana(player)` | 当前有效值 / 包含内置加成与附属上限事件的上限 |
| `manaPercent(player)` | 当前值与上限之比；无法力池时为零 |
| `hasMana(player, amount)` | 是否足够支付指定数量 |
| `consumeMana(player, amount)` | 足够时扣除并返回 `true`；不足或无法力池时返回 `false` |
| `gainMana(player, amount)` | 请求回复法力，经过回复事件 |
| `setMana(player, value)` | 直接设置数值，绕过回复事件，可用于退款或管理操作 |
| `synchronizeMana(player)` | 立即重新计算上限、裁剪超额法力并同步 HUD |

数量必须是有限的非负浮点数，否则抛出 `IllegalArgumentException`。
`setMana` 允许有限的负数，按零处理；超过上限的值按上限处理。

## 按条件修改法力上限

`SscManaMaximumEvent` 在服务器计算当前激活法力池上限时发布到 **Forge 事件总线**。
`getOriginalMaximum()` 是包含内置 `mana_attribute.max_mana_modifier.add` 的上限；
`getMaximum()` / `setMaximum(float)` 用于读取和修改经过前序监听器处理的上限。
可以叠加、乘算或直接替换，修改值必须有限，全部监听器执行后最终上限至少为 `1`。
无法力池时不发布此事件，`SscManaApi.maximumMana` 仍返回零。

附属的 `ssc_addon:conditional_mana_attribute` 可以在监听器中遍历玩家拥有的该类型
Power，判断各自条件，并把 `max_mana_modifier` 的结果应用到事件。下面演示单项
条件加成；`meetsAddonCondition` 需替换为附属的实际条件判断：

```java
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.onixary.shapeShifterCurseForge.api.SscManaMaximumEvent;

// 将此监听器实例注册到 MinecraftForge.EVENT_BUS。
public final class ConditionalManaHandler {
    @SubscribeEvent
    public void onManaMaximum(SscManaMaximumEvent event) {
        if (meetsAddonCondition(event.getEntity())) {
            event.setMaximum(event.getMaximum() + 25.0F);
        }
    }
}
```

例如内置上限为 `125` 时，以上条件成立得到 `150`，失效后恢复为 `125`。
多个 Power 应逐项基于当前值计算，避免用 `getOriginalMaximum()` 覆盖其他 Power
或监听器的结果；需要规定监听器之间的加算/乘算顺序时，使用 Forge 事件优先级。
SSC 不会自动解释附属命名空间的 Power，附属仍负责类型注册、条件与修饰符语义。

上限事件按查询重新计算，不累积保存修改量；条件变化后，下次查询立即使用新上限。
服务器每个玩家 tick 末尾会检查当前法力池，仅在类型、数值或上限变化时同步 HUD。
降低上限会裁剪超出的已存储法力，提高上限不会自动增加当前法力。
`mana`、支付判断、回复限制、百分比、变形和重生补满均使用修改后的上限。
需要立即裁剪并同步时可调用 `SscManaApi.synchronizeMana(player)`。

监听器只应计算数值，不要修改玩家状态或在回调中调用 `synchronizeMana`、`setMana`、
`gainMana`。同一玩家在上限事件中再次查询上限或法力百分比时，内层查询使用 SSC
内置上限，不再发布事件，避免条件判断递归；这类条件不依赖正在计算的最终上限。

## 在通用法阵充能期间暂停回复

`SscManaRecoveryEvent` 在法力增加前同步发布到 **Forge 事件总线**。
附属可以在回调中查询自己的充能状态并取消事件，无需在充能结束时恢复一个全局开关。
下面的 `isChargingGenericCircle` 是附属自己的状态查询方法，需替换为实际实现：

```java
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.onixary.shapeShifterCurseForge.api.SscManaRecoveryEvent;

// 将此监听器实例注册到 MinecraftForge.EVENT_BUS。
public final class CircleManaHandler {
    @SubscribeEvent
    public void onManaRecovery(SscManaRecoveryEvent event) {
        if (isChargingGenericCircle(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
```

事件包含玩家、`getManaType()`、请求的 `getOriginalAmount()` 和当前的 `getAmount()`。
需要调整回复速度时，可以调用 `event.setAmount(event.getAmount() * 0.5F)`；
设置为零也会阻止本次回复。调整后的数量同样必须有限且非负，最终仍受法力上限限制。
监听器应修改事件，避免在回调中调用 `gainMana` 导致递归发布事件。

取消事件覆盖 SSC 内置的月夜使魔狐回复、蛛丝自然回复及其技能树升级、所有
`shape-shifter-curse:gain_mana` 动作、储能体、喂食效果，以及附属调用的
`SscManaApi.gainMana`。此接口不控制附属自行存储的能量池，附属仍需自行暂停它们。

扣除法力、直接 `setMana`、变形/重生补满和蛛丝钩爪退款不属于回复事件。
直接修改 `PlayerFormData.setManaPool` 也不会触发事件或自动同步 HUD；附属应使用上述接口。
