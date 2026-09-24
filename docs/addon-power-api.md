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

The core project produces `shape_shifter_curse-<version>-api.jar` with
`./gradlew apiJar`. Add-ons can compile against its Maven classifier without
bundling it:

```gradle
repositories {
    maven { url = uri('../shape-shifter-curse-forge/maven-repo/') }
}
dependencies {
    compileOnly 'net.onixary:shape_shifter_curse:1.9.3.14:api'
}
```

Publish the core project locally with `./gradlew publishMavenJavaPublicationToLocalModRepositoryRepository`.
The API jar is a compile-time contract; the full SSC Forge mod must be present
when running the add-on. Add-ons that use SSC internals beyond the public API
still need a compile dependency on the full core mod.
