# SSC Forge Java 注册指南

本指南面向 SSC Forge 的附属模组作者。它说明如何注册 Java Form、Power、Condition、
Action 和 Evolution，并约定一个清晰的组织方式：

```text
ModForms / ModPowers / ModConditions / ModActions / ModEvolutions
                 ↓
         各定义类自己的 init()
                 ↓
           @Mod 模组入口统一 init
```

SSC 的 Java 注册对象都是**定义**，不是某个玩家当前变身的实例。玩家的阶段、冷却、
资源值和临时能力状态必须放在每玩家运行时层（Forge capability/attachment 或自己的
服务），绝不能保存到 `SscForm` 或 `SscPower` 的字段中。

## 1. 基本规则

每个 `SscRegistrar` 固定代表一个 addon namespace。一个 addon 可以按 `ModForms`、
`ModPowers` 等职责建立多个 registrar，但它们都应使用同一个 addon namespace：

```java
private static final SscRegistrar REGISTRAR =
        SscJavaRegistries.registrar(MyAddon.MOD_ID);
```

- `form()` 注册出的 Form ID 必须属于该 namespace；跨 namespace 会立即报错。
- `power("dash", ...)`、`condition("is_raining", ...)`、`action("jump", ...)` 自动使用
  registrar 的 namespace，分别得到 `my_addon:dash` 等 ID。
- 先声明、后 `init()`：Supplier 直到 `init()` 才真正构造并注册对象。
- `init()` 成功后可重复调用，结果是 no-op；失败后 registrar 进入 `FAILED`，**不可重试**。
  SSC 不做全局 registry rollback，重试会掩盖首次失败并制造重复 ID。
- `SscRegistryObject<T>` 可放在 `static final` 字段中，但只有 `init()` 成功后才能调用
  `get()` 或 `id()`。

## 2. 推荐目录布局

```text
src/main/java/com/example/myaddon/
├─ MyAddon.java                 # @Mod 入口，仅调用各 Mod*.init()
├─ init/
│  ├─ ModForms.java
│  ├─ ModPowers.java
│  ├─ ModConditions.java
│  ├─ ModActions.java
│  └─ ModEvolutions.java
└─ form/
   ├─ CatgirlForm.java
   └─ ...
```

不必机械地建立空文件：没有 Java Action，就不要建 `ModActions`。但不要把所有注册、
Form 实现和模组入口混进一个类。

## 3. 注册 Form

`ModForms` 是 Form 的唯一登记分支。每个 Form 自己持有稳定的 `TYPE` handle，
`ModForms.init()` 负责先触发每个 Form 的声明，再提交共享 registrar。

```java
public final class ModForms {
    private static final SscRegistrar REGISTRAR =
            SscJavaRegistries.registrar(MyAddon.MOD_ID);

    private ModForms() {}

    public static <T extends SscForm> SscRegistryObject<T> register(Supplier<T> factory) {
        return REGISTRAR.form(factory);
    }

    public static void init() {
        CatgirlForm.init();
        // FutureWolfForm.init();
        REGISTRAR.init();
    }
}
```

普通 stage Form 只提供 family 内部 ID。stage 从 1 起算，实际资源后缀从 0 起算：
`catgirl` 的 stage 1 是 `my_addon:catgirl_0`。默认最大 stage 为 4。

```java
public final class CatgirlForm extends SscForm {
    public static final SscRegistryObject<CatgirlForm> TYPE =
            ModForms.register(CatgirlForm::new);

    /** 由 ModForms.init() 调用，触发 TYPE 的静态声明。 */
    public static void init() {}

    public CatgirlForm() {
        super(ResourceLocation.fromNamespaceAndPath(MyAddon.MOD_ID, "catgirl"));
    }

    @Override
    protected int stage() {
        return 1;
    }

    @Override
    protected void configure(FormProperties properties) {
        properties.weight(1)
                .bodyType(FormBodyType.NORMAL)
                .scale(1.0F, 1.0F, 1.0F)
                .specialForm();
    }

    @Override
    protected void powers(PowerRegistrar powers) {
        powers.add(SscPowers.NIGHT_VISION); // 数据定义 Power 的引用
    }
}
```

