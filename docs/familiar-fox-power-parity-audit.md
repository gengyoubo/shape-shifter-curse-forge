# Familiar Fox Power audit (Fabric 1.20.1)

The four form assignments and all 66 `form_familiar_fox*.json` Power definitions match the Fabric reference structurally. This audit follows their Forge execution paths, not just the data files.

| Power family | Fabric trigger / behavior | Forge path and result |
| --- | --- | --- |
| `simple_looting`, `attribute`, `conditioned_attribute`, `in_water_speed_modifier`, `modify_break_speed`, `modify_jump`, `modify_exhaustion`, `modify_food` | Vanilla attribute, travel, loot, food, and jump hooks | Existing Forge mixins and event bridges cover these types. |
| `self_action_on_hit`, `action_on_hit`, `self_action_when_hit` | Hit and damage hooks | Existing Forge hit hooks execute their configured actions. |
| `action_on_entity_use` | Main-hand entity interaction; bi-entity and held-item actions | Fixed the missing `bientity_action`/`held_item_action` execution and `hands` filter. |
| `item_on_item` | Primary inventory click: cursor stack onto slot stack | Replaced the main-hand/right-click/off-hand approximation with an inventory stack-click Mixin. Fox's six recipes use the supported `consume` actions and command result. |
| `action_on_item_use`, `modify_projectile_damage`, `resource`, `cooldown` | Instant item use, fireball launch, projectile hit | Existing action and resource hooks are present. Fixed fireball speed/divergence, projectile particles, and paper-fireball explosion center. |
| `mana_type_power`, `has_mana_percent`, `apply_effect`, `action_over_time` | Fox mana starts at zero, max 100, cursed-moon regeneration 0.02/tick; low-mana effects | Fixed mana maximum/initial value/regeneration, percentage condition, and effect activation/cleanup cadence. |
| `attract_by_entity`, `entity_glow`, `particle` | Continuous target attraction and visuals | Attraction search origin now matches Fabric. Forge's glow is a server entity flag and can be visible to other players; Fabric's viewer-specific glow behavior still needs runtime parity work. |
| `effect_immunity`, `optional_effect_immunity`, `add_sustained_instinct`, `tan_form_temperature_modifier` | Status immunity, instinct, TAN integration | Existing Forge handlers are connected to the corresponding definitions. |
| `active_self` | Fire ring and manual fox sound | Existing active-key dispatcher handles these definitions and cooldown conditions. |

`compileJava --offline` succeeds, and the new inventory-click Mixin appears in the generated refmap. The audit does not establish in-game parity for owner-specific glow, packet timing on inventory clicks, or interactions with other mods.
