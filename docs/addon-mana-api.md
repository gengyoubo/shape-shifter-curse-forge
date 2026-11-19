# SSC Forge 法力接口

附属通过 `net.onixary.shapeShifterCurseForge.api.SscManaApi` 访问玩家当前激活的
SSC 法力池。接口随主模组发布，依赖配置见 [Power API](addon-power-api.md#dependency)。
所有方法接收 `ServerPlayer`，在服务器线程调用；写入会限制在 `0..maximumMana`
并同步给该玩家的客户端。没有激活法力类 Power 时，类型为空、数值为零、写入无效。

| 方法 | 用途 |
| --- | --- |
| `hasManaPool(player)` | 是否有激活的法力池 |
| `manaType(player)` | 当前法力类型，返回 `Optional<ResourceLocation>` |
| `mana(player)` / `maximumMana(player)` | 当前值 / 包含能力与饰品加成的上限 |
| `manaPercent(player)` | 当前值与上限之比；无法力池时为零 |
| `hasMana(player, amount)` | 是否足够支付指定数量 |
| `consumeMana(player, amount)` | 足够时扣除并返回 `true`；不足或无法力池时返回 `false` |
| `gainMana(player, amount)` | 请求回复法力，经过回复事件 |
| `setMana(player, value)` | 直接设置数值，绕过回复事件，可用于退款或管理操作 |

数量必须是有限的非负浮点数，否则抛出 `IllegalArgumentException`。
`setMana` 允许有限的负数，按零处理；超过上限的值按上限处理。

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
