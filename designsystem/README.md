# Hasiy Design System Compose SDK

`Hasiy Design System Compose` is a Compose Multiplatform library containing Liquid Glass styled Compose and Material3 components for Android and iOS. It does not depend on the demo app or its resources.

## Requirements

- Android minSdk 26
- compileSdk 35
- Kotlin 2.1.21 / AGP 8.13.2 / Gradle 9.5.0
- Compose Multiplatform 1.8.2; Android BOM 2024.10.01 (resolved versions also follow CMP constraints)

## Add to another project

For a composite/local project:

```kotlin
dependencies {
    implementation(project(":designsystem"))
}
```

Build a reusable AAR:

```powershell
.\gradlew.bat :designsystem:assembleRelease
```

The artifact is generated at `designsystem/build/outputs/aar/designsystem-release.aar`.

Prefer Maven or source modules to a raw AAR. AARs do not bundle tokens, Haze or Compose dependencies.
The public root coordinate is `top.hasiy:hasiy-design-system-compose:2.0.0`; its transitive token coordinate
is `top.hasiy:designsystem-tokens:1.0.0`. Both modules have Maven publishing configured. Publish root metadata
and all required target artifacts together. Local verification uses `build/verification-maven` and the
independent consumer in `fixtures/android-consumer`; see [BUILDING.md](../BUILDING.md) for commands.
This repository does not configure remote repository credentials or upload artifacts automatically.

Android/JVM API snapshots are checked by `apiCheck`. iOS compile/test jobs require macOS; native ABI snapshots
and real iOS consumer/visual acceptance remain separate release gates.

Custom checkboxes, radio buttons and sliders expose state/actions to accessibility services. Sliders use
current callbacks and range after recomposition; cancelled gestures do not submit a completed control write.

## Theme and touch behavior

Use `GlassPresets.Drop`, `Neutral`, `Dark`, or `Native`, or create a custom `GlassConfig`. At rest the glass highlight sits at `highlightCenterX` / `highlightCenterY` (upper-left by default). While the user presses a component the highlight follows the pointer, brightens, tightens, and gains an extra fingertip hot spot; on release it fades back and springs to its resting position. That is the default (`followTouchHighlight = true`, `hideHighlightOnTouch = false`). Set `followTouchHighlight = false` to keep the press brightening without the pointer tracking, or `hideHighlightOnTouch = true` to fade the highlight out while pressed instead.

`glassSurface` clips its content with `clip(shape)`. Any click modifier that draws a Material indication (`clickable`, `selectable`, `toggleable`, ...) must therefore be chained **after** `glassSurface`, otherwise the ripple is drawn before the clip and spills out of the rounded corners as a rectangle.

## Public component coverage

| Category | APIs |
| --- | --- |
| Foundation | `glassSurface`, `glassBlur`, `GlassCard`, `GlassButton`, `GlassIconButton` |
| Dialog / Popup | `GlassBackdropHost`, `GlassDialogBlurHost`, `GlassDialog`, `GlassPopupBlurBox`, `GlassPopup` |
| Selection | `GlassCheckbox`, `GlassRadioButton`, `GlassSlider`, `GlassVolumeSlider`, `GlassSwitch` |
| Navigation | `GlassSegmentedTabBar`, `GlassNavigationBar`, `GlassNavigationRail`, `GlassModalNavigationDrawer` |
| Bottom sheets | `GlassBottomSheet`, `GlassBottomSheetScaffold`, `GlassModalBottomSheet` |
| Actions | `GlassFloatingActionButton`, `GlassExtendedFloatingActionButton` |
| Information | `GlassSnackbar`, `GlassBadge`, `GlassAssistChip`, `GlassSuggestionChip`, `GlassFilterChip`, `GlassInputChip`, `GlassTooltipBox` |
| Menus | `GlassDropdownMenu`, `GlassDropdownMenuItem`, `GlassExposedDropdownMenuBox`, `GlassContextMenuArea` |
| Input | `GlassTextField`, `GlassSearchBar`, `GlassDockedSearchBar`, `GlassDatePickerDialog`, `GlassTimePickerDialog` |
| Feedback | `GlassProgressBar`, `GlassPullToRefreshBox` |
| Layout / list | `GlassTopBar`, `GlassListItem`, `GlassThemeSelector` |

`GlassBottomSheetScaffold` is a screen/root-layout component. Like Material3's original scaffold, it must receive a finite height and must not be placed directly inside `verticalScroll` or another parent that measures children with infinite height.

## Blur contracts

The rendering contract has four layers:

```text
L0  Page background and decoration
L1  Real page content recorded once by GlassBackdropHost
L2  Glass surface: backdrop blur/fallback tint, highlight, border, and shadow
L3  Foreground text, icons, and controls; never blurred
```

- Wrap each screen once with `GlassBackdropHost`. It records the page as a blur source but never blurs the page itself.
- A nested `GlassBackdropHost` reuses the outer source instead of creating another capture source.
- Every SDK overlay applies backdrop blur only inside its own rounded surface: Dialog, Popup, Dropdown/ExposedDropdown, ContextMenu, docked-search results, Tooltip, DatePicker, TimePicker, Drawer, BottomSheet/ModalBottomSheet, and Snackbar.
- Content outside the overlay stays sharp. Glass modal components also use a transparent Material scrim; the invisible scrim still handles outside-tap dismissal.
- Pass the same `Shape` to `glassBackdrop`, `glassSurface`, and the Material container so blur, fill, border, and clipping remain aligned.
- `overlayBlurRadius` in `GlassConfig` controls the default radius. `GlassBackdropHost(blurRadius = ...)`, legacy `GlassDialogBlurHost`, or legacy `GlassPopupBlurBox` can override it for a subtree.
- API 26–30, a missing host, or another unsupported backdrop path uses `overlayFallbackAlpha` as a denser frosted tint; foreground content remains readable and interactive.
- `GlassDialogBlurHost` and `GlassPopupBlurBox` remain source-compatible, but their old whole-page/anchor-blur behavior has intentionally been removed.

```kotlin
GlassBackdropHost(Modifier.fillMaxSize()) {
    ScreenContent()
    if (showDialog) {
        GlassDialog(
            onDismissRequest = { showDialog = false },
            title = "Title",
            message = "Only this card blurs its backdrop",
            onConfirm = { showDialog = false },
        )
    }
}
```

## Cross-platform theme tokens

The `:designsystem-tokens` module is a KMP metadata module for sharing theme identity and semantic colors between CMP targets. It has no Android resource or Compose dependency:

```kotlin
val spec = GlassThemeSpec(
    id = "fitdash-sky",
    visualStyle = GlassVisualStyle.DROP,
    isLight = false,
    primary = 0xFF5AA9EEL,
    onPrimary = 0xFF06202FL,
    secondary = 0xFF6FD3E8L,
    backgroundTop = 0xFF2A1630L,
    backgroundBottom = 0xFF0B0D1AL,
    surface = 0xFF15151AL,
    onSurface = 0xFFFFFFFFL,
    glassBase = 0xFF9A9AA8L,
    glassContent = 0xFFFFFFFFL,
    glassHighlight = 0xFFFFFFFFL,
    glassBorder = 0xFFFFFFFFL,
    accent = 0xFF5AA9EEL,
    accentEnabled = false,
)
```

The Android Compose adapter exposes `DesignSystemTheme(spec)`, `LocalGlassConfig`, and `LocalGlassThemeSpec`. The existing Android glass renderer remains platform-specific; iOS/CMP rendering must provide a separate renderer before the controls can be used from `commonMain`.