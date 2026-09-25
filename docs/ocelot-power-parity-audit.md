# Ocelot form power parity audit

Reference: Fabric 1.20.1 source, `origins/form_ocelot_0` through `form_ocelot_3`, their assigned Power JSON, and Fabric's cat, ocelot, and creeper mixins.

All four stages assign the same Power IDs in Forge and Fabric (12, 23, 32, and 37 assignments respectively). Every assigned Power definition exists in Forge. The three differing JSON definitions were the far-jump actions: Forge used `space:local_horizontal` where Fabric uses `space:local`; these now match. The shared `add_velocity` local-space transform now includes pitch and the vertical result.

`velvet_paws` uses `apoli:prevent_game_event` for `minecraft:step`. Forge had muted the audible footstep instead. Forge now suppresses the emitted step game event on the server and leaves the sound to its separate `no_step_sound` Power.

Fabric changes vanilla cat and ocelot flee-goal predicates for `cat_friendly`, and adds a player-flee goal to creepers for `scare_creepers`. Forge now applies those goal changes. Its existing target-change event remains for immediate target cancellation.

Compilation passed with `gradlew compileJava --offline`. In-game movement, vibration, and AI behavior still require manual confirmation; this audit does not claim runtime verification.
