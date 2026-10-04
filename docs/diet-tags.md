# SSC 饮食标签

三个饮食 JSON 位于 `src/main/resources/data/shape-shifter-curse/tags/items/`：

| 文件 | 标签 ID | 生效范围 |
| --- | --- | --- |
| `diet_vegetarian.json` | `shape-shifter-curse:diet_vegetarian` | 蝙蝠的素食加成、非素食减益和素食限制 |
| `diet_raw_meat.json` | `shape-shifter-curse:diet_raw_meat` | 豹猫、美西螈、阿努比斯狼、蜘蛛的饮食判定，以及豹猫的生肉加成、消化纤维球饰品和进食本能 |
| `diet_raw_fish.json` | `shape-shifter-curse:diet_raw_fish` | 美西螈的生鱼加成和进食本能；同时被生肉饮食标签引用 |

这些是物品标签，使用 Minecraft 的标签 JSON 格式。可以通过数据包覆盖同路径文件，
或在 `values` 中增加物品 ID，然后执行 `/reload`。无需修改 Java 或重新编译代码。

```json
{
  "replace": false,
  "values": [
    "minecraft:apple",
    { "id": "example:vegetable_dish", "required": false }
  ]
}
```

模组物品使用 `required: false`，缺少对应模组时仍可加载标签。
`#namespace:tag` 表示引用另一个标签；避免形成循环引用。

生肉与生鱼饮食标签默认引用已有的 `raw_meat` 和 `raw_fish` 分类，保留原有兼容名单。
生鱼属于生肉饮食，新增到生鱼饮食标签的食物也可用于豹猫生肉饮食。
原有分类仍用于掉落转化等功能，饮食配置可以独立扩展。

饮食判定直接检查上述标签，未列入对应饮食标签的食物按各阶段现有规则受到减益或收益归零。
蝙蝠的素食判定不再自动放行所有非肉类食物或仅带 Vegan Delight 标记的食物。
需要支持的模组食品应加入对应标签。即使一个食物被标记为肉类，只要加入素食饮食标签，
蝙蝠也会按素食接受它。原本没有饮食限制的初始阶段不会新增限制。
素食包括水果、蔬菜、谷物、蛋和乳制品；该标签本身不会把非食物物品变成可食用物品。

原有 `ignore_diet`、`ignore_vc_eat`、变形食物等豁免继续生效。
豹猫、美西螈、阿努比斯狼和蜘蛛的腐肉例外继续保留，蜘蛛的流食囊也继续有效。
蜘蛛终阶段现在也接受生肉饮食标签中的食物。阿努比斯狼的骨头进食能力继续保留。
美西螈和豹猫共享检查生肉饮食标签的 `form_carnivore` 限制；美西螈对生鱼仍有额外加成。
豹猫继续使用已有能力 ID，消化纤维球饰品仍能替换对应限制。
仅生肉或素食有效的限制通常将其他食物的收益归零，而非阻止使用。
