# Forge 指令与 Fabric 对照

Forge 现已注册 Fabric 的 `/shape_shifter_curse` 根指令。保留 `/ssc` 作为旧存档、脚本和 Power 诊断指令的兼容入口。

已接入的 Fabric 子指令：

- `set_form`、`transform_to_form`、`set_dynamic_form`、`transform_to_dynamic_form`、`set_sub_form`、`transform_to_sub_form`：参数均为 `<target> <form>`，并按普通、动态、子形态筛选建议和目标。
- `jump_to_next_cursed_moon`、`world_time set|add <time>`。
- `keep_original_skin <value>`、`set_form_color`、`form_color`。`form_color config enable_default_color` 与 Fabric 一样切换客户端的默认配色开关。
- `debug set_form <target> <form>`、`debug clear_player_form_data <target>`、`debug clear_player_skin_data <target>`、`debug clear_player_mana_data <target>`、`debug_attrs <target>`。

尚未注册的 Fabric 子指令依赖 Forge 尚未移植的对应系统：`adjust_feral_item_loc`、`patron_info`、`debug dev_command`、`debug clear_player_minion_data`、`debug su`、`debug reupload_auth_file`。这些名字不能只接一个空处理函数冒充已经移植。

语义差异：Fabric 的 `transform_to_*` 有完整的延迟变身流程；Forge 目前立即切换 Form 并播放变身动画。`set_sub_form` 的建议筛选了子形态，但 Forge 尚无 Fabric `NeedCheckUsableForm` 的完整权限检查。

新增游戏规则 `/gamerule sscKeepFormAfterDeath`，默认 `true`。设为 `false` 后，玩家死亡重生时回到 `shape-shifter-curse:original_before_enable`；跨维度传送不会重置形态。Fabric 源码本身无此规则，其默认行为是保留形态。
