# SSC 饮食标签

三个饮食 JSON 位于 `src/main/resources/data/shape-shifter-curse/tags/items/`：

| 文件 | 标签 ID | 生效范围 |
| --- | --- | --- |
| `diet_vegetarian.json` | `shape-shifter-curse:diet_vegetarian` | 蝙蝠的素食加成、非素食减益和素食限制 |
| `diet_raw_meat.json` | `shape-shifter-curse:diet_raw_meat` | 豹猫、美西螈、阿努比斯狼、蜘蛛的饮食判定，以及豹猫的生肉加成、消化纤维球饰品和进食本能 |
| `diet_raw_fish.json` | `shape-shifter-curse:diet_raw_fish` | 美西螈的生鱼加成和进食本能；同时被生肉饮食标签引用 |

这些是物品标签，使用 Minecraft 的标签 JSON 格式，同时是配方继承的种子。
可以通过数据包覆盖同路径文件，或在 `values` 中增加物品 ID，然后执行 `/reload`。
无需修改 Java 或重新编译代码。

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

饮食判定现在使用“标签种子 + 配方推导”的服务器缓存。
没有分类的食物按各阶段现有规则受到减益或收益归零。
蝙蝠不会自动放行所有非肉类食物或仅带 Vegan Delight 标记的食物。
同一物品同时具有素食和肉／鱼属性时，肉／鱼优先；植物肉等误标物品可通过分类黑名单修正。
原本没有饮食限制的初始阶段不会新增限制。
素食包括水果、蔬菜、谷物、蛋和乳制品；该标签本身不会把非食物物品变成可食用物品。

原有 `ignore_diet`、`ignore_vc_eat`、变形食物等豁免继续生效。
豹猫、美西螈、阿努比斯狼和蜘蛛的腐肉例外继续保留，蜘蛛的流食囊也继续有效。
蜘蛛终阶段现在也接受生肉饮食标签中的食物。阿努比斯狼的骨头进食能力继续保留。
美西螈和豹猫共享检查生肉饮食标签的 `form_carnivore` 限制；美西螈对生鱼仍有额外加成。
豹猫继续使用已有能力 ID，消化纤维球饰品仍能替换对应限制。
仅生肉或素食有效的限制通常将其他食物的收益归零，而非阻止使用。

## 配方继承

默认配置：`data/shape-shifter-curse/diet_inheritance/default.json`。

1. 使用上述三个 SSC 标签、已有肉／鱼标签、Origins 标签和 Forge／Common 食材标签作为种子。
2. 枚举服务器 `RecipeManager` 中能提供产物及完整原料候选的配方。
3. 沿所有配方链传播分类，包含不可直接食用的中间原料，如小麦 → 面粉 → 面条。
4. 开服及 `/reload` 时完整重建；登录和重载时把结果同步给客户端。