`configure()` 和 `Builder` 共享同一套语义规则。优先使用 `finalForm()`、
`specialForm()`、`inhibitorImmune()` 等方法，不要将内部 flag 字符串作为公开 API。
游戏行为优先通过 `powers.add(...)` 组合，不要持续新增 Form 属性字段。

### 命名分支 Form

常规进化 stage 不需要重复写 ID。只有**分支**必须显式命名，并使用
`SscForm.branch(familyId, branchName)`；例如它会生成 `my_addon:catgirl_marble`：

```java
ResourceLocation family = ResourceLocation.fromNamespaceAndPath(MyAddon.MOD_ID, "catgirl");
SscForm marble = SscForm.branch(family, "marble")
        .stage(4)
        .specialForm()
        .build();
```

Builder Form 没有 `configure()`/`powers()` hook。需要继承共同属性或 Power 时，使用
class Form；只需一次性、无 hook 的命名分支时才使用 Builder。

## 4. 注册 Power

Java `SscPower` 在玩家当前 Form 拥有该 Power 时，每个**服务端玩家 tick**执行一次。
它是共享定义，不能存玩家 cooldown。

```java
public final class ModPowers {
    private static final SscRegistrar REGISTRAR =
            SscJavaRegistries.registrar(MyAddon.MOD_ID);

    public static final SscRegistryObject<SscPower> REGENERATION =
            REGISTRAR.power("regeneration", player -> {
                if (player.tickCount % 40 == 0) {
                    player.heal(1.0F);
                }
            });

    private ModPowers() {}

    public static void init() {
        REGISTRAR.init();
    }
}
```

若 Form 要引用该 Java Power，先初始化 `ModPowers`，再初始化 `ModForms`：

```java
@Override
protected void powers(PowerRegistrar powers) {
    powers.add(ModPowers.REGENERATION.id());
}
```

也可引用数据定义 Power：`powers.add(ResourceLocation)` 不要求该 Power 在 Java 注册。
适合复用 SSC 自带的 `SscPowers` 或 datapack 在 `/reload` 时加载的 Power。

## 5. 注册 Condition 与 Action

Java Condition 和 Action 是给 Power / JSON `type` 使用的实现类型。回调中的 `data`
是完整 JSON 对象，包含 `type` 和你的自定义字段。

```java
public final class ModConditions {
    private static final SscRegistrar REGISTRAR =
            SscJavaRegistries.registrar(MyAddon.MOD_ID);

    public static final SscRegistryObject<SscCondition> IS_RAINING =
            REGISTRAR.condition("is_raining", (player, target, data) ->
                    player.level().isRaining());

    private ModConditions() {}
    public static void init() { REGISTRAR.init(); }
}
```

```java
public final class ModActions {
    private static final SscRegistrar REGISTRAR =
            SscJavaRegistries.registrar(MyAddon.MOD_ID);

    public static final SscRegistryObject<SscAction> SHORT_HOP =
            REGISTRAR.action("short_hop", (player, target, data) ->
                    target.push(0.0D, 0.35D, 0.0D));

    private ModActions() {}
    public static void init() { REGISTRAR.init(); }
}
```

`SscCondition` 的 `target` 允许为 `null`；`SscAction` 的 `target` 一定非空。SSC 会在
Condition 返回后统一处理 JSON 的 `inverted`，因此实现本身不要再次反转它。

## 6. 注册 Evolution

Evolution 只描述 Form ID 之间的有向边，不存属性、Power、模型、继承或“自动选目标”的
策略。一个 family 负责拥有自己的图。

