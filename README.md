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

Forge 迁移阶段暂时只保留 GeckoLib；Apoli、Calio、Trinkets、Curios、JEI 和其他联动库不作为当前核心依赖。