SSC 自带 `diet_origins_meat`，复制 [Origins 1.20 默认肉类标签](https://github.com/apace100/origins-fabric/blob/1.20/src/main/resources/data/origins/tags/items/meat.json)
的 19 种原版物品，并可选引用 `#origins:meat`。无需安装 Origins。
[Origins 1.20 默认 ignore_diet](https://github.com/apace100/origins-fabric/blob/1.20/src/main/resources/data/origins/tags/items/ignore_diet.json)
为空，所以直接读取外部标签和 SSC 已有豁免，不复制一份空名单。
这份 Origins 默认名单本身不是完整的模组食物兼容表；跨模组扩展由 SSC 已有名单、公共标签及配方传播共同完成。

### 基础种子兼容

优先接收食材语义标签；已有最终食物名单暂时作为兼容兜底保留。
默认配置增加了以下入口，Forge 和 Common 的单／复数常见别名会一起尝试：

| 类型 | 入口示例 |
| --- | --- |
| 植物食物集合 | `createaddition:plant_foods` |
| 浆果、香草、香料 | `forge:berries`、`forge:herbs`、`forge:spices`、胡椒／香草／姜／肉豆蔻等基础标签 |
| 茶、咖啡、可可 | `forge:tea_leaves`、`forge:coffee_beans`、`c:cacao_butter` |
| 谷物、豆类 | 米、玉米、豆类、豆科、大豆、黑豆、青豆标签 |
| 面粉、面团、面条、面包 | `forge:pasta`、`forge:noodles`、`c:flour`、`c:dough`、`c:pasta`、基础面包标签 |
| 植物油、醋、酱油 | `forge:olive_oils`、`c:vegetable_oil`、醋／酱油标签 |
| 蛋、乳及植物乳原料 | `forge:cooked_eggs`、`c:milk`、黄油、奶酪、奶油、酸奶、豆乳标签 |
| 甜味中间原料 | 果酱、糖浆、糖蜜标签 |
| 肉类别名 | `forge:raw_meat`、`c:raw_meat`，以及生／熟牛肉、猪肉、鸡肉、羊肉、培根标签 |
| 海鲜 | `forge:seafood`、`c:seafood`、贝类／甲壳类标签，归入 FISH |
| 中性调料／容器 | `forge:salts`、`c:salt`、`forge:buckets/water`，继续保留碗、瓶和刀具 |

这些是可选入口：安装的模组或数据包必须实际提供对应物品标签，别名本身不会创建食材。
现有 FISH 名单已经包含 `tide:crystal_shrimp` 及 Ocean's Delight 的鱿鱼触手，
因此新增海鲜入口沿用“鱼／水生动物食材”口径，不引入新的 SEAFOOD 分类。

来源核对：

- [Farmer's Delight 1.20 CommonTags](https://github.com/vectorwing/FarmersDelight/blob/1.20/src/main/java/vectorwing/farmersdelight/common/tag/CommonTags.java)
  定义了浆果、基础面包、面条、蛋等标签，肉类原料聚合标签使用单数 `raw_meat`。
- [Create Crafts & Additions 植物食物标签](https://github.com/mrh0/createaddition/blob/1.20.1/src/main/resources/data/createaddition/tags/items/plant_foods.json)
  及 [Farmer's Delight 对该标签的扩展](https://github.com/vectorwing/FarmersDelight/blob/1.20/src/generated/resources/data/createaddition/tags/items/plant_foods.json)。
- [Croptopia 橄榄油标签](https://github.com/ExcessiveAmountsOfZombies/Croptopia/blob/v3_1.20/common/src/main/generated/dependents/platform/tags/items/olive_oils.json)
  与该目录的茶叶、咖啡豆、黄油、奶酪、酱油、果酱等标签；
  [Forge 资源处理](https://github.com/ExcessiveAmountsOfZombies/Croptopia/blob/v3_1.20/forge/build.gradle)
  把 `platform` 占位目录及 `${dependent}` 替换为 `forge`。
- [Create: Food 1.20.1 的 Common 标签目录](https://github.com/AverageAnime/create-food/tree/1.20.1/src/main/resources/data/c/tags/items)
  包含单数形式的面粉、面团、奶、黄油、植物油、咖啡豆等入口。

只选含义明确的基础原料标签。通用 `oil`、`cooking_oil` 可能包含动物脂肪或工业原料；
通用 `jelly`／`gelatin` 不保证为植物来源，所以不直接赋予素食属性。
`diet:*` 和 `nourish:*` 的营养分类也不等于素食分类：
例如 [Diet 的 grains 标签](https://github.com/TheIllusiveC4/Diet/blob/1.20.x/forge/src/generated/resources/data/diet/tags/items/grains.json)
包含虾炒饭、金蜜火腿、烤鸡，不能整组作为素食种子。

内置蛋类修正：SSC 素食包含蛋，但
[Farmer's Delight 的 Origins meat 兼容标签](https://github.com/vectorwing/FarmersDelight/blob/1.20/src/generated/resources/data/origins/tags/items/meat.json)
把煎蛋列为肉类。因此默认肉类黑名单排除蛋类标签和 `farmersdelight:fried_egg`，
让煎蛋及其衍生料理按蛋奶素口径推导。培根煎蛋等混合料理仍会从真正的肉类原料继承 MEAT。

### 推导规则

- 任一原料或候选原料有肉／鱼属性，产物继承对应属性；混合料理可以同时含肉和鱼。
  食物自身的 `FoodProperties.isMeat` 也作为肉类种子兜底，可用分类黑名单修正。
- 素食要求所有非中性原料、所有候选物品都可确认为素食，且至少有一个素食原料。
  未知原料不会被默认当作素食。
- 碗、瓶、水、盐、带标签的刀具等为中性原料，不妨碍素食判定，也不会被反向配方染上饮食分类。
- 饮食豁免只作用于指定物品，不传播给料理。金苹果做出的新食物不会自动免除限制。
- 同一产物有多个配方时，取全部配方可能含有的肉／鱼属性。因此既能用肉也能用植物制作的同一物品，
  静态缓存会按可能含肉处理；不记录这一个物品堆实际使用过的原料或 NBT。
- 循环配方可处理；无种子的循环不会凭空生成分类。不存在固定层数限制。
- 无固定产物、无可枚举原料或原料标签为空的配方跳过。特殊机器必须通过标准配方接口暴露这些信息；
  不保证覆盖每个食物模组、动态配方或直接生成的无配方食物。

**行为变化：** `diet_raw_meat`／`diet_raw_fish` 保留原有 ID，但现在是肉／鱼来源饮食入口。
烹饪或进一步合成不会消除这些来源属性；熟肉、鱼汤等也能用于相应饮食、加成和进食本能。
这套分类不区分生熟。旧 `raw_meat`／`raw_fish` 标签及其掉落转换用途不受配方缓存扩展影响。
推导结果只接入 SSC 的饮食条件，不向 Minecraft 或其他模组的真实物品标签写入新增物品。

### 数据包修正

新增 `data/<命名空间>/diet_inheritance/<文件名>.json`，例如：

```json
{
  "replace": false,
  "seeds": {
    "vegetarian": ["#example:plants"],
    "meat": ["#example:meat_ingredients"],
    "fish": ["#example:seafood"]
  },
  "whitelist": {
    "vegetarian": ["example:isolated_salad", "example:vegan_burger"],
    "meat": ["example:alien_meat_bar"]
  },
  "blacklist": {
    "meat": ["example:vegan_burger"],
    "fish": ["example:vegan_burger"]
  },
  "food_blacklist": ["example:misclassified_food"],
  "recipe_blacklist": ["example:bad_recipe", "example:recycling/*"],
  "neutral_ingredients": ["example:spoon"]
}
```

- `seeds`：来源集合；`whitelist`：补充孤立物品，二者都会继续向配方产物传播。
- 分类支持 `vegetarian`、`meat`、`fish`、`ignore_diet`；物品选择器支持物品 ID 和 `#物品标签`。
  未安装模组的物品／标签会忽略，不要求必选依赖。
- `blacklist`：禁止指定物品拥有某个分类，优先于种子、白名单及配方推导。
  植物肉修正通常需要同时排除 `meat` 和 `fish`，再加入素食白名单。
- `food_blacklist`：清除物品的全部分类，也截断从它传播的路径。此处“黑名单”不会直接禁止玩家进食；
  腐肉、流食囊等能力中显式写出的例外及 `ignore_vc_eat` 等其他豁免仍按原能力执行。
- `recipe_blacklist`：排除整条配方，可用 `*` 匹配配方 ID；只作用于本系统，不删除游戏配方。
- `neutral_ingredients`：中性原料，优先于分类；不要把肉、鱼、牛奶等真正食材列为中性。
- 默认配置先处理，其余资源按完整 ID 排序合并；`replace: true` 会清空此前合并的继承配置。
  相同资源路径仍按数据包优先级覆盖。缺省字段等同于空列表。
- 格式错误的文件会记录错误并跳过；没有有效分类的食物保持未分类。

### 检查

管理员可用 `/ssc diet inspect minecraft:cooked_beef` 查询最终分类；
`/ssc diet stats` 查看分类物品、有效配方和跳过配方数量。
统计包含中间原料，不等于可食用食物数。
修改配置后执行 `/reload`，缓存会完全替换，删掉旧数据留下的分类。

开发回归检查：`gradlew.bat testDietInheritance --offline`。