```java
public final class CatgirlForms {
    private static final ResourceLocation FAMILY =
            ResourceLocation.fromNamespaceAndPath(MyAddon.MOD_ID, "catgirl");

    public static final Evolution EVOLUTION = Evolution.builder()
            .chain(
                    SscForm.stageId(FAMILY, 1),
                    SscForm.stageId(FAMILY, 2),
                    SscForm.stageId(FAMILY, 3),
                    SscForm.stageId(FAMILY, 4))
            .branch(SscForm.stageId(FAMILY, 3),
                    ResourceLocation.fromNamespaceAndPath(MyAddon.MOD_ID, "catgirl_marble"))
            .build();
}

public final class ModEvolutions {
    private static final SscRegistrar REGISTRAR =
            SscJavaRegistries.registrar(MyAddon.MOD_ID);

    static {
        REGISTRAR.evolution(() -> CatgirlForms.EVOLUTION);
    }

    private ModEvolutions() {}

    public static void init() {
        REGISTRAR.init();
    }
}
```

所有边的两端都必须在首次 Form 解析前存在、属于同一个 group，且目标 stage 恰好比来源
高一级。分支没有命名约定时不要创建；普通 stage ID 应继续由 `stageId()` 生成。

## 7. 模组入口的初始化顺序

在 Forge 的 `@Mod` 构造器中调用一次。Power / Condition / Action 应在 Form 前提交，
Evolution 放在 Form 后最容易阅读：

```java
@Mod(MyAddon.MOD_ID)
public final class MyAddon {
    public static final String MOD_ID = "my_addon";

    public MyAddon() {
        ModConditions.init();
        ModActions.init();
        ModPowers.init();
        ModForms.init();
        ModEvolutions.init();
    }
}
```

不要在 `FMLCommonSetupEvent`、玩家登录事件、tick 或命令执行时才注册定义；此时 Form
解析或客户端渲染可能已经开始。也不要用反射、注解扫描或按需构造取代明确的 `init()`。

## 8. Forge Java 注册与原 Fabric 内容的差异

| 主题 | Forge 当前 API | 原 Fabric / 迁移内容 |
| --- | --- | --- |
| Java 扩展注册 | `SscRegistrar` + `SscRegistryObject`，由 `@Mod` 构造器明确提交 | 当前仓库未附带 Fabric 源码，也没有可承诺完全等价的 Fabric Java 注册 API |
| Form/Power 数据 | Java 定义可与 JSON Power 引用混用 | 原内容主要为 Origins/Apoli 风格 JSON；Forge 兼容层仍会读取这类数据，并响应 `/reload` |
| 注册时机 | 模组加载期一次；成功后幂等，失败后不可重试 | 数据包资源在 reload 生命周期解析，资源修改通常需 `/reload` 或重启 |
| 原版 Registry（物品、方块、实体） | 继续使用 Forge `DeferredRegister` 和 mod event bus | Fabric 采用自己的 Registry/初始化机制；不能把 Forge 注册事件或 handle 直接移植过去 |
| 玩家运行时状态 | 使用 Forge capability/attachment 或附属模组自己的运行时存储 | 不应假设可与 Forge 共享对象；跨平台需各自实现状态存储和同步 |

因此，资源层（Geo、贴图、`ssc_form_model`、可兼容的 Power JSON）尽量保持跨端；Java
注册、事件订阅、玩家状态和网络层则应视为平台实现。`SscJavaRegistries` 的 API 形状是为
未来 Fabric 对应实现预留的，但目前文档只保证 Forge 行为。

## 9. 发布前检查

1. 每个 `SscRegistrar` 只使用一个 namespace；同一 addon 的各 `Mod*` 分支使用该 addon 的 mod id。
2. 每个 `static final` handle 都在相应 `Mod*.init()` 前被对应定义类触发声明。
3. 入口每个 `Mod*.init()` 仅调用一次；不要在事件中重调。
4. Java Power 不存玩家可变状态；每玩家状态进入 capability/attachment。
5. Form 行为用 Power；Form 属性用语义方法；Evolution 只存边。
6. Java 定义 ID、JSON `type`、资源 namespace 与 `mods.toml` 的 mod id 一致。
