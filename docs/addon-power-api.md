# Power lookup for add-ons

`SscPowerApi` is the supported read-only entry point for looking up a form's
Power IDs. Stage numbers are **one-based**: `axolotl_0` is stage 1, and
`axolotl_3` is stage 4. The suffix in a built-in form ID is not the stage number.

```java
ResourceLocation family = ResourceLocation.fromNamespaceAndPath(
        "shape-shifter-curse", "axolotl");
List<ResourceLocation> powers = SscPowerApi.powersFor(family, 4);
// The resolved form is shape-shifter-curse:axolotl_3.
```

The first argument can also be an exact form ID (including a branch form) or
the group ID `shape-shifter-curse:axolotl_form`. For an exact form ID, the API
walks that form's progression branch to the requested stage. An unknown form
or stage returns an empty list. `formIdAtStage` returns the resolved exact ID
when the caller needs it.

`powersFor(form, stage)` returns the complete list assigned at that stage,
including Powers retained from earlier stages and Java-form ancestors. It
reads datapack and Java registrations, including form-level additions and
removals. It does not evaluate runtime conditions or player-specific accessory
changes. Use `powersFor(player)` for a particular player's effective Power IDs.
All returned lists are immutable.

## Dependency

The public API is packaged in the normal SSC Forge mod jar. Add-ons depend on
that mod directly; there is no second API mod or API jar to install:

```gradle
repositories {
    maven { url = uri('../shape-shifter-curse-forge/maven-repo/') }
}
dependencies {
    implementation fg.deobf('net.onixary:shape_shifter_curse:1.9.3.14')
}
```

Publish the core project locally with `./gradlew publishMavenJavaPublicationToLocalModRepositoryRepository`.
The full SSC Forge mod must also be present when running the add-on. The
`net.onixary.shapeShifterCurseForge.api` package is the supported call surface;
other implementation packages can change between versions.
