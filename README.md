# Shape Shifter Curse

当前根目录是原生 Forge 1.20.1 迁移工程。

## 工程布局

- 根目录：Forge 1.20.1，作为后续主要开发目标。
- `fabric/`：原 Fabric 工程，仅作为功能和资源对照，暂不删除。

## 构建

Forge：

```text
gradlew.bat build
```

Fabric 对照工程：

```text
fabric\\gradlew.bat -p fabric build
```

## Forge 运行依赖

- GeckoLib 4.8.4+：变体 Geo 模型渲染。

原 Fabric 工程的 Player Animation Lib 行为由 Forge 内置的轻量兼容层实现：只保留
`player_animation` JSON 所需的坐标转换、默认骨骼 pivot 合成与 `PlayerModel` 姿势写回，
不会将 Player Animation Lib 作为运行依赖引入。

Forge 端不依赖 AzureLib 或 Player Animation Lib。Apoli、Calio、Trinkets、Curios、JEI 和其他联动库也不属于当前核心依赖。
