# Animation lifecycle alignment

Reference snapshots: Fabric `c08fc39`, Unofficial Port 1.21.1 `8c8b3ee`,
Unofficial Port 26.1 `8d4736a`; Forge baseline `9fa245d`.

## Behavior

- Selection order is transform, power, then normal locomotion. SSC uses one selected
  full-body clip here; this does not introduce an unrelated general-purpose mask/layer API.
- COUNT replaces previous persistent server state and runs N cycles on the client.
  Both unconditional and ID-filtered STOP reach COUNT even without a server playback entry.
- TIME broadcasts immediately, counts down server-side, and refreshes its remaining
  duration every 100 player ticks. It also expires locally if STOP is missed.
- LOOP remains persistent on the server but has a 120-tick client lease renewed every
  100 player ticks. Refresh packets preserve the clip clock; explicit starts restart it.
- Existing StartTracking synchronization is retained. A new client entity requests
  persistent state once, after the connection is ready. COUNT is not replayed to late observers.
- Playback uses the client world clock, so packets arriving before entity creation do not
  inherit the wrong entity age. Entity removal and world changes clear client state;
  logout clears server state, and respawn stops old animations. Persistent non-attachment
  animations can recover after dimension changes through requests/tracking/heartbeat.
- Power and locomotion share fade handling, including fade from the vanilla base pose.
  Root, limb, and extra-bone transitions use the shortest angular arc. Extra JSON bones
  are in degrees; model/root rotations are in radians.
- AnimationProfile carries fade ticks plus AnimationTransition (easing and skipFade).
  Existing profiles keep linear defaults. Supported transition curves are linear and
  sine/quadratic/cubic/quartic in/out/in-out. This is not a claim to implement every PAL
  easing family or its entire modifier API. Keyframe-internal interpolation is unchanged.

## Compatibility

Network protocol is now **9** (was 8): power packets carry a refresh flag and
ID-filtered STOP, and message 22 requests persistent state. Update client and server together.

## Verification

Run `python tools/test_animation_lifecycle.py` with Python 3 and JDK 17+ on PATH.
It compiles the production service, client handler, packet, clock, transition and
profile classes against small world/transport fakes, testing their observable behavior.
It does not launch Minecraft or validate Forge event delivery/rendering visually.

Run `gradlew.bat build` for the Forge build. The existing build expects the exact
`jei-1.20.1-forge-15.28.0.160.jar` in ignored `libs/`.

In-game follow-up scenarios: transform while attached; stop a COUNT attack early;
observe an already-looping player after entering tracking range; change dimensions,
respawn, and reload resources; switch crawl/idle across a large rotation; verify held
items and extra hind-leg bones share the body's fade.

## Equipment pose follow-up

The Geo overlay already suppressed vanilla swim/crawl root rotation for clips that
supply their own body rotation. PlayerRenderer now uses the same decision, preventing
armor and held-item layers from receiving an extra vanilla rotation/translation.
The prepared limb pose is captured once and reused after vanilla setupAnim instead
of sampling again on a different baseline during fades. This also updates the hat
and keeps inventory-preview equipment on the same pose/root transform as the form.
Prepared-pose ownership is checked, and failed render passes clear it.

## Better Combat / PlayerAnimator (issue #6)

SSC now applies its base pose immediately after `HumanoidModel.setupAnim`, before
PlayerAnimator's first `ModelPart.copyFrom` hook. The Geo overlay captures the final
pose after PlayerAnimator has applied its layers. Vanilla's later render pass restores
only SSC's base pose and lets PlayerAnimator apply its layers once again; it does not
feed the already-combined pose back through the external animation stack. This keeps
combat fades, held items, clothing, and the form model on the same limb pose.

An optional reflection bridge sets PlayerAnimator's partial-tick clock before the
Geo pre-pass and copies its whole-player `body` translation/rotation. The Geo root
uses the same order as the vanilla renderer: form scale, vanilla rotations,
PlayerAnimator root, SSC root. No PlayerAnimator classes are required to load SSC.
The bridge targets the 1.20.1 PlayerAnimator 1.0.2-rc1 API; unsupported APIs log a
warning and disable the root bridge.

Local Gradle runs include Better Combat 1.9.0, PlayerAnimator 1.0.2-rc1 and Cloth Config
11.1.136 as deobfuscated runtime dependencies. Their Forge jars are in ignored `libs/`:
`bettercombat-forge-1.9.0+1.20.1.jar`, `player-animation-lib-forge-1.0.2-rc1+1.20.jar`,
and `cloth-config-11.1.136-forge.jar`. Downloads are available from the projects'
[Better Combat](https://modrinth.com/mod/better-combat),
[PlayerAnimator](https://modrinth.com/mod/playeranimator), and
[Cloth Config](https://modrinth.com/mod/cloth-config) pages. The development run
configuration enables PlayerAnimator's refmap remapping. Run `gradlew.bat runClient`
to test; these runtime dependencies are not bundled into the SSC release jar.

The local runtime also includes [SlashBlade: Resharped 1.9.65](https://modrinth.com/mod/slashblade-resharped/version/1.9.65)
for Forge 1.20.1, from `libs/SlashBladeResharped-1.20.1-1.9.65.jar`. Its optional
PlayerAnimator integration can use the library already installed above. This adds
the mod for compatibility testing; it does not claim in-game animation validation.

PlayerAnimator's first-person `THIRD_PERSON_MODEL` pass also enters SSC's Geo renderer.
That pass draws only the configured left/right arm subtrees, keeping ancestor transforms
while hiding head/body/tail geometry. Bone visibility is restored in `finally`, so the
following third-person pass remains intact. The vanilla arm copies covered by SSC stay
hidden even if PlayerAnimator re-enables them after Forge's Pre event. SSC's existing
no-render-arm power still applies. Ordinary vanilla first-person hand rendering keeps
its existing `RenderArmEvent` path, and remote players keep their full models.

Source references: [PlayerAnimator's model hook](https://github.com/KosmX/minecraftPlayerAnimator/blob/1.20/minecraft/common/src/main/java/dev/kosmx/playerAnim/mixin/PlayerModelMixin.java)
and [renderer root hook](https://github.com/KosmX/minecraftPlayerAnimator/blob/1.20/minecraft/common/src/main/java/dev/kosmx/playerAnim/mixin/PlayerRendererMixin.java).

Verification uses the released Forge PlayerAnimator library and Better Combat 1.9.0's
single-handed horizontal slash and two-handed vertical slash clips. Numerical checks
cover repeated render passes, SSC fades, root motion, inactive attacks, and absence of
PlayerAnimator, plus first-person arm configuration and visibility restoration (375
checks, with a separate library-absence check). The harness substitutes Minecraft/rendering APIs; it does not verify
Mixin delivery or visuals in a running client. Client follow-up should cover third-person
attacks with armor and held items, both hands, small/feral forms, and first-person attacks.

The standalone regression suite now includes exact pose restoration, repeated-pass
idempotence, and hat/limb alignment (64 checks total). Visual verification still
requires a game restart and testing crouch/crawl with armor in third person.
